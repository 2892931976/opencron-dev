package org.opencron.server.job;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.*;
import org.opencron.common.logging.InternalLogger;
import org.opencron.common.logging.InternalLoggerFactory;
import org.opencron.common.utils.CommonUtils;
import org.opencron.common.utils.DateUtils;
import org.opencron.server.domain.Agent;
import org.opencron.server.service.AgentService;
import org.opencron.server.service.ConfigService;
import org.opencron.server.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class OpencronHeartBeat {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OpencronHeartBeat.class);

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    private Bootstrap bootstrap;

    private NioEventLoopGroup group;

    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();

    private long keepAliveDelay = 1000 * 5;//5秒一次心跳

    @Autowired
    private AgentService agentService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private NoticeService noticeService;

    private Map<String, Agent> heartbeatAgentMap = new ConcurrentHashMap<String, Agent>(0);

    public void heartbeat(Agent agent) throws Exception {

        heartbeatAgentMap.put(agent.getIp(), agent);

        this.group = new NioEventLoopGroup();

        this.bootstrap = new Bootstrap();

        bootstrap.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));

        final ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, timer, agent.getPort(), agent.getIp()) {

            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS),
                        idleStateTrigger,
                        new StringDecoder(),
                        new StringEncoder(),
                        new HeartBeatHandler()
                };
            }

        };

        ChannelFuture future;
        //进行连接
        try {
            synchronized (bootstrap) {
                bootstrap.handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = bootstrap.connect(agent.getIp(), agent.getPort());
            }
            future.sync();
        } catch (Throwable t) {
            if (t instanceof ConnectException) {
                disconnectAction(agent.getIp());
            }
            this.group.shutdownGracefully();
        }
    }

    @ChannelHandler.Sharable
    public class ConnectorIdleStateTrigger extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(ChannelHandlerContext handlerContext, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleState state = ((IdleStateEvent) evt).state();
                if (state == IdleState.WRITER_IDLE) {
                    // write heartbeat to server
                    handlerContext.writeAndFlush(getAgentFromHandlerContext(handlerContext).getPasswordByteBuf());
                }
            } else {
                super.userEventTriggered(handlerContext, evt);
            }
        }

    }


    @ChannelHandler.Sharable
    public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext handlerContext) throws Exception {
            logger.info("[opencron] agent heartbeat Starting..... {}", DateUtils.formatFullDate(new Date()));
            handlerContext.fireChannelActive().writeAndFlush(getAgentFromHandlerContext(handlerContext).getPasswordByteBuf());
        }

        @Override
        public void channelInactive(ChannelHandlerContext handlerContext) throws Exception {
            logger.info("[opencron] agent channelInactive {}", DateUtils.formatFullDate(new Date()));
        }

        @Override
        public void channelRead(ChannelHandlerContext handlerContext, Object msg) throws Exception {
            Agent agent = getAgentFromHandlerContext(handlerContext);
            if (agent == null) {
                throw new RuntimeException("[opencron] ChannelHandlerContext can't found agent");
            }
            String result = (String) msg;
            if (result.equals("1")) {
                if (!agent.getStatus()) {
                    agent.setStatus(true);
                    agentService.merge(agent);
                }
            } else {
                disconnectAction(agent.getIp());
                //链路关闭通...
                handlerContext.fireChannelInactive();
            }
            ReferenceCountUtil.release(msg);
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            super.exceptionCaught(ctx, cause);
        }
    }


    interface ChannelHandlerHolder {

        ChannelHandler[] handlers();
    }

    /**
     * 重连检测狗，当发现当前的链路不稳定关闭之后，进行12次重连
     */
    @ChannelHandler.Sharable
    public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {

        private final Bootstrap bootstrap;
        private final Timer timer;
        private final int port;

        private final String host;

        private int attempts;

        public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, int port, String host) {
            this.bootstrap = bootstrap;
            this.timer = timer;
            this.port = port;
            this.host = host;
        }

        /**
         * channel链路每次active的时候，将其连接的次数重新☞ 0
         */
        @Override
        public void channelActive(ChannelHandlerContext handlerContext) throws Exception {
            logger.info("[opencron] agent reActive successful,reset reTry count,{}", DateUtils.formatFullDate(new Date()));
            attempts = 0;
            handlerContext.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext handlerContext) throws Exception {
            logger.info("[opencron] agent channel closed,reTry connection [{}] starting...", attempts);
            if (attempts < 10) {
                attempts++;
                timer.newTimeout(this, keepAliveDelay, TimeUnit.MILLISECONDS);
            } else {
                /**
                 * Agent机器失连,进行10次的连接重试,如果都是连接失败，则认为Agent已经失联,发送通知,关闭Agent链路...
                 */
                logger.warn("[opencron] agent channel disconnected");
                Agent agent = getAgentFromHandlerContext(handlerContext);
                if (agent != null) {
                    disconnectAction(agent.getIp());
                } else {
                    throw new RuntimeException("[opencron] ChannelHandlerContext can't found agent");
                }
            }

            handlerContext.fireChannelInactive();
        }


        public void run(Timeout timeout) throws Exception {

            synchronized (bootstrap) {

                bootstrap.handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {

                        channel.pipeline().addLast(handlers());
                    }
                });

                bootstrap.connect(host, port).addListener(new ChannelFutureListener() {

                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        boolean succeed = channelFuture.isSuccess();
                        if (!succeed) {
                            logger.info("[opencron] agent channel reTry connection failed");
                            channelFuture.channel().pipeline().fireChannelInactive();
                        } else {
                            logger.info("[opencron] agent channel reTry connection successful");
                        }
                    }
                });

            }

        }
    }

    private Agent getAgentFromHandlerContext(ChannelHandlerContext handlerContext) {
        SocketAddress socketAddress = handlerContext.channel().remoteAddress();
        String host = socketAddress.toString().replaceAll("^/|:\\d+$", "");
        return heartbeatAgentMap.get(host);
    }

    public void disconnectAction(String host) {
        Agent agent = heartbeatAgentMap.get(host);

        if (agent.getStatus()) {

            agent.setStatus(false);

            if (CommonUtils.isEmpty(agent.getNotifyTime()) || new Date().getTime() - agent.getNotifyTime().getTime() >= configService.getSysConfig().getSpaceTime() * 60 * 1000) {
                noticeService.notice(agent);
                //记录本次任务失败的时间
                agent.setNotifyTime(new Date());
            }

            //save disconnect status to db.....
            agentService.merge(agent);
        }

    }

}

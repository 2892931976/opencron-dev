package org.opencron.server.job;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.*;
import org.opencron.common.job.Action;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.serialization.Decoder;
import org.opencron.common.serialization.Encoder;
import org.opencron.common.util.DateUtils;
import org.opencron.server.domain.Agent;
import org.opencron.server.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class  OpencronHeartBeat {

    private static final Logger logger = LoggerFactory.getLogger(OpencronHeartBeat.class);

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    private Bootstrap bootstrap;

    private NioEventLoopGroup group;

    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();

    private long keepAliveDelay = 1000 * 5;//5秒一次心跳

    @Autowired
    private AgentService agentService;

    private Map<String, Agent> heartbeatAgentMap = new ConcurrentHashMap<String, Agent>(0);

    public void heartbeat(Agent agent) throws Exception {

        heartbeatAgentMap.put(agent.getHost(), agent);

        this.group = new NioEventLoopGroup();

        this.bootstrap = new Bootstrap();

        bootstrap.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));

        final ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, timer, agent.getPort(), agent.getHost()) {

            public ChannelHandler[] handlers() throws IOException {
                return new ChannelHandler[]{
                        //new LengthFieldBasedFrameDecoder(1<<20, 0, 4, 0, 4),
                        //new LengthFieldPrepender(4),
                        this,
                        new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS),
                        idleStateTrigger,
                        new Decoder<Response>(Response.class, 1024 * 1024, 2, 4),
                        new Encoder<Request>(Request.class),
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

                future = bootstrap.connect(agent.getHost(), agent.getPort());
            }
            future.sync();
        } catch (Throwable t) {
            if (t instanceof ConnectException) {
                disconnectAction(agent.getHost());
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
                    Agent agent = getAgent(handlerContext);
                    if (agent!=null) {
                        Request request = Request.request(agent.getHost(),agent.getPort(), null,agent.getPassword());
                        handlerContext.fireChannelActive().writeAndFlush(request);
                    }
                }
            } else {
                super.userEventTriggered(handlerContext, evt);
            }
        }

    }


    @ChannelHandler.Sharable
    public class HeartBeatHandler extends SimpleChannelInboundHandler<Response> {

        @Override
        public void channelActive(ChannelHandlerContext handlerContext) throws Exception {
            logger.info("[opencron] agent heartbeat Starting..... {}", DateUtils.formatFullDate(new Date()));
            Agent agent = getAgent(handlerContext);
            if (agent!=null) {
                Request request = Request.request(agent.getHost(),agent.getPort(), null,agent.getPassword());
                handlerContext.fireChannelActive().writeAndFlush(request);
            }

        }

        @Override
        public void channelInactive(ChannelHandlerContext handlerContext) throws Exception {
            logger.info("[opencron] agent channelInactive {}", DateUtils.formatFullDate(new Date()));
        }

        @Override
        protected void channelRead0(ChannelHandlerContext handlerContext, Response response) throws Exception {
            Agent agent = getAgent(handlerContext);
            if (agent == null) {
                throw new RuntimeException("[opencron] ChannelHandlerContext can't found agent");
            }
            if (response.isSuccess()) {
                if (!agent.getStatus()) {
                    agent.setStatus(true);
                    agentService.merge(agent);
                }
            } else {
                disconnectAction(agent.getHost());
                //链路关闭通...
                handlerContext.fireChannelInactive();
            }
            ReferenceCountUtil.release(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            super.exceptionCaught(ctx, cause);
        }
    }


    interface ChannelHandlerHolder {

        ChannelHandler[] handlers() throws IOException;

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
                Agent agent = getAgent(handlerContext);
                if (agent != null) {
                    disconnectAction(agent.getHost());
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

    private Agent getAgent(ChannelHandlerContext handlerContext) {
        SocketAddress socketAddress = handlerContext.channel().remoteAddress();
        String host = socketAddress.toString().replaceAll("^/|:\\d+$", "");
        return heartbeatAgentMap.get(host);
    }

    private void disconnectAction(String host) {
        Agent agent = heartbeatAgentMap.get(host);

        if (agent.getStatus()) {
            agentService.doDisconnect(agent);
        }
    }

}

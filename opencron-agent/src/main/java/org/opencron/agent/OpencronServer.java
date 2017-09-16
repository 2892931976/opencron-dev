package org.opencron.agent;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.rpc.codec.RpcDecoder;
import org.opencron.common.rpc.codec.RpcEncoder;
import org.opencron.common.utils.LoggerFactory;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OpencronServer {

    private static Logger logger = LoggerFactory.getLogger(OpencronServer.class);

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ServerBootstrap bootstrap = new ServerBootstrap();
    private ChannelFuture channelFuture;

    private ThreadPoolExecutor pool;//业务处理线程池

    private int port;

    private String password;

    public OpencronServer(int port, String password) {
        this.port = port;
        this.password = password;
    }

    public void start() {

        this.pool = new ThreadPoolExecutor(50, 100, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger idGenerator = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "opencron-Agent-" + this.idGenerator.incrementAndGet());
            }
        });

        this.bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .localAddress(new InetSocketAddress(port)).childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(
                                new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS),
                                new AgentIdleHandler.AcceptorIdleStateTrigger(),
                                new RpcDecoder(Request.class),
                                new RpcEncoder(Response.class),
                                new AgentIdleHandler(password),
                                new AgentHandler(pool)
                        );
                    }
                });

        try {
            this.channelFuture = this.bootstrap.bind(port).sync();
            this.channelFuture.channel().closeFuture().sync();
            this.channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if(f.isSuccess()){
                        logger.info("Rpc Server start at address:{} success", port);
                    } else {
                        logger.error("Rpc Server start at address:{} failure", port);
                    }
                }
            });
            System.out.println("Rpc Server start..."+port);

        } catch (InterruptedException e) {
            throw new RuntimeException("bind server error", e);
        }
    }

    public void stop() {
        logger.info("[opencron] Agent stopping... ");
        this.pool.shutdown();
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

}



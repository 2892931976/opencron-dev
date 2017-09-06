package org.opencron.agent;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.opencron.common.utils.LoggerFactory;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class AgentServer {

    private static Logger logger = LoggerFactory.getLogger(AgentServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture channelFuture;

    private int port;

    private String password;

    public AgentServer(int port,String password){
        this.port = port;
        this.password = password;
    }

    public void start() {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                    .localAddress(new InetSocketAddress(port)).childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                            channel.pipeline().addLast(new AgentIdleHandler.AcceptorIdleStateTrigger());
                            channel.pipeline().addLast("decoder", new StringDecoder());
                            channel.pipeline().addLast("encoder", new StringEncoder());
                            channel.pipeline().addLast(new AgentIdleHandler(password));//心跳检查
                            //channel.pipeline().addLast(new AgentHandler());//
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
            // 绑定端口，开始接收进来的连接
            this.channelFuture = bootstrap.bind(this.port).sync();
            this.channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
        }
    }

    public void stop() {

        logger.info("[opencron] Agent stopping... ");

        if (this.bossGroup!=null){
            this.bossGroup.shutdownGracefully();
        }

        if (this.workerGroup!=null){
            this.workerGroup.shutdownGracefully();
        }
    }

}



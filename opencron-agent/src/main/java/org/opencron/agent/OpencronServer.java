/**
 * Copyright (c) 2015 The Opencron Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.serialization.Decoder;
import org.opencron.common.serialization.Encoder;
import org.opencron.common.util.SystemPropertyUtils;
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

    public OpencronServer() {  }

    public void start() {

        final int port = SystemPropertyUtils.getInt("opencron.port",1577);

        this.pool = new ThreadPoolExecutor(50, 100, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger idGenerator = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "opencron-Agent-" + this.idGenerator.incrementAndGet());
            }
        });

        final AgentMonitor monitor = new AgentMonitor();

        monitor.start();

        this.bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                .localAddress(new InetSocketAddress(port)).childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(
                            new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS),
                            new AgentIdleHandler.AcceptorIdleStateTrigger(),
                            new Decoder(Request.class,1<<20, 2, 4),
                            new Encoder(Response.class),
                            new AgentIdleHandler(),
                            new AgentHandler(pool,monitor)
                        );
                    }
                }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            this.channelFuture = this.bootstrap.bind(port).sync();
            this.channelFuture.channel().closeFuture().sync();
            this.channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if(f.isSuccess()){
                        logger.info("Rpc Server start at address:{} success",port);
                    } else {
                        logger.error("Rpc Server start at address:{} failure",port);
                    }
                }
            });
            System.out.println("Rpc Server start..."+port);

        } catch (InterruptedException e) {
            throw new RuntimeException("bind server error",e);
        }
    }

    public void stop() {
        logger.info("[opencron] Agent stopping... ");
        this.pool.shutdown();
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

}






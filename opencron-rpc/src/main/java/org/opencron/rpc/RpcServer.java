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

package org.opencron.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.HashedWheelTimer;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.serialization.Decoder;
import org.opencron.common.serialization.Encoder;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcServer {

    private static Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ServerBootstrap bootstrap = new ServerBootstrap();
    private ChannelFuture channelFuture;

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    private ThreadPoolExecutor pool;//业务处理线程池

    private int prot;

    private RpcHandler handler;

    public RpcServer() {  }

    public RpcServer(int prot,Handler handler){
        this.prot = prot;
        this.handler = new RpcHandler(handler);
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
                .channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
                .localAddress(new InetSocketAddress(this.prot)).childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(
                            new Decoder<Request>(Request.class, 1024 * 1024, 2, 4),
                            new Encoder<Response>(Response.class),
                            handler
                        );
                    }
                }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            this.channelFuture = this.bootstrap.bind(this.prot).sync();
            this.channelFuture.channel().closeFuture().sync();
            this.channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if(f.isSuccess()){
                        logger.info("Rpc Server start at address:{} success",prot);
                    } else {
                        logger.error("Rpc Server start at address:{} failure",prot);
                    }
                }
            });
            System.out.println("Rpc Server start..."+this.prot);

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






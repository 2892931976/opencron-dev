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

package org.opencron.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.opencron.common.Constants;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.util.HttpUtils;
import org.opencron.rpc.Client;
import org.opencron.rpc.ClientAsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * agent OpencronCaller
 *
 * @author <a href="mailto:benjobs@qq.com">B e n</a>
 * @version 1.0
 * @date 2016-03-27
 */

public class NettyClient implements Client {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));

    private Bootstrap bootstrap = new Bootstrap();

    protected final ConcurrentHashMap<Integer, NettyFuture> futureTable =  new ConcurrentHashMap<Integer, NettyFuture>(256);

    private final ConcurrentHashMap<String, ChannelWrapper> channelTable = new ConcurrentHashMap<String, ChannelWrapper>();

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public NettyClient() {
        this.connect();
    }

    @Override
    public void connect() {

        final NettyClientHandler nettyClientHandler = new NettyClientHandler(this);

        bootstrap.group(nioEventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(
                                NettyCodecAdapter.getCodecAdapter().getDecoder(Response.class, 1024 * 1024, 2, 4),
                                NettyCodecAdapter.getCodecAdapter().getEncoder(Request.class),
                                nettyClientHandler
                        );
                    }
                });

        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
            private final AtomicInteger idGenerator = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "opencron:rpc "+ this.idGenerator.incrementAndGet());
            }
        });

        this.scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                scanRpcFutureTable();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void disconnect() {
        this.futureTable.clear();
        this.channelTable.clear();
        this.scheduledThreadPoolExecutor.shutdown();
    }

    @Override
    public Response sentSync(final Request request) throws Exception {
        Channel channel = getOrCreateChannel(request);
        if (channel != null && channel.isActive()) {
            final NettyFuture nettyFuture = new NettyFuture(request.getTimeOut());
            this.futureTable.put(request.getId(), nettyFuture);
            //写数据
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("send success, request id:{}", request.getId());
                        nettyFuture.setSendRequestSuccess(true);
                        return;
                    } else {
                        logger.info("send failure, request id:{}", request.getId());
                        futureTable.remove(request.getId());
                        nettyFuture.setSendRequestSuccess(false);
                        nettyFuture.setFailure(future.cause());
                    }
                }
            });
            return nettyFuture.get();
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }

    @Override
    public void sentAsync(final Request request,final ClientAsyncCallback callback) throws Exception {

        Channel channel = getOrCreateChannel(request);

        if (channel != null && channel.isActive()) {

            final NettyFuture nettyFuture = new NettyFuture(request.getTimeOut(),callback);
            this.futureTable.put(request.getId(), nettyFuture);
            //写数据
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        logger.info("send success, request id:{}", request.getId());
                        nettyFuture.setSendRequestSuccess(true);
                        return;
                    } else {
                        logger.info("send failure, request id:{}", request.getId());
                        futureTable.remove(request.getId());
                        nettyFuture.setSendRequestSuccess(false);
                        nettyFuture.setFailure(future.cause());
                        //回调
                        callback.onFailure(future.cause());
                    }
                }
            });
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }

    @Override
    public void sentOneway(final Request request) throws Exception{
        Channel channel = getOrCreateChannel(request);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        logger.info("send success, request id:{}", request.getId());
                    } else {
                        logger.info("send failure, request id:{}", request.getId(), future);
                    }
                }
            });
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }

    private Channel getOrCreateChannel(Request request) {

        ChannelWrapper channelWrapper = this.channelTable.get(request.getAddress());

        if (channelWrapper != null && channelWrapper.isActive()) {
            return channelWrapper.getChannel();
        }

        synchronized (this){
            // 发起异步连接操作
            ChannelFuture channelFuture = bootstrap.connect(HttpUtils.parseSocketAddress(request.getAddress()));
            channelWrapper = new ChannelWrapper(channelFuture);
            this.channelTable.put(request.getAddress(), channelWrapper);
        }
        if (channelWrapper != null) {
            ChannelFuture channelFuture = channelWrapper.getChannelFuture();
            long timeout = 5000;
            if (channelFuture.awaitUninterruptibly(timeout)) {
                if (channelWrapper.isActive()) {
                    logger.info("createChannel: connect remote host[{}] success, {}", request.getAddress(), channelFuture.toString());
                    return channelWrapper.getChannel();
                } else {
                    logger.warn("createChannel: connect remote host[" + request.getAddress() + "] failed, " + channelFuture.toString(), channelFuture.cause());
                }
            } else {
                logger.warn("createChannel: connect remote host[{}] timeout {}ms, {}", request.getAddress(), timeout, channelFuture);
            }
        }
        return null;
    }

    /**定时清理超时Future**/
    private void scanRpcFutureTable() {
        Iterator<Map.Entry<Integer, NettyFuture>> it = this.futureTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, NettyFuture> next = it.next();
            NettyFuture rep = next.getValue();

            if ((rep.getBeginTimestamp() + rep.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {  //超时
                it.remove();
            }
        }
    }

}

/**
 * Copyright 2016 benjobs
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

package org.opencron.server.job;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.rpc.codec.RpcDecoder;
import org.opencron.common.rpc.codec.RpcEncoder;
import org.opencron.common.rpc.core.ChannelWrapper;
import org.opencron.common.rpc.core.InvokeCallback;
import org.opencron.common.rpc.core.RpcFuture;
import org.opencron.common.utils.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

@Component
public class OpencronClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private EventLoopGroup group = new NioEventLoopGroup();

    private Bootstrap bootstrap = new Bootstrap();

    protected final ConcurrentHashMap<Integer, RpcFuture> rpcFutureTable =  new ConcurrentHashMap<Integer, RpcFuture>(256);

    private final ConcurrentHashMap<String, ChannelWrapper> channelTable = new ConcurrentHashMap<String, ChannelWrapper>();

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public void start(){
        bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline().addLast(
                                new RpcDecoder(Response.class), //
                                new RpcEncoder(Request.class), //
                                new OpencronHandler()
                        );
                    }
                });

        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
            private final AtomicInteger idGenerator = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Rpc-Scheduled-" + this.idGenerator.incrementAndGet());
            }
        });

        this.scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                scanRpcFutureTable();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        this.scheduledThreadPoolExecutor.shutdown();
        this.group.shutdownGracefully();
    }

    public Response sendSync(String address, final Request request, long timeout, TimeUnit unit) throws Exception {
        Channel channel = getOrCreateChannel(address);
        if (channel != null && channel.isActive()) {

            final RpcFuture<Response> rpcFuture = new RpcFuture<Response>(timeout, unit);
            this.rpcFutureTable.put(request.getId(), rpcFuture);
            //写数据
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("send success, request id:{}", request.getId());
                        rpcFuture.setSendRequestSuccess(true);
                        return;
                    } else {
                        logger.info("send failure, request id:{}", request.getId());
                        rpcFutureTable.remove(request.getId());
                        rpcFuture.setSendRequestSuccess(false);
                        rpcFuture.setFailure(future.cause());
                    }
                }
            });

            return rpcFuture.get(timeout, unit);
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }

    public void sendAsync(String address, final Request request, long timeout, TimeUnit unit, final InvokeCallback callback) throws Exception {

        Channel channel = getOrCreateChannel(address);

        if (channel != null && channel.isActive()) {

            final RpcFuture<Response> rpcFuture = new RpcFuture<Response>(timeout, unit, callback);
            this.rpcFutureTable.put(request.getId(), rpcFuture);
            //写数据
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {

                    if (future.isSuccess()) {
                        logger.info("send success, request id:{}", request.getId());
                        rpcFuture.setSendRequestSuccess(true);
                        return;
                    } else {
                        logger.info("send failure, request id:{}", request.getId());

                        rpcFutureTable.remove(request.getId());
                        rpcFuture.setSendRequestSuccess(false);
                        rpcFuture.setFailure(future.cause());
                        //回调
                        callback.onFailure(future.cause());
                    }
                }
            });
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }

    public void sendOneway(String address, final Request request, long timeout, TimeUnit unit){

        Channel channel = getOrCreateChannel(address);
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

    class OpencronHandler extends SimpleChannelInboundHandler<Response> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response msg) throws Exception {

            final Response response = msg;
            logger.info("Rpc client receive response id:{}", response.getId());
            RpcFuture future = rpcFutureTable.get(response.getId());

            future.setResult(response);
            if(future.isAsync()){   //异步调用
                logger.info("Rpc client async callback invoke");
                future.execCallback();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            logger.error("捕获异常", cause);
        }
    }

    private Channel getOrCreateChannel(String address){

        ChannelWrapper cw = this.channelTable.get(address);
        if (cw != null && cw.isActive()) {
            return cw.getChannel();
        }

        synchronized (this){
            // 发起异步连接操作
            ChannelFuture channelFuture = bootstrap.connect(NetUtils.parseSocketAddress(address));
            cw = new ChannelWrapper(channelFuture);
            this.channelTable.put(address, cw);
        }
        if (cw != null) {
            ChannelFuture channelFuture = cw.getChannelFuture();
            long timeout = 5000;
            if (channelFuture.awaitUninterruptibly(timeout)) {
                if (cw.isActive()) {
                    logger.info("createChannel: connect remote host[{}] success, {}", address, channelFuture.toString());
                    return cw.getChannel();
                } else {
                    logger.warn("createChannel: connect remote host[" + address + "] failed, " + channelFuture.toString(), channelFuture.cause());
                }
            } else {
                logger.warn("createChannel: connect remote host[{}] timeout {}ms, {}", address, timeout, channelFuture);
            }
        }
        return null;
    }

    /**定时清理超时Future**/
    private void scanRpcFutureTable() {
        final List<RpcFuture> timeoutReqList = new ArrayList<RpcFuture>();
        Iterator<Map.Entry<Integer, RpcFuture>> it = this.rpcFutureTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RpcFuture> next = it.next();
            RpcFuture rep = next.getValue();

            if ((rep.getBeginTimestamp() + rep.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {  //超时
                it.remove();
                timeoutReqList.add(rep);
            }
        }

        for (RpcFuture future : timeoutReqList) {
            //释放资源
        }
    }

}

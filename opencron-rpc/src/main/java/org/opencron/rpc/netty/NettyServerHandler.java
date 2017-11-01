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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.opencron.common.job.*;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.rpc.RpcHandler;
import org.slf4j.Logger;

@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<Request> {

    private Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    private RpcHandler handler;

    public NettyServerHandler(RpcHandler handler){
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext handlerContext) {
        logger.info("[opencron] agent channelActive Active...");
        handlerContext.fireChannelActive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, Request request) throws Exception {
        Response response = handler.handle(request);
        if(request.getRpcType()!= RpcType.ONE_WAY){    //非单向调用
            handlerContext.writeAndFlush(response);
        }
        logger.info("[opencron] agent process done,request:{},action:", request.getAction());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.error("[opencron] agent channelInactive");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
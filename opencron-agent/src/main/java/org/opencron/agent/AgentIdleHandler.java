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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.slf4j.Logger;

public class AgentIdleHandler extends SimpleChannelInboundHandler<Request> {

    private String password;

    private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public AgentIdleHandler(String password) {
        this.password = password;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext handlerContext, Request request) throws Exception {
        if (!this.password.equalsIgnoreCase(request.getPassword())) {
            logger.error("[opencron] heartbeat password error!,with server {}",handlerContext.channel().remoteAddress());
        }
        Response response = Response.response(request).setSuccess(this.password.equalsIgnoreCase(request.getPassword())).end();
        handlerContext.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext handlerContext, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (this == handlerContext.pipeline().last()) {
            logger.info("[opencron] agent {} shutdowd...",handlerContext.channel().id());
        }
        handlerContext.channel().close();
        handlerContext.close();
    }

    @ChannelHandler.Sharable
    public static class AcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(ChannelHandlerContext handlerContext, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleState state = ((IdleStateEvent) evt).state();
                if (state == IdleState.READER_IDLE) {
                    logger.warn("[opencron] heartbeat READER_IDLE,with server {}",handlerContext.channel().remoteAddress());
                }
            } else {
                super.userEventTriggered(handlerContext, evt);
            }
        }
    }

}
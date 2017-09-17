/*
 * Copyright (c) 2015 The Opencron Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencron.transport.netty.handler.connector;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.opencron.common.logging.InternalLogger;
import org.opencron.common.logging.InternalLoggerFactory;
import org.opencron.common.util.Signal;
import org.opencron.transport.api.payload.ResponseBytes;
import org.opencron.transport.api.processor.ConsumerProcessor;
import org.opencron.transport.netty.channel.NettyChannel;

import java.io.IOException;

import static org.opencron.common.util.ExceptionUtils.stackTrace;


@ChannelHandler.Sharable
public class ConnectorHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectorHandler.class);

    private ConsumerProcessor processor;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (msg instanceof ResponseBytes) {
            try {
                processor.handleResponse(NettyChannel.attachChannel(ch), (ResponseBytes) msg);
            } catch (Throwable t) {
                logger.error("[opencron] An exception was caught: {}, on {} #channelRead().", stackTrace(t), ch);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("[opencron] Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);
            }

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();
        ChannelConfig config = ch.config();

        // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
        // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
        if (!ch.isWritable()) {
            // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
            if (logger.isWarnEnabled()) {
                logger.warn("[opencron] {} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(false);
        } else {
            // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
            if (logger.isWarnEnabled()) {
                logger.warn("[opencron] {} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel ch = ctx.channel();

        if (cause instanceof Signal) {
            logger.error("[opencron] An I/O signal was caught: {}, force to close channel: {}.", ((Signal) cause).name(), ch);

            ch.close();
        } else if (cause instanceof IOException) {
            logger.error("[opencron] An I/O exception was caught: {}, force to close channel: {}.", stackTrace(cause), ch);

            ch.close();
        } else {
            logger.error("[opencron] An unexpected exception was caught: {}, channel: {}.", stackTrace(cause), ch);
        }
    }

    public ConsumerProcessor processor() {
        return processor;
    }

    public void processor(ConsumerProcessor processor) {
        this.processor = processor;
    }
}

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

package org.opencron.transport.netty.handler.acceptor;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.opencron.common.logging.InternalLogger;
import org.opencron.common.logging.InternalLoggerFactory;
import org.opencron.common.util.Signal;
import org.opencron.transport.api.Status;
import org.opencron.transport.api.channel.Channel;
import org.opencron.transport.api.payload.RequestBytes;
import org.opencron.transport.api.processor.ProviderProcessor;
import org.opencron.transport.netty.channel.NettyChannel;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opencron.common.util.ExceptionUtils.stackTrace;


@ChannelHandler.Sharable
public class AcceptorHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AcceptorHandler.class);

    private static final AtomicInteger channelCounter = new AtomicInteger(0);

    private ProviderProcessor processor;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        io.netty.channel.Channel ch = ctx.channel();

        if (msg instanceof RequestBytes) {
            Channel channel = NettyChannel.attachChannel(ch);
            try {
                processor.handleRequest(channel, (RequestBytes) msg);
            } catch (Throwable t) {
                processor.handleException(channel, (RequestBytes) msg, Status.SERVER_ERROR, t);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);
            }

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.incrementAndGet();

        logger.info("Connects with {} as the {}th channel.", ctx.channel(), count);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.getAndDecrement();

        logger.warn("Disconnects with {} as the {}th channel.", ctx.channel(), count);

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        io.netty.channel.Channel ch = ctx.channel();
        ChannelConfig config = ch.config();

        // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
        // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
        if (!ch.isWritable()) {
            if (logger.isWarnEnabled()) {
                logger.warn("[opencron] {} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(false);
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("[opencron] {} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        io.netty.channel.Channel ch = ctx.channel();

        if (cause instanceof Signal) {
            logger.error("An I/O signal was caught: {}, force to close channel: {}.", ((Signal) cause).name(), ch);

            ch.close();
        } else if (cause instanceof IOException) {
            logger.error("An I/O exception was caught: {}, force to close channel: {}.", stackTrace(cause), ch);

            ch.close();
        } else {
            logger.error("An unexpected exception was caught: {}, channel: {}.", stackTrace(cause), ch);
        }
    }

    public ProviderProcessor processor() {
        return processor;
    }

    public void processor(ProviderProcessor processor) {
        this.processor = processor;
    }
}

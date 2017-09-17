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

package org.opencron.transport.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.opencron.transport.api.channel.Channel;
import org.opencron.transport.api.channel.FutureListener;
import org.opencron.transport.netty.handler.connector.ConnectionWatchdog;

import java.net.SocketAddress;

/**
 * 对Netty {@link io.netty.channel.Channel} 的包装, 通过静态方法 {@link #attachChannel(io.netty.channel.Channel)} 获取一个实例,
 * {@link NettyChannel} 实例构造后会attach到对应 {@link io.netty.channel.Channel} 上, 不需要每次创建.
 */
public class NettyChannel implements Channel {

    private static final AttributeKey<NettyChannel> NETTY_CHANNEL_KEY = AttributeKey.valueOf("netty.channel");

    /**
     * Returns the {@link NettyChannel} for given {@link io.netty.channel.Channel}, this method never return null.
     */
    public static NettyChannel attachChannel(io.netty.channel.Channel channel) {
        Attribute<NettyChannel> attr = channel.attr(NETTY_CHANNEL_KEY);
        NettyChannel nChannel = attr.get();
        if (nChannel == null) {
            NettyChannel newNChannel = new NettyChannel(channel);
            nChannel = attr.setIfAbsent(newNChannel);
            if (nChannel == null) {
                nChannel = newNChannel;
            }
        }
        return nChannel;
    }

    private final io.netty.channel.Channel channel;

    private NettyChannel(io.netty.channel.Channel channel) {
        this.channel = channel;
    }

    public io.netty.channel.Channel channel() {
        return channel;
    }

    @Override
    public String id() {
        return channel.id().asShortText(); // 注意这里的id并不是全局唯一, 单节点中是唯一的
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public boolean inIoThread() {
        return channel.eventLoop().inEventLoop();
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    @Override
    public boolean isMarkedReconnect() {
        ConnectionWatchdog watchdog = channel.pipeline().get(ConnectionWatchdog.class);
        return watchdog != null && watchdog.isStarted();
    }

    @Override
    public boolean isAutoRead() {
        return channel.config().isAutoRead();
    }

    @Override
    public void setAutoRead(boolean autoRead) {
        channel.config().setAutoRead(autoRead);
    }

    @Override
    public Channel close() {
        channel.close();
        return this;
    }

    @Override
    public Channel close(final FutureListener<Channel> listener) {
        final Channel channel = this;
        this.channel.close().addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    listener.operationSuccess(channel);
                } else {
                    listener.operationFailure(channel, future.cause());
                }
            }
        });
        return channel;
    }

    @Override
    public Channel write(Object msg) {
        channel.writeAndFlush(msg, channel.voidPromise());
        return this;
    }

    @Override
    public Channel write(Object msg, final FutureListener<Channel> listener) {
        final Channel channel = this;
        this.channel.writeAndFlush(msg)
                .addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            listener.operationSuccess(channel);
                        } else {
                            listener.operationFailure(channel, future.cause());
                        }
                    }
                });
        return channel;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof NettyChannel && channel.equals(((NettyChannel) obj).channel));
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }

    @Override
    public String toString() {
        return channel.toString();
    }
}

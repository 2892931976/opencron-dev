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

package org.opencron.transport.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.opencron.transport.api.Connection;
import org.opencron.transport.api.UnresolvedAddress;
public abstract class NettyConnection extends Connection {

    private final ChannelFuture future;

    public NettyConnection(UnresolvedAddress address, ChannelFuture future) {
        super(address);
        this.future = future;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    @Override
    public void operationComplete(final Runnable callback) {
        future.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    callback.run();
                }
            }
        });
    }
}

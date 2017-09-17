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

package org.opencron.transport.api;


import org.opencron.transport.api.channel.CopyOnWriteGroupList;
import org.opencron.transport.api.channel.DirectoryChannelGroup;
import org.opencron.transport.api.channel.ChannelGroup;
import org.opencron.transport.api.processor.ConsumerProcessor;

import java.util.Collection;

public interface Connector<C> extends Transporter {

    /**
     * Connector options [parent, child].
     */
    Config config();

    /**
     * Binds the rpc processor.
     */
    void withProcessor(ConsumerProcessor processor);

    /**
     * Connects to the remote peer.
     */
    C connect(UnresolvedAddress address);

    /**
     * Connects to the remote peer.
     */
    C connect(UnresolvedAddress address, boolean async);

    /**
     * Returns or new a {@link ChannelGroup}.
     */
    ChannelGroup group(UnresolvedAddress address);

    /**
     * Returns all {@link ChannelGroup}s.
     */
    Collection<ChannelGroup> groups();

    /**
     * Adds a {@link ChannelGroup} by {@link Directory}.
     */
    boolean addChannelGroup(Directory directory, ChannelGroup group);

    /**
     * Removes a {@link ChannelGroup} by {@link Directory}.
     */
    boolean removeChannelGroup(Directory directory, ChannelGroup group);

    /**
     * Returns list of {@link ChannelGroup}s by the same {@link Directory}.
     */
    CopyOnWriteGroupList directory(Directory directory);

    /**
     * Returns {@code true} if has available {@link ChannelGroup}s
     * on this {@link Directory}.
     */
    boolean isDirectoryAvailable(Directory directory);

    /**
     * Returns the {@link DirectoryChannelGroup}.
     */
    DirectoryChannelGroup directoryGroup();

    /**
     * Returns the {@link ConnectionManager}.
     */
    ConnectionManager connectionManager();

    /**
     * Shutdown the server.
     */
    void shutdownGracefully();

    interface ConnectionWatcher {

        /**
         * Start to connect to server.
         */
        void start();

        /**
         * Wait until the connections is available or timeout,
         * if available return true, otherwise return false.
         */
        boolean waitForAvailable(long timeoutMillis);
    }
}

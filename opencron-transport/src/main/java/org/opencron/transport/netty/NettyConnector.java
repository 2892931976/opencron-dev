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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;
import org.opencron.common.logging.InternalLogger;
import org.opencron.common.logging.InternalLoggerFactory;
import org.opencron.common.util.ClassUtils;
import org.opencron.common.util.Constants;
import org.opencron.common.util.ContainerUtils;
import org.opencron.common.util.NamedThreadFactory;
import org.opencron.transport.api.*;
import org.opencron.transport.api.channel.CopyOnWriteGroupList;
import org.opencron.transport.api.channel.DirectoryChannelGroup;
import org.opencron.transport.api.channel.ChannelGroup;
import org.opencron.transport.api.processor.ConsumerProcessor;
import org.opencron.transport.netty.channel.NettyChannelGroup;
import org.opencron.transport.netty.estimator.MessageSizeEstimator;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;

import static org.opencron.common.util.AssertUtils.checkNotNull;

public abstract class NettyConnector implements Connector<Connection> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyConnector.class);

    static {
        // touch off DefaultChannelId.<clinit>
        // because getProcessId() sometimes too slow
        ClassUtils.classInitialize("io.netty.channel.DefaultChannelId", 500);
    }

    protected final Protocol protocol;
    protected final HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("connector.timer"));

    private final ConcurrentMap<UnresolvedAddress, ChannelGroup> addressGroups = ContainerUtils.newConcurrentMap(true);
    private final DirectoryChannelGroup directoryGroup = new DirectoryChannelGroup();
    private final ConnectionManager connectionManager = new ConnectionManager();

    private Bootstrap bootstrap;
    private EventLoopGroup worker;
    private int nWorkers;

    protected volatile ByteBufAllocator allocator;

    public NettyConnector(Protocol protocol) {
        this(protocol, Constants.AVAILABLE_PROCESSORS + 1);
    }

    public NettyConnector(Protocol protocol, int nWorkers) {
        this.protocol = protocol;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory workerFactory = workerThreadFactory("opencron.connector");
        worker = initEventLoopGroup(nWorkers, workerFactory);

        bootstrap = new Bootstrap().group(worker);

        Config child = config();
        child.setOption(Option.IO_RATIO, 100);
        child.setOption(Option.PREFER_DIRECT, true);
        child.setOption(Option.USE_POOLED_ALLOCATOR, true);

        doInit();
    }

    protected abstract void doInit();

    protected ThreadFactory workerThreadFactory(String name) {
        return new DefaultThreadFactory(name, Thread.MAX_PRIORITY);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public void withProcessor(ConsumerProcessor processor) {
        // the default implementation does nothing
    }

    @Override
    public ChannelGroup group(UnresolvedAddress address) {
        checkNotNull(address, "address");

        ChannelGroup group = addressGroups.get(address);
        if (group == null) {
            ChannelGroup newGroup = channelGroup(address);
            group = addressGroups.putIfAbsent(address, newGroup);
            if (group == null) {
                group = newGroup;
            }
        }
        return group;
    }

    @Override
    public Collection<ChannelGroup> groups() {
        return addressGroups.values();
    }

    @Override
    public boolean addChannelGroup(Directory directory, ChannelGroup group) {
        CopyOnWriteGroupList groups = directory(directory);
        boolean added = groups.addIfAbsent(group);
        if (added) {
            logger.info("Added channel group: {} to {}.", group, directory.directory());
        }
        return added;
    }

    @Override
    public boolean removeChannelGroup(Directory directory, ChannelGroup group) {
        CopyOnWriteGroupList groups = directory(directory);
        boolean removed = groups.remove(group);
        if (removed) {
            logger.warn("Removed channel group: {} in directory: {}.", group, directory.directory());
        }
        return removed;
    }

    @Override
    public CopyOnWriteGroupList directory(Directory directory) {
        return directoryGroup.find(directory);
    }

    @Override
    public boolean isDirectoryAvailable(Directory directory) {
        CopyOnWriteGroupList groups = directory(directory);
        ChannelGroup[] snapshot = groups.snapshot();
        for (ChannelGroup g : snapshot) {
            if (g.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DirectoryChannelGroup directoryGroup() {
        return directoryGroup;
    }

    @Override
    public ConnectionManager connectionManager() {
        return connectionManager;
    }

    @Override
    public void shutdownGracefully() {
        connectionManager.cancelAllReconnect();
        worker.shutdownGracefully();
    }

    protected void setOptions() {
        Config child = config();

        setIoRatio(child.getOption(Option.IO_RATIO));

        boolean direct = child.getOption(Option.PREFER_DIRECT);
        if (child.getOption(Option.USE_POOLED_ALLOCATOR)) {
            if (direct) {
                allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new PooledByteBufAllocator(false);
            }
        } else {
            if (direct) {
                allocator = new UnpooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new UnpooledByteBufAllocator(false);
            }
        }
        bootstrap.option(ChannelOption.ALLOCATOR, allocator)
                .option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, MessageSizeEstimator.DEFAULT);
    }

    /**
     * A {@link Bootstrap} that makes it easy to bootstrap a {@link io.netty.channel.Channel} to use
     * for clients.
     */
    protected Bootstrap bootstrap() {
        return bootstrap;
    }

    /**
     * Lock object with bootstrap.
     */
    protected Object bootstrapLock() {
        return bootstrap;
    }

    /**
     * The {@link EventLoopGroup} for the child. These {@link EventLoopGroup}'s are used to handle
     * all the events and IO for {@link io.netty.channel.Channel}'s.
     */
    protected EventLoopGroup worker() {
        return worker;
    }

    /**
     * Creates the same address of the channel group.
     */
    protected ChannelGroup channelGroup(UnresolvedAddress address) {
        return new NettyChannelGroup(address);
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.
     * The default value is {@code 50}, which means the event loop will try to spend the same
     * amount of time for I/O as for non-I/O tasks.
     */
    public abstract void setIoRatio(int workerIoRatio);

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory}.
     */
    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);
}

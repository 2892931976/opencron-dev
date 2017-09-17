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

package org.opencron.transport.api.channel;


import org.opencron.common.util.ContainerUtils;
import org.opencron.transport.api.Directory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectoryChannelGroup {

    // key: 服务标识; value: 提供服务的节点列表(group list)
    private final ConcurrentMap<String, CopyOnWriteGroupList> groups = ContainerUtils.newConcurrentMap(true);
    // 对应服务节点(group)的引用计数
    private final GroupRefCounterMap groupRefCounter = new GroupRefCounterMap();

    public CopyOnWriteGroupList find(Directory directory) {
        String _directory = directory.directory();
        CopyOnWriteGroupList groupList = groups.get(_directory);
        if (groupList == null) {
            CopyOnWriteGroupList newGroupList = new CopyOnWriteGroupList(this);
            groupList = groups.putIfAbsent(_directory, newGroupList);
            if (groupList == null) {
                groupList = newGroupList;
            }
        }
        return groupList;
    }

    /**
     * 获取指定group的引用计数
     */
    public int getRefCount(ChannelGroup group) {
        AtomicInteger counter = groupRefCounter.get(group);
        if (counter == null) {
            return 0;
        }
        return counter.get();
    }

    /**
     * 指定group的引用计数 +1
     */
    public int incrementRefCount(ChannelGroup group) {
        return groupRefCounter.getOrCreate(group).incrementAndGet();
    }

    /**
     * 指定group的引用计数 -1, 如果引用计数为 0 则remove
     */
    public int decrementRefCount(ChannelGroup group) {
        AtomicInteger counter = groupRefCounter.get(group);
        if (counter == null) {
            return 0;
        }
        int count = counter.decrementAndGet();
        if (count == 0) {
            // get与remove并不是原子操作, 但在当前场景是可接受的
            groupRefCounter.remove(group);
        }
        return count;
    }

    static class GroupRefCounterMap extends ConcurrentHashMap<ChannelGroup, AtomicInteger> {

        private static final long serialVersionUID = 6590976614405397299L;

        public AtomicInteger getOrCreate(ChannelGroup key) {
            AtomicInteger counter = super.get(key);
            if (counter == null) {
                AtomicInteger newCounter = new AtomicInteger(0);
                counter = super.putIfAbsent(key, newCounter);
                if (counter == null) {
                    counter = newCounter;
                }
            }
            return counter;
        }
    }
}

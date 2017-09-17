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


import org.opencron.common.logging.InternalLogger;
import org.opencron.common.logging.InternalLoggerFactory;
import org.opencron.common.util.ContainerUtils;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class ConnectionManager {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionManager.class);

    private final ConcurrentMap<UnresolvedAddress, CopyOnWriteArrayList<Connection>> connections = ContainerUtils.newConcurrentMap(true);

    /**
     * 设置为由opencron自动管理连接
     */
    public void manage(Connection connection) {
        UnresolvedAddress address = connection.getAddress();
        CopyOnWriteArrayList<Connection> list = connections.get(address);
        if (list == null) {
            CopyOnWriteArrayList<Connection> newList = new CopyOnWriteArrayList<Connection>();
            list = connections.putIfAbsent(address, newList);
            if (list == null) {
                list = newList;
            }
        }
        list.add(connection);
    }

    /**
     * 取消对指定地址的自动重连
     */
    public void cancelReconnect(UnresolvedAddress address) {
        CopyOnWriteArrayList<Connection> list = connections.remove(address);
        if (list != null) {
            for (Connection c : list) {
                c.setReconnect(false);
            }
            logger.warn("Cancel reconnect to: {}.", address);
        }
    }

    /**
     * 取消对所有地址的自动重连
     */
    public void cancelAllReconnect() {
        for (UnresolvedAddress address : connections.keySet()) {
            cancelReconnect(address);
        }
    }
}

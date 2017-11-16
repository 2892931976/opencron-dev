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
package org.opencron.rpc.mina;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author benjobs
 */
public class ConnectWrapper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private ConnectFuture connectFuture;

    private IoSession ioSession;

    public ConnectWrapper(ConnectFuture connectFuture) {
        this.connectFuture = connectFuture;
        this.ioSession = connectFuture.getSession();
    }

    public boolean isActive() {
        return this.connectFuture!=null && this.connectFuture.isConnected();
    }

    public void close() {
        if (this.ioSession != null) {
            if (this.ioSession.isConnected()) {
                ioSession.getCloseFuture().awaitUninterruptibly();
            }
            this.ioSession.close(true);
            this.ioSession = null;
        }
        if (this.connectFuture != null) {
            this.connectFuture.cancel();
            this.connectFuture = null;
        }
    }

    public ConnectFuture getConnectFuture() {
        return connectFuture;
    }
}
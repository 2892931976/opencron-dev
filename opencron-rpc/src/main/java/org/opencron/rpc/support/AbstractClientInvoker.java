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

package org.opencron.rpc.support;

import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.job.RpcType;
import org.opencron.rpc.Client;
import org.opencron.rpc.InvokeCallback;
import org.opencron.rpc.ClientInvoker;


/**
 * @author <a href="mailto:benjobs@qq.com">B e n</a>
 * @version 1.0
 * @date 2016-03-27
 */

public class AbstractClientInvoker implements ClientInvoker {

    private Client client = ExtensionLoader.load(Client.class);

    //同步调用
    public Response sentSync(Request request) {
        try {
            return client.sentSync(request.setRpcType(RpcType.SYNC));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //单向调用
    public void sentOneway(Request request) {
        try {
            client.sentOneway(request.setRpcType(RpcType.ONE_WAY));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //异步调用...
    public void sentAsync(Request request, InvokeCallback callback) {
        try {
            client.sentAsync(request.setRpcType(RpcType.ASYNC),callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

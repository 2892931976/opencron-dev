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

package org.opencron.rpc;

import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.job.RpcType;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * agent OpencronCaller
 *
 * @author <a href="mailto:benjobs@qq.com">B e n</a>
 * @version 1.0
 * @date 2016-03-27
 */

@Component
public class RpcInvoker implements Invoker {

    //同步调用
    public Response callSync(Request request) {
        try {
            request.setRpcType(RpcType.SYNC).setId(new AtomicInteger(0).incrementAndGet());
            RpcClient rpcClient = new RpcClient();
            Response response = rpcClient.sendSync(request);
            System.out.println("send request:"+request.getId()+", receive response id:"+response.getId()+",result:"+response.getResult());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //单向调用
    public void callOneway(Request request) {
        try {
            request.setRpcType(RpcType.ONE_WAY).setId(new AtomicInteger(0).incrementAndGet());
            RpcClient rpcClient = new RpcClient();
            rpcClient.sendOneway(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //异步调用...
    public void callAsync(Request request, InvokeCallback callback) {
        try {
            request.setRpcType(RpcType.ASYNC).setId(new AtomicInteger(0).incrementAndGet());
            RpcClient rpcClient = new RpcClient();
            rpcClient.sendAsync(request, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

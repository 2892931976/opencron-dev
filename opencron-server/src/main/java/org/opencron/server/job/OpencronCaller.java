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

package org.opencron.server.job;

import com.alibaba.fastjson.JSON;
import org.opencron.common.Constants;
import org.opencron.common.job.Action;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.util.CommonUtils;
import org.opencron.common.util.collection.ParamsMap;
import org.opencron.rpc.InvokeCallback;
import org.opencron.rpc.support.AbstractRpcInvoker;
import org.opencron.server.domain.Agent;
import org.opencron.server.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * agent OpencronCaller
 *
 * @author <a href="mailto:benjobs@qq.com">B e n</a>
 * @version 1.0
 * @date 2016-03-27
 */

@Component
public class OpencronCaller extends AbstractRpcInvoker {

    @Autowired
    private AgentService agentService;

    //同步调用
    public Response sentSync(Request request) {
        checkProxyAgent(request);
        try {
            return super.sentSync(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //单向调用
    public void sentOneway(Request request) {
        checkProxyAgent(request);
        try {
            super.sentOneway(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //异步调用...
    public void sentAsync(Request request, InvokeCallback callback) {
        checkProxyAgent(request);
        try {
            super.sentAsync(request, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkProxyAgent(Request request) {
        if ( request.getProxyAgent()!=null ) {
            ParamsMap proxyParams = new ParamsMap();
            proxyParams.put(
                    Constants.PARAM_PROXYHOST_KEY, request.getHost(),
                    Constants.PARAM_PROXYPORT_KEY, request.getPort(),
                    Constants.PARAM_PROXYACTION_KEY,request.getAction().name(),
                    Constants.PARAM_PROXYPASSWORD_KEY, request.getPassword()
            );

            if (CommonUtils.notEmpty(request.getParams())) {
                proxyParams.put(Constants.PARAM_PROXYPARAMS_KEY, JSON.toJSONString(request.getParams()));
            }

            Agent proxyAgent = agentService.getAgent(request.getProxyAgent());
            request.setHost(proxyAgent.getHost());
            request.setPort(proxyAgent.getPort());
            request.setAction(Action.PROXY);
            request.setPassword(proxyAgent.getPassword());
            request.setParams(proxyParams);
        }
    }

}

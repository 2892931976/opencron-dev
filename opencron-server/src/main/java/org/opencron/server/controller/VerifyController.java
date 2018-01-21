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

package org.opencron.server.controller;

import org.opencron.common.Constants;
import org.opencron.server.domain.Agent;
import it.sauronsoftware.cron4j.SchedulingPattern;
import org.opencron.server.service.AgentService;
import org.opencron.server.service.ExecuteService;
import org.opencron.server.vo.Status;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
@RequestMapping("verify")
public class VerifyController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private AgentService agentService;

    @RequestMapping(value = "exp.do", method = RequestMethod.POST)
    @ResponseBody
    public Status validateCronExp(Integer cronType, String cronExp) {
        boolean pass = false;
        if (cronType == 0) pass = SchedulingPattern.validate(cronExp);
        if (cronType == 1) pass = CronExpression.isValidExpression(cronExp);
        return Status.FALSE;
    }

    @RequestMapping(value = "ping.do", method = RequestMethod.POST)
    @ResponseBody
    public Status validatePing(int proxy, Long proxyId, String host, Integer port, String password) {
        Agent agent = new Agent();
        agent.setProxy(proxy);
        agent.setHost(host);
        agent.setPort(port);
        agent.setPassword(password);

        if (proxy == Constants.ConnType.PROXY.getType()) {
            agent.setProxy(Constants.ConnType.CONN.getType());
            if (proxyId != null) {
                Agent proxyAgent = agentService.getAgent(proxyId);
                if (proxyAgent == null) {
                    return Status.FALSE;
                }
                agent.setProxyAgent(proxyId);
                //需要代理..
                agent.setProxy(Constants.ConnType.PROXY.getType());
            }
        }
        boolean ping = executeService.ping(agent);

        if (!ping) {
            logger.error(String.format("validate host:%s,port:%s cannot ping!", agent.getHost(), port));
        }
        return new Status(ping);
    }

    @RequestMapping(value = "guid.do", method = RequestMethod.POST)
    @ResponseBody
    public String getGuid(int proxy, Long proxyId, String host, Integer port, String password, HttpServletResponse response) {
        Agent agent = new Agent();
        agent.setProxy(proxy);
        agent.setHost(host);
        agent.setPort(port);
        agent.setPassword(password);

        if (proxy == Constants.ConnType.PROXY.getType()) {
            agent.setProxy(Constants.ConnType.CONN.getType());
            if (proxyId != null) {
                Agent proxyAgent = agentService.getAgent(proxyId);
                if (proxyAgent == null) {
                    return null;
                }
                agent.setProxyAgent(proxyId);
                //需要代理..
                agent.setProxy(Constants.ConnType.PROXY.getType());
            }
        }
        return executeService.guid(agent);
    }
}

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

import org.opencron.common.job.Opencron;
import org.opencron.common.util.CommonUtils;
import org.opencron.server.domain.Agent;
import org.opencron.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpencronTask implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(OpencronTask.class);

    @Autowired
    private AgentService agentService;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private OpencronHeartBeat opencronHeartBeat;

    @Override
    public void afterPropertiesSet() throws Exception {
        configService.initDataBase();
        //检测所有的agent...
        clearCache();
        //通知所有的agent,启动心跳检测...
        allAgentHeartbeat();
        schedulerService.initQuartz(executeService);
        schedulerService.initCrontab();
    }

    private void clearCache() {
        OpencronTools.CACHE.remove(OpencronTools.CACHED_AGENT_ID);
        OpencronTools.CACHE.remove(OpencronTools.CACHED_JOB_ID);
    }

    private void allAgentHeartbeat() throws Exception {
        logger.info("[opencron]:checking Agent connection...");
        List<Agent> agents = agentService.getAll();
        if (CommonUtils.notEmpty(agents)) {
            for (Agent agent:agents) {
                opencronHeartBeat.heartbeat(agent);
            }
        }
    }

    /**
     *
     * 一分钟扫描一次已经失联的Agent，如果Agent失联后重启会自动连接上...
     * @throws Exception
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void disconnectedAgentHeartbeat() throws Exception {
        List<Agent> agents = agentService.getAgentByConnStatus(Opencron.ConnStatus.DISCONNECTED);
        if (CommonUtils.notEmpty(agents)) {
            for (Agent agent:agents) {
                opencronHeartBeat.heartbeat(agent);
            }
        }
    }

}

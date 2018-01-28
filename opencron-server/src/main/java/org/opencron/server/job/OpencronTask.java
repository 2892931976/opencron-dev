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

import org.opencron.common.Constants;
import org.opencron.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class OpencronTask {

    private final Logger logger = LoggerFactory.getLogger(OpencronTask.class);

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private SchedulerService schedulerService;

    @PostConstruct
    public void initialization() throws Exception {
        configService.initDataBase();
        //检测所有的agent...
        clearCache();
        schedulerService.initQuartz(executeService);
        schedulerService.initCrontab();
    }

    private void clearCache() {
        OpencronTools.CACHE.remove(Constants.PARAM_CACHED_AGENT_ID_KEY);
        OpencronTools.CACHE.remove(Constants.PARAM_CACHED_JOB_ID_KEY);
    }

}

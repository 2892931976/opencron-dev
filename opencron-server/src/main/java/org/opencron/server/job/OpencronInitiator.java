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

import net.sf.ehcache.store.chm.ConcurrentHashMap;
import org.opencron.common.Constants;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.ConsistentHash;
import org.opencron.common.util.PropertyPlaceholder;
import org.opencron.registry.URL;
import org.opencron.registry.api.RegistryService;
import org.opencron.registry.zookeeper.ChildListener;
import org.opencron.server.service.*;
import org.opencron.server.vo.JobInfo;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;

import static org.opencron.common.util.CommonUtils.uuid;

/**
 * @author benjobs.
 */

@Component
public class OpencronInitiator {

    private static final Logger logger = LoggerFactory.getLogger(OpencronInitiator.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private JobService jobService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private AgentService agentService;

    private RegistryService registryService = new RegistryService();

    private final URL registryURL = URL.valueOf(PropertyPlaceholder.get(Constants.PARAM_OPENCRON_REGISTRY_KEY));

    private final String SERVER_ID = uuid();

    private final String registryPath = Constants.ZK_REGISTRY_SERVER_PATH + "/" + SERVER_ID;

    private Map<String,String> agentMap = new ConcurrentHashMap<String, String>(0);

    /**
     * 每台server启动起来都必须往注册中心注册信息...注册中心在重新统一分配任务到每台server上...
     *
     * @throws Exception
     */
    @PostConstruct
    public void initialization() throws Exception {

        //初始化数据库...
        configService.initDataBase();

        //监控agent的增加和删除
        agentMonitor();

        //server的监控和动态分配任务.
        serverMonitor();
    }

    private void agentMonitor() {
        //agent添加,删除监控...
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_AGENT_PATH, new ChildListener() {
            @Override
            public synchronized void childChanged(String path, List<String> children) {
                if (agentMap.isEmpty()) {
                    for (String agent:children) {
                        agentMap.put(agent,agent);
                        logger.info("[opencron] agent connected! info:{}",agent);
                        agentService.doConnect(agent);
                    }
                }else {
                    Map<String,String> map = new ConcurrentHashMap<String, String>(agentMap);
                    for (String agent:children) {
                        map.remove(agent);
                        if (!agentMap.containsKey(agent)) {
                            //新增...
                            agentMap.put(agent,agent);
                            logger.info("[opencron] agent connected! info:{}",agent);
                            agentService.doConnect(agent);
                        }
                    }

                    for (String child:map.keySet()) {
                        agentMap.remove(child);
                        logger.info("[opencron] agent doDisconnect! info:{}",child);
                        agentService.doDisconnect(child);
                    }
                }
            }
        });
    }

    private void serverMonitor() {

        //server监控增加和删除
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_SERVER_PATH, new ChildListener() {
            @Override
            public synchronized void childChanged(String path, List<String> children) {
                try {
                    //一致性哈希计算出每个Job落在哪个server上
                    ConsistentHash<String> hash = new ConsistentHash<String>(160,children);

                    List<JobInfo> crontab = jobService.getJobInfo(Constants.CronType.CRONTAB);
                    //落在该机器上的crontab任务
                    for (JobInfo jobInfo:crontab) {
                        String server = hash.get(jobInfo.getJobId());
                        //该任务落在当前的机器上
                        if (server.equals(SERVER_ID)) {
                            schedulerService.syncTigger(jobInfo);
                        }
                    }

                    List<JobInfo> quartzJobs = jobService.getJobInfo(Constants.CronType.QUARTZ);
                    for (JobInfo jobInfo:quartzJobs) {
                        String server = hash.get(jobInfo.getJobId());
                        //落在该机器上的quartz任务
                        if (server.equals(SERVER_ID)) {
                            schedulerService.syncTigger(jobInfo);
                        }
                    }
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
            }
        });

        //将server加入到注册中心
        registryService.register(registryURL,registryPath,true);

        //register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (logger.isInfoEnabled()) {
                    logger.info("[opencron] run shutdown hook now...");
                }
                registryService.unregister(registryURL,registryPath);
            }
        }, "OpencronShutdownHook"));

    }

    @PreDestroy
    public void destroy() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("[opencron] run destroy now...");
        }
        registryService.unregister(registryURL,registryPath);
    }

}
    

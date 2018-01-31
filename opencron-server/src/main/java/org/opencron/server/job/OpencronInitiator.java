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
import org.opencron.common.util.CommonUtils;
import org.opencron.common.util.ConsistentHash;
import org.opencron.common.util.PropertyPlaceholder;
import org.opencron.registry.URL;
import org.opencron.registry.api.RegistryService;
import org.opencron.registry.zookeeper.ChildListener;
import org.opencron.server.domain.Job;
import org.opencron.server.service.*;
import org.opencron.server.vo.JobInfo;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.opencron.common.util.CommonUtils.toLong;
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

    private final String SERVER_ID = uuid();

    private final URL registryURL = URL.valueOf(PropertyPlaceholder.get(Constants.PARAM_OPENCRON_REGISTRY_KEY));

    private final String registryPath = Constants.ZK_REGISTRY_SERVER_PATH + "/" + SERVER_ID;

    private Map<String,String> agents = new ConcurrentHashMap<String, String>(0);

    private Map<String,String> jobMap = new ConcurrentHashMap<String, String>(0);

    private List<String> servers = new ArrayList<String>(0);

    private volatile boolean destroy = false;

    private Lock lock = new ReentrantLock();

    /**
     * 每台server启动起来都必须往注册中心注册信息...注册中心在重新统一分配任务到每台server上...
     *
     * @throws Exception
     */
    @PostConstruct
    public void initialization() throws Exception {

        //初始化数据库...
        configService.initDataBase();

        //init job...
        schedulerService.initJob();

        //监控agent的增加和删除
        agentMonitor();

        //server的监控和动态分配任务.
        serverMonitor();

        //job的监控
        jobMonitor();
    }

    private void agentMonitor() {
        //agent添加,删除监控...
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_AGENT_PATH, new ChildListener() {
            @Override
            public void childChanged(String path, List<String> children) {

                lock.lock();

                if (destroy) return;

                if (agents.isEmpty()) {
                    for (String agent:children) {
                        agents.put(agent,agent);
                        logger.info("[opencron] agent connected! info:{}",agent);
                        agentService.doConnect(agent);
                    }
                }else {
                    Map<String,String> map = new ConcurrentHashMap<String, String>(agents);
                    for (String agent:children) {
                        map.remove(agent);
                        if (!agents.containsKey(agent)) {
                            //新增...
                            agents.put(agent,agent);
                            logger.info("[opencron] agent connected! info:{}",agent);
                            agentService.doConnect(agent);
                        }
                    }

                    for (String child:map.keySet()) {
                        agents.remove(child);
                        logger.info("[opencron] agent doDisconnect! info:{}",child);
                        agentService.doDisconnect(child);
                    }
                }

                lock.unlock();
            }
        });
    }

    private void serverMonitor() {
        //server监控增加和删除
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_SERVER_PATH, new ChildListener() {
            @Override
            public void childChanged(String path, List<String> children) {

                if (destroy) return;

                servers = children;

                try {

                    lock.lock();

                    //一致性哈希计算出每个Job落在哪个server上
                    ConsistentHash<String> hash = new ConsistentHash<String>(160, servers);

                    List<Job> jobs = jobService.getScheduleJob();

                    for (Job job:jobs) {
                        String jobId = job.getJobId().toString();
                        //该任务落在当前的机器上
                        if ( SERVER_ID.equals(hash.get(jobId)) ) {
                            if (!jobMap.containsKey(jobId)) {
                                jobMap.put(jobId,jobId);
                                schedulerService.syncTigger(job.getJobId());
                            }
                        }else {
                            jobMap.remove(job);
                            schedulerService.removeTigger(toLong(job));
                        }
                    }

                } catch (SchedulerException e) {
                    e.printStackTrace();
                }finally {
                    lock.unlock();
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

    private void jobMonitor() {
        //job的监控
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_JOB_PATH, new ChildListener() {
            @Override
            public synchronized void childChanged(String path, List<String> children) {

                if (destroy) return;

                try {

                    lock.lock();

                    Map<String,String> unJobMap = new HashMap<String, String>(jobMap);

                    for (String job:children) {
                        unJobMap.remove(job);
                        if ( !jobMap.containsKey(job)) {
                            ConsistentHash<String> hash = new ConsistentHash<String>(160, servers);
                            if ( hash.get(job).equals(SERVER_ID) ) {
                                jobMap.put(job,job);
                                schedulerService.syncTigger(CommonUtils.toLong(job));
                            }
                        }
                    }

                    for (String job:unJobMap.keySet()) {
                        jobMap.remove(job);
                        schedulerService.removeTigger(toLong(job));
                    }
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }

            }
        });

        List<Job> jobs = jobService.getScheduleJob();
        for (Job job:jobs) {
            registryService.register(registryURL,Constants.ZK_REGISTRY_JOB_PATH+"/"+job.getJobId(),true);
        }
    }

    @PreDestroy
    public void destroy() {
        destroy = true;
        if (logger.isInfoEnabled()) {
            logger.info("[opencron] run destroy now...");
        }

        //server unregister
        registryService.unregister(registryURL,registryPath);

        //job unregister
        if (CommonUtils.notEmpty(jobMap)) {
            for (String job:jobMap.keySet()) {
                registryService.unregister(registryURL,Constants.ZK_REGISTRY_JOB_PATH+"/"+ job);
            }
        }
    }

}
    

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

    private List<String> servers = new ArrayList<String>(0);

    private volatile boolean destroy = false;

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
            public synchronized void childChanged(String path, List<String> children) {

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
            }
        });
    }

    private void serverMonitor() {
        //server监控增加和删除
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_SERVER_PATH, new ChildListener() {
            @Override
            public synchronized void childChanged(String path, List<String> children) {

                if (destroy) return;

                try {
                    servers = children;

                    //将job添加到缓存中.
                    Map<String,String> jobMap = (Map<String, String>) OpencronTools.CACHE.get(Constants.PARAM_CACHED_JOB_MAP_KEY);
                    jobMap = jobMap == null?new ConcurrentHashMap<String, String>(0):jobMap;
                    Map<String,String> unJobMap = new ConcurrentHashMap<String, String>(jobMap);

                    //一致性哈希计算出每个Job落在哪个server上
                    ConsistentHash<String> hash = new ConsistentHash<String>(160, servers);
                    List<JobInfo> jobs = jobService.getJobInfo(Constants.CronType.CRONTAB);
                    List<JobInfo> quartzJobs = jobService.getJobInfo(Constants.CronType.QUARTZ);
                    jobs.addAll(quartzJobs);

                    //落在该机器上的任务
                    for (JobInfo jobInfo:jobs) {
                        unJobMap.remove(jobInfo.getJobId().toString());
                        String server = hash.get(jobInfo.getJobId().toString());
                        //该任务落在当前的机器上
                        if (server.equals(SERVER_ID)) {
                            if (!jobMap.containsKey(jobInfo.getJobId().toString())) {
                                jobMap.put(jobInfo.getJobId().toString(),jobInfo.getJobId().toString());
                                schedulerService.syncTigger(jobInfo);
                            }
                        }
                    }

                    /**
                     *
                     * 已经删除的job
                     * 忽略直接将任务从zookeeper中删除而不经过server
                     * 如果是经过server发生的删除job行为则该job在第一时间就从zookeeper中移除了
                     *
                     */
                    for (String job:unJobMap.keySet()) {
                        jobMap.remove(job);
                    }

                    OpencronTools.CACHE.put(Constants.PARAM_CACHED_JOB_MAP_KEY,jobMap);

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

    private void jobMonitor() {
        //job的监控
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_JOB_PATH, new ChildListener() {
            @Override
            public synchronized void childChanged(String path, List<String> children) {

                if (destroy) return;

                try {
                    //将job添加到缓存中.
                    Map<String,String> jobMap = (Map<String, String>) OpencronTools.CACHE.get(Constants.PARAM_CACHED_JOB_MAP_KEY);
                    jobMap = jobMap == null?new HashMap<String, String>(0):jobMap;
                    Map<String,String> unJobMap = new HashMap<String, String>(jobMap);

                    ConsistentHash<String> hash = new ConsistentHash<String>(160, servers);
                    for (String job:children) {
                        unJobMap.remove(job);
                        if (SERVER_ID.equals(hash.get(job))) {
                            if (!jobMap.containsKey(job)) {
                                jobMap.put(job,job);
                                schedulerService.syncTigger(CommonUtils.toLong(job));
                            }
                        }
                    }
                    /**
                     * 已经删除的job
                     * 忽略直接将任务从zookeeper中删除而不经过server
                     * 如果是经过server发生的删除job行为则该job在第一时间就从zookeeper中移除了
                     */
                    for (String job:unJobMap.keySet()) {
                        jobMap.remove(job);
                    }
                    OpencronTools.CACHE.put(Constants.PARAM_CACHED_JOB_MAP_KEY,jobMap);
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
            }
        });

        //将job加入到zookeeper
        List<JobInfo> crontab = jobService.getJobInfo(Constants.CronType.CRONTAB);
        List<JobInfo> quartz = jobService.getJobInfo(Constants.CronType.QUARTZ);
        crontab.addAll(quartz);
        for (JobInfo jobInfo:crontab) {
            registryService.register(registryURL,Constants.ZK_REGISTRY_JOB_PATH+"/"+jobInfo.getJobId(),true);
        }
    }

    @PreDestroy
    public void destroy() throws Exception {
        destroy = true;
        if (logger.isInfoEnabled()) {
            logger.info("[opencron] run destroy now...");
        }

        //server unregister
        registryService.unregister(registryURL,registryPath);

        //job unregister
        Map<String,String> jobMap = (Map<String, String>) OpencronTools.CACHE.get(Constants.PARAM_CACHED_JOB_MAP_KEY);
        if (CommonUtils.notEmpty(jobMap)) {
            for (String job:jobMap.keySet()) {
                registryService.unregister(registryURL,Constants.ZK_REGISTRY_JOB_PATH+"/"+ job);
            }
        }
    }

}
    

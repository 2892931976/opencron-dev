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


package org.opencron.server.service;

import org.opencron.server.job.OpencronCollector;
import org.opencron.server.job.OpencronRegistry;
import org.opencron.server.vo.JobInfo;
import org.quartz.*;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;


@Service
public final class SchedulerService {

    private final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private OpencronCollector opencronCollector;

    @Autowired
    private OpencronRegistry opencronRegistry;

    private Scheduler quartzScheduler;

    public SchedulerService() {}

    public boolean exists(Serializable jobId) throws SchedulerException {
        if (jobId == null || JobKey.jobKey(jobId.toString()) == null) {
            return false;
        }
        return quartzScheduler.checkExists(JobKey.jobKey(jobId.toString()));
    }

    public void put(List<JobInfo> jobs) throws SchedulerException {
        for (JobInfo jobInfo : jobs) {
            put(jobInfo);
        }
    }

    public void put(JobInfo job) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobId().toString());
        CronTrigger cronTrigger = newTrigger().withIdentity(triggerKey).withSchedule(cronSchedule(job.getCronExp())).build();

        //when exists then delete..
        if (exists(job.getJobId())) {
            this.remove(job.getJobId());
        }
        //add new job 。。。
        JobDetail jobDetail = JobBuilder.newJob(this.executeService.getClass()).withIdentity(JobKey.jobKey(job.getJobId().toString())).build();
        jobDetail.getJobDataMap().put(job.getJobId().toString(), job);
        jobDetail.getJobDataMap().put("jobBean", this.executeService);
        Date date = quartzScheduler.scheduleJob(jobDetail, cronTrigger);
        if (logger.isInfoEnabled()) {
            logger.info("opencron: add success,cronTrigger:{}", cronTrigger, date);
        }
    }

    public void remove(Serializable jobId) throws SchedulerException {
        if (exists(jobId)) {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());
            quartzScheduler.pauseTrigger(triggerKey);// 停止触发器
            quartzScheduler.unscheduleJob(triggerKey);// 移除触发器
            quartzScheduler.deleteJob(JobKey.jobKey(jobId.toString()));// 删除任务
            if (logger.isInfoEnabled()) {
                logger.info("opencron: removed, triggerKey:{},", triggerKey);
            }
        }
    }

    public void startQuartz() throws SchedulerException {
        if (quartzScheduler != null && !quartzScheduler.isStarted()) {
            quartzScheduler.start();
        }
    }

    public void shutdown() throws SchedulerException {
        if (quartzScheduler != null && !quartzScheduler.isShutdown()) {
            quartzScheduler.shutdown();
        }
    }

    public void pause(Serializable jobId) throws SchedulerException {
        if (exists(jobId)) {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());
            quartzScheduler.pauseTrigger(triggerKey);
        }
    }

    public void resume(Serializable jobId) throws SchedulerException {
        if (exists(jobId)) {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobId.toString());
            quartzScheduler.resumeTrigger(triggerKey);
        }
    }

    public void syncTigger(JobInfo job) throws Exception {
        //job已经被删除..
        if (job.getDeleted()) {
            //将该作业从zookeeper中移除掉....
            opencronRegistry.jobUnRegister(job.getJobId());
            return;
        }
        //新增或修改的job往zookeeper中同步一次...
        opencronRegistry.jobRegister(job.getJobId());

        /**
         * 如果该job在zookeeper中已经存在则zookeeper就不会有回调事件触发
         * 不能保证修改后的job也同步到crontab或quartz的任务队列里.
         * 因此需要手动调用一次
         */
        opencronRegistry.jobDistribute(job.getJobId());
    }

    public void syncTigger(Long jobId) throws Exception {
        JobInfo job = jobService.getJobInfoById(jobId);
        this.syncTigger(job);
    }

    public void initJob() throws SchedulerException {
        //crontab
        it.sauronsoftware.cron4j.Scheduler scheduler = new it.sauronsoftware.cron4j.Scheduler();
        scheduler.addTaskCollector(opencronCollector);
        scheduler.start();

        //quartz
        this.quartzScheduler = new StdSchedulerFactory().getScheduler();
        this.startQuartz();
    }


}
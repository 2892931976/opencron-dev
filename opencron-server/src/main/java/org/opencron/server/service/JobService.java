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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.opencron.common.Constants.*;

import org.opencron.common.Constants;
import org.opencron.common.util.PropertyPlaceholder;
import org.opencron.registry.URL;
import org.opencron.registry.api.RegistryService;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.Job;
import org.opencron.server.domain.User;
import org.opencron.server.job.OpencronCollector;
import org.opencron.server.job.OpencronTools;
import org.opencron.server.tag.PageBean;


import org.opencron.common.util.CommonUtils;
import org.opencron.server.vo.JobInfo;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;

import static org.opencron.common.util.CommonUtils.notEmpty;

@Service
@Transactional
public class JobService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserService userService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private OpencronCollector opencronCollector;

    private Logger logger = LoggerFactory.getLogger(JobService.class);

    public Job getJob(Long jobId) {
        return queryDao.get(Job.class, jobId);
    }

    /**
     * 获取将要执行的任务
     *
     * @return
     */
    public List<JobInfo> getJobInfo(CronType cronType) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.host,D.password FROM T_JOB AS T " +
                "LEFT JOIN T_AGENT AS D " +
                "ON T.agentId = D.agentId " +
                "WHERE IFNULL(T.flowNum,0)=0 " +
                "AND cronType=? " +
                "AND T.deleted=0";
        List<JobInfo> jobs = queryDao.sqlQuery(JobInfo.class, sql, cronType.getType());
        queryJobMore(jobs);
        return jobs;
    }

    public List<JobInfo> getJobInfoByAgentId(Long agentId, CronType cronType) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.host,D.password FROM T_JOB AS T " +
                "INNER JOIN T_AGENT D " +
                "ON T.agentId = D.agentId " +
                "WHERE IFNULL(T.flowNum,0)=0 " +
                "AND cronType=? " +
                "AND T.deleted=0 " +
                "AND D.agentId=? ";

        List<JobInfo> jobs = queryDao.sqlQuery(JobInfo.class, sql, cronType.getType(), agentId);
        queryJobMore(jobs);
        return jobs;
    }

    private void queryJobMore(List<JobInfo> jobs) {
        if (CommonUtils.notEmpty(jobs)) {
            for (JobInfo job : jobs) {
                job.setAgent(agentService.getAgent(job.getAgentId()));
                queryJobInfoChildren(job);
                queryJobUser(job);
            }
        }
    }

    public List<Job> getJobsByJobType(HttpSession session, JobType jobType) {
        String sql = "SELECT * FROM T_JOB WHERE deleted=0 AND jobType=?";
        if (JobType.FLOW.equals(jobType)) {
            sql += " AND flowNum=0";
        }
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            sql += " AND userId = " + user.getUserId() + " AND agentId IN (" + user.getAgentIds() + ")";
        }
        return queryDao.sqlQuery(Job.class, sql, jobType.getCode());
    }

    public List<Job> getAll() {
        List<Job> jobs = OpencronTools.CACHE.get(Constants.PARAM_CACHED_JOB_ID_KEY, List.class);
        if (CommonUtils.isEmpty(jobs)) {
            flushJob();
        }
        return OpencronTools.CACHE.get(Constants.PARAM_CACHED_JOB_ID_KEY, List.class);
    }

    private synchronized void flushJob() {
        OpencronTools.CACHE.put(Constants.PARAM_CACHED_JOB_ID_KEY, queryDao.sqlQuery(Job.class, "SELECT * FROM T_JOB WHERE deleted=0"));
    }

    public PageBean<JobInfo> getJobInfoPage(HttpSession session, PageBean pageBean, JobInfo job) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.host,D.password,U.userName AS operateUname " +
                "FROM T_JOB AS T " +
                "LEFT JOIN T_AGENT AS D " +
                "ON T.agentId = D.agentId " +
                "LEFT JOIN T_USER AS U " +
                "ON T.userId = U.userId " +
                "WHERE IFNULL(flowNum,0)=0 " +
                "AND T.deleted=0 ";
        if (job != null) {
            if (notEmpty(job.getAgentId())) {
                sql += " AND T.agentId=" + job.getAgentId();
            }
            if (notEmpty(job.getCronType())) {
                sql += " AND T.cronType=" + job.getCronType();
            }
            if (notEmpty(job.getJobType())) {
                sql += " AND T.jobType=" + job.getJobType();
            }
            if (notEmpty(job.getRedo())) {
                sql += " AND T.redo=" + job.getRedo();
            }
            if (!OpencronTools.isPermission(session)) {
                User user = OpencronTools.getUser(session);
                sql += " AND T.userId = " + user.getUserId() + " AND T.agentId IN (" + user.getAgentIds() + ")";
            }
        }
        pageBean = queryDao.sqlPageQuery(pageBean, JobInfo.class, sql);
        List<JobInfo> parentJobs = pageBean.getResult();

        for (JobInfo parentJob : parentJobs) {
            queryJobInfoChildren(parentJob);
        }
        pageBean.setResult(parentJobs);
        return pageBean;
    }

    private List<JobInfo> queryJobInfoChildren(JobInfo job) {
        if (job.getJobType().equals(JobType.FLOW.getCode())) {
            String sql = "SELECT T.*,D.name AS agentName,D.port,D.host,D.password,U.userName AS operateUname FROM T_JOB AS T " +
                    "LEFT JOIN T_AGENT AS D " +
                    "ON T.agentId = D.agentId " +
                    "LEFT JOIN T_USER AS U " +
                    "ON T.userId = U.userId " +
                    "WHERE T.deleted=0 " +
                    "AND T.flowId = ? " +
                    "AND T.flowNum>0 " +
                    "ORDER BY T.flowNum ASC";
            List<JobInfo> childJobs = queryDao.sqlQuery(JobInfo.class, sql, job.getFlowId());
            if (CommonUtils.notEmpty(childJobs)) {
                for (JobInfo jobInfo : childJobs) {
                    jobInfo.setAgent(agentService.getAgent(jobInfo.getAgentId()));
                }
            }
            job.setChildren(childJobs);
            return childJobs;
        }
        return Collections.emptyList();
    }


    public Job merge(Job job) {
        Job saveJob = (Job) queryDao.merge(job);
        flushJob();
        return saveJob;
    }

    public JobInfo getJobInfoById(Long id) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.host,D.password,U.username AS operateUname " +
                " FROM T_JOB AS T " +
                "LEFT JOIN T_AGENT AS D " +
                "ON T.agentId = D.agentId " +
                "LEFT JOIN T_USER AS U " +
                "ON T.userId = U.userId " +
                "WHERE T.jobId =?";
        JobInfo job = queryDao.sqlUniqueQuery(JobInfo.class, sql, id);
        if (job == null) {
            return null;
        }
        queryJobMore(Arrays.asList(job));
        return job;
    }

    private void queryJobUser(JobInfo job) {
        if (job != null && job.getUserId() != null) {
            User user = userService.getUserById(job.getUserId());
            job.setUser(user);
        }
    }

    public List<JobInfo> getJobByAgentId(Long agentId) {
        String sql = "SELECT T.*,D.name AS agentName,D.port,D.host,D.password,U.userName AS operateUname FROM T_JOB AS T " +
                "LEFT JOIN T_USER AS U " +
                "ON T.userId = U.userId " +
                "LEFT JOIN T_AGENT D " +
                "ON T.agentId = D.agentId " +
                "WHERE T.agentId =?";
        return queryDao.sqlQuery(JobInfo.class, sql, agentId);
    }

    public boolean existsName(Long jobId, Long agentId, String name) {
        String sql = "SELECT COUNT(1) FROM T_JOB WHERE agentId=? AND deleted=0 AND jobName=? ";
        if (notEmpty(jobId)) {
            sql += " AND jobId != " + jobId + " AND flowId != " + jobId;
        }
        return (queryDao.sqlCount(sql, agentId, name)) > 0L;
    }

    public String checkDelete(Long id) {
        Job job = getJob(id);
        if (job == null) {
            return "error";
        }

        //该任务是否正在执行中
        String sql = "SELECT COUNT(1) FROM T_RECORD WHERE jobId = ? AND `status`=?";
        int count = queryDao.sqlCount(sql, id, RunStatus.RUNNING.getStatus());
        if (count > 0) {
            return "false";
        }

        //流程任务则检查任务流是否在运行中...
        if (job.getJobType() == JobType.FLOW.getCode()) {
            sql = "SELECT COUNT(1) FROM T_RECORD AS R INNER JOIN (" +
                    " SELECT J.jobId FROM T_JOB AS J INNER JOIN T_JOB AS F" +
                    " ON J.flowId = F.flowId" +
                    " WHERE f.jobId = ?" +
                    " ) AS J" +
                    " on R.jobId = J.jobId" +
                    " and R.status=?";
            count = queryDao.sqlCount(sql, id, RunStatus.RUNNING.getStatus());
            if (count > 0) {
                return "false";
            }
        }

        return "true";
    }


    @Transactional(rollbackFor = Exception.class)
    public void delete(Long jobId) throws SchedulerException {
        Job job = getJob(jobId);
        if (job != null) {
            //单一任务,直接执行删除
            String sql = "UPDATE T_JOB SET deleted=1 WHERE ";
            if (job.getJobType().equals(JobType.SINGLETON.getCode())) {
                sql += " jobId=" + jobId;
            }
            if (job.getJobType().equals(JobType.FLOW.getCode())) {
                if (job.getFlowNum() == 0) {
                    //顶层流程任务,则删除一组
                    sql += " flowId=" + job.getFlowId();
                } else {
                    //其中一个子流程任务,则删除单个
                    sql += " jobId=" + jobId;
                }
            }
            queryDao.createSQLQuery(sql).executeUpdate();
            schedulerService.syncTigger(jobId);
            flushJob();
        }
    }


    public void saveFlowJob(Job job, List<Job> children) throws SchedulerException {
        job.setLastChild(false);
        job.setUpdateTime(new Date());
        job.setFlowNum(0);//顶层sort是0
        /**
         * 保存最顶层的父级任务
         */
        if (job.getJobId() != null) {
            merge(job);
            /**
             * 当前作业已有的子作业
             */
            JobInfo jobInfo = new JobInfo();
            jobInfo.setJobType(JobType.FLOW.getCode());
            jobInfo.setFlowId(job.getFlowId());

            /**
             * 取差集..
             */
            List<JobInfo> hasChildren = queryJobInfoChildren(jobInfo);
            //数据库里已经存在的子集合..
            top:
            for (JobInfo hasChild : hasChildren) {
                //当前页面提交过来的子集合...
                for (Job child : children) {
                    if (child.getJobId() != null && child.getJobId().equals(hasChild.getJobId())) {
                        continue top;
                    }
                }
                /**
                 * 已有的子作业被删除的,则做删除操作...
                 */
                delete(hasChild.getJobId());
            }
        } else {
            Job job1 = merge(job);
            job1.setFlowId(job1.getJobId());//flowId
            merge(job1);
            job.setJobId(job1.getJobId());
        }

        for (int i = 0; i < children.size(); i++) {
            Job child = children.get(i);
            /**
             * 子作业的流程编号都为顶层父任务的jobId
             */
            child.setFlowId(job.getJobId());
            child.setUserId(job.getUserId());
            child.setUpdateTime(new Date());
            child.setJobType(JobType.FLOW.getCode());
            child.setFlowNum(i + 1);
            child.setLastChild(child.getFlowNum() == children.size());
            child.setWarning(job.getWarning());
            child.setMobiles(job.getMobiles());
            child.setEmailAddress(job.getEmailAddress());
            merge(child);
        }
    }

    public boolean checkJobOwner(HttpSession session, Long userId) {
        return OpencronTools.isPermission(session) || userId.equals(OpencronTools.getUserId(session));
    }

    public boolean pauseJob(Job jobBean) {

        Job job = this.getJob(jobBean.getJobId());

        if (jobBean.getPause() == null) return false;

        if (job.getPause() != null && jobBean.getPause().equals(job.getPause())) {
            return false;
        }

        CronType cronType = CronType.getByType(job.getCronType());

        switch (cronType) {
            case QUARTZ:
                try {
                    if (jobBean.getPause()) {
                        //暂停任务
                        schedulerService.pause(jobBean.getJobId());
                    } else {
                        //恢复任务
                        schedulerService.resume(jobBean.getJobId());
                    }
                    job.setPause(jobBean.getPause());
                    merge(job);
                    return true;
                } catch (SchedulerException e) {
                    logger.error("[opencron] pauseQuartzJob error:{}", e.getLocalizedMessage());
                    return false;
                }
            case CRONTAB:
                try {
                    if (jobBean.getPause()) {
                        opencronCollector.removeTask(jobBean.getJobId());
                    } else {
                        JobInfo jobInfo = getJobInfoById(job.getJobId());
                        opencronCollector.addTask(jobInfo);
                    }
                    job.setPause(jobBean.getPause());
                    merge(job);
                    return true;
                } catch (Exception e) {
                    logger.error("[opencron] pauseCrontabJob error:{}", e.getLocalizedMessage());
                    return false;
                }
        }
        return true;
    }

    public List<Job> getFlowJob(Long id) {
        return queryDao.hqlQuery("from Job where flowId=?",id);
    }
}

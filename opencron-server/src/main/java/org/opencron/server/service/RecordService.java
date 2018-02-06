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

import org.apache.commons.collections.ArrayStack;
import org.opencron.common.Constants;
import org.opencron.common.util.CommandUtils;
import org.opencron.common.util.CommonUtils;
import org.opencron.common.util.StringUtils;
import org.opencron.common.util.collection.ParamsMap;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.Job;
import org.opencron.server.domain.Record;
import org.opencron.server.job.OpencronTools;
import org.opencron.server.domain.User;
import org.opencron.server.tag.PageBean;
import org.opencron.server.vo.ChartInfo;
import org.opencron.server.vo.RecordInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.opencron.common.util.CommonUtils.notEmpty;
import static org.opencron.common.util.CommonUtils.toLong;

@Service
@Transactional
public class RecordService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private JobService jobService;

    public PageBean query(HttpSession session, PageBean<RecordInfo> pageBean, RecordInfo recordInfo, String queryTime, boolean status) {
        String sql = "SELECT R.recordId,R.jobId,R.command,R.success,R.startTime,R.status,R.redoCount,R.jobType,R.groupId," +
                "CASE WHEN R.status IN (1,3,5,6) THEN R.endTime WHEN R.status IN (0,2,4) THEN NOW() END AS endTime," +
                "R.execType,T.jobName,T.agentId,D.name AS agentName,D.password,D.host,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                "LEFT JOIN T_JOB AS T " +
                "ON R.jobId = T.jobId " +
                "LEFT JOIN T_AGENT AS D " +
                "ON R.agentId = D.agentId " +
                "LEFT JOIN T_USER AS U " +
                "ON R.userId = U.userId " +
                "AND CASE R.jobType WHEN 1 THEN R.flowNum=0 WHEN 0 THEN R.parentId IS NULL END " +
                "WHERE R.parentId is NULL " +
                "AND R.status IN " + (status ? "(1,3,4,5,6)" : "(0,2,4)");
        if (recordInfo != null) {
            if (notEmpty(recordInfo.getSuccess())) {
                sql += " AND R.success = " + recordInfo.getSuccess() + "";
            }
            if (notEmpty(recordInfo.getAgentId())) {
                sql += " AND R.agentId = " + recordInfo.getAgentId() + " ";
            }
            if (notEmpty(recordInfo.getJobId())) {
                sql += " AND R.jobId = " + recordInfo.getJobId() + " ";
            }
            if (notEmpty(queryTime)) {
                sql += " AND date_format(R.startTime,'%Y-%m-%d')='" + queryTime + "'";
            }
            if (notEmpty(recordInfo.getExecType())) {
                sql += " AND R.execType = " + recordInfo.getExecType() + " ";
            }
            if (status) {
                sql += " AND IFNULL(R.flowNum,0) = 0 ";
            }
            if (!OpencronTools.isPermission(session)) {
                User user = OpencronTools.getUser(session);
                sql += " AND R.userId = " + user.getUserId() + " AND R.agentId in (" + user.getAgentIds() + ")";
            }
        }
        sql += " ORDER BY R.startTime DESC";
        queryDao.sqlPageQuery(pageBean, RecordInfo.class, sql);
        if (status) {
            //已完成任务的子任务及重跑记录查询
            queryChildrenAndRedo(pageBean);
        }
        return pageBean;
    }

    private void queryChildrenAndRedo(PageBean<RecordInfo> pageBean) {
        List<RecordInfo> parentRecords = pageBean.getResult();
        for (RecordInfo parentRecord : parentRecords) {
            String sql = "SELECT R.recordId,R.jobId,R.jobType,R.startTime,R.endTime,R.execType,R.status,R.redoCount,R.command,R.success,T.jobName,D.name AS agentName,D.password,D.host,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                    "INNER JOIN T_JOB AS T " +
                    "ON R.jobId = T.jobId " +
                    "LEFT JOIN T_AGENT AS D " +
                    "ON T.agentId = D.agentId " +
                    "LEFT JOIN T_USER AS U " +
                    "ON T.userId = U.userId " +
                    "WHERE R.parentId = ? " +
                    "ORDER BY R.startTime ASC ";

            //单一任务有重跑记录的，查出后并把最后一条重跑记录的执行结果记作整个任务的成功、失败状态
            if (parentRecord.getJobType() == Constants.JobType.SINGLETON.getCode() && parentRecord.getRedoCount() > 0) {
                List<RecordInfo> records = queryDao.sqlQuery(RecordInfo.class, sql, parentRecord.getRecordId());
                parentRecord.setSuccess(records.get(records.size() - 1).getSuccess());
                parentRecord.setChildRecord(records);
            }
            //流程任务，先查出父任务的重跑记录，再查出各个子任务，最后查询子任务的重跑记录，并以最后一条记录的执行结果记作整个流程任务的成功、失败状态
            if (parentRecord.getJobType() == Constants.JobType.FLOW.getCode()) {
                if (parentRecord.getRedoCount() != 0) {
                    List<RecordInfo> records = queryDao.sqlQuery(RecordInfo.class, sql, parentRecord.getRecordId());
                    //流程任务不能保证子任务也有记录，先给父任务一个成功、失败状态
                    parentRecord.setSuccess(Constants.ResultStatus.FAILED.getStatus());
                    parentRecord.setChildRecord(records);
                }
                sql = "SELECT R.recordId,R.jobId,R.jobType,R.startTime,R.endTime,R.execType,R.status,R.redoCount,R.command,R.success,R.groupId,T.jobName,T.lastChild,D.name as agentName,D.password,D.host,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                        "INNER JOIN T_JOB AS T " +
                        "ON R.jobId = T.jobId " +
                        "LEFT JOIN T_AGENT AS D " +
                        "ON T.agentId = D.agentId " +
                        "LEFT JOIN T_USER AS U " +
                        "ON T.userId = U.userId " +
                        "WHERE R.parentId IS NULL " +
                        "AND R.groupId = ? " +
                        "AND R.flowNum > 0 " +
                        "ORDER BY R.flowNum ASC ";
                List<RecordInfo> childJobs = queryDao.sqlQuery(RecordInfo.class, sql, parentRecord.getGroupId());
                if (notEmpty(childJobs)) {
                    parentRecord.setChildJob(childJobs);
                    for (RecordInfo childJob : parentRecord.getChildJob()) {
                        if (childJob.getRedoCount() > 0) {
                            sql = "SELECT R.recordId,R.jobId,R.jobType,R.startTime,R.endTime,R.execType,R.status,R.redoCount,R.command,R.success,R.parentId,T.jobName,D.name AS agentName,D.password,D.host,T.cronExp,U.userName AS operateUname FROM T_RECORD AS R " +
                                    "INNER JOIN T_JOB AS T " +
                                    "ON R.jobId = T.jobId " +
                                    "LEFT JOIN T_AGENT AS D " +
                                    "ON T.agentId = D.agentId " +
                                    "LEFT JOIN T_USER AS U " +
                                    "ON T.userId = U.userId " +
                                    "WHERE R.parentId = ? " +
                                    "ORDER BY R.startTime ASC ";
                            List<RecordInfo> childRedo = queryDao.sqlQuery(RecordInfo.class, sql, childJob.getRecordId());
                            childJob.setChildRecord(childRedo);
                        }

                    }
                    //判断整个流程任务最终执行的成功、失败状态
                    RecordInfo lastJob = childJobs.get(childJobs.size() - 1);
                    if (lastJob.getLastChild()) {
                        if (notEmpty(lastJob.getChildRecord())) {
                            parentRecord.setSuccess(lastJob.getChildRecord().get(lastJob.getChildRecord().size() - 1).getSuccess());
                        } else {
                            parentRecord.setSuccess(lastJob.getSuccess());
                        }
                    } else {
                        parentRecord.setSuccess(Constants.ResultStatus.FAILED.getStatus());
                    }
                }
            }
        }
        pageBean.setResult(parentRecords);
    }

    public RecordInfo getDetailById(Long id) {
        return queryDao.sqlUniqueQuery(
                RecordInfo.class,
                "SELECT R.recordId,R.jobType,R.jobId,R.startTime,R.endTime,R.execType,R.returnCode,R.message,R.redoCount,R.command,R.success,T.jobName,T.agentId,D.name AS agentName,D.password,D.host,T.cronExp,T.userId,U.userName AS operateUname FROM T_RECORD AS R " +
                        "LEFT JOIN T_JOB AS T " +
                        "ON R.jobId = T.jobId " +
                        "LEFT JOIN T_AGENT AS D " +
                        "ON R.agentId = D.agentId " +
                        "LEFT JOIN T_USER AS U " +
                        "ON R.userId = U.userId " +
                        "WHERE R.recordId = ?",
                id);
    }

    public Record merge(Record record) {
        record = (Record) queryDao.merge(record);
        String sql = "UPDATE T_RECORD SET startTime=NOW() WHERE recordId=?";
        queryDao.createSQLQuery(sql, record.getRecordId()).executeUpdate();
        return record;
    }

    public Record get(Long recordId) {
        return queryDao.get(Record.class, recordId);
    }

    /**
     * 只查询单一任务的
     *
     * @return
     */
    public List<Record> getReExecuteRecord() {
        String sql = "SELECT R.*,D.host,D.`name` AS agentName,D.password FROM T_RECORD AS R " +
                "INNER JOIN T_AGENT AS D " +
                "ON R.agentId = D.agentId " +
                "WHERE R.success=0 " +
                "AND R.jobType=0 " +
                "AND R.status IN(1,5) " +
                "AND R.parentId IS NULL " +
                "AND R.redo=1 " +
                "AND R.redoCount<R.runCount ";
        return queryDao.sqlQuery(Record.class, sql);
    }

    /**
     *
     * @param id
     * @return true:running false:noRun
     */
    public Boolean isRunning(Long id) {
        //找到当前任务所有执行过的流程任务
        List<Job> jobs = jobService.getFlowJob(id);

        List<Long> jobIds = Arrays.asList(id);
        if (CommonUtils.notEmpty(jobs)) {
            for (Job job:jobs) {
                jobIds.add(job.getJobId());
            }
        }

        for (Long jobId:jobIds) {
            String hql = "select count(1) from Record where jobId=:jobId and status IN (:status)";
            Integer status[] = new Integer[3];
            status[0] = Constants.RunStatus.RUNNING.getStatus();//正在运行
            status[1] = Constants.RunStatus.STOPPING.getStatus();//正在停止
            status[2] = Constants.RunStatus.RERUNNING.getStatus();//正在重跑

            Map params = ParamsMap.map().set("jobId",jobId).set("status",status);
            boolean running = toLong(queryDao.createQuery(hql,params).uniqueResult()) > 0;
            //找到该任务的任务一个单一任务或者流程任务为运行中则返回true
            if (running) {
                return true;
            }
        }
        return false;
    }

    public List<ChartInfo> getRecord(HttpSession session, String startTime, String endTime) {
        String sql = "SELECT DATE_FORMAT(r.startTime,'%Y-%m-%d') AS date, " +
                "sum(CASE r.success WHEN 0 THEN 1 ELSE 0 END) failure, " +
                "sum(CASE r.success WHEN 1 THEN 1 ELSE 0 END) success, " +
                "sum(CASE r.success WHEN 2 THEN 1 ELSE 0 END) killed, " +
                "sum(CASE r.jobType WHEN 0 THEN 1 ELSE 0 END) singleton, " +
                "sum(CASE r.jobType WHEN 1 THEN 1 ELSE 0 END) flow, " +
                "sum(CASE j.cronType WHEN 0 THEN 1 ELSE 0 END) crontab, " +
                "sum(CASE j.cronType WHEN 1 THEN 1 ELSE 0 END) quartz, " +
                "sum(CASE r.execType WHEN 0 THEN 1 ELSE 0 END) auto, " +
                "sum(CASE r.execType WHEN 1 THEN 1 ELSE 0 END) operator, " +
                "sum(CASE r.redoCount>0 WHEN 1 THEN 1 ELSE 0 END) rerun " +
                "FROM T_RECORD r left join T_JOB j ON r.jobid=j.jobid " +
                "WHERE DATE_FORMAT(r.startTime,'%Y-%m-%d') " +
                "BETWEEN '" + startTime + "' AND '" + endTime + "'";
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            sql += " AND r.userId = " + user.getUserId() + " AND r.agentId in (" + user.getAgentIds() + ")";
        }
        sql += " GROUP BY DATE_FORMAT(r.startTime,'%Y-%m-%d') ORDER BY DATE_FORMAT(r.startTime,'%Y-%m-%d') ASC";
        return queryDao.sqlQuery(ChartInfo.class, sql);
    }

    public ChartInfo getAsProgress(HttpSession session) {
        String sql = "SELECT " +
                "sum(CASE R.success WHEN 0 THEN 1 ELSE 0 END) failure, " +
                "sum(CASE R.success WHEN 1 THEN 1 ELSE 0 END) success, " +
                "sum(CASE R.success WHEN 2 THEN 1 ELSE 0 END) killed, " +
                "sum(CASE R.jobType WHEN 0 THEN 1 ELSE 0 END) singleton, " +
                "sum(CASE R.jobType WHEN 1 THEN 1 ELSE 0 END) flow, " +
                "sum(CASE J.cronType WHEN 0 THEN 1 ELSE 0 END) crontab, " +
                "sum(CASE J.cronType WHEN 1 THEN 1 ELSE 0 END) quartz, " +
                "sum(CASE R.execType WHEN 0 THEN 1 ELSE 0 END) auto, " +
                "sum(CASE R.execType WHEN 1 THEN 1 ELSE 0 END) operator, " +
                "sum(CASE R.redoCount>0 WHEN 1 THEN 1 ELSE 0 END) rerun " +
                "FROM T_RECORD R LEFT JOIN T_JOB J ON R.jobid=J.jobid WHERE 1=1 ";

        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            sql += " AND R.userId = " + user.getUserId() + " AND R.agentId in (" + user.getAgentIds() + ")";
        }
        return queryDao.sqlUniqueQuery(ChartInfo.class, sql);
    }

    @Transactional
    public synchronized void flowJobDone(Record record) {
        String sql = "UPDATE T_RECORD SET status=? WHERE groupId=?";
        queryDao.createSQLQuery(sql, Constants.RunStatus.DONE.getStatus(), record.getGroupId()).executeUpdate();
    }

    public List<Record> getRunningFlowJob(Long recordId) {
        String sql = "SELECT R.* FROM T_RECORD AS R INNER JOIN (SELECT groupId FROM T_RECORD WHERE recordId=?) AS T WHERE R.groupId = T.groupId";
        return queryDao.sqlQuery(Record.class, sql, recordId);
    }

    public Integer getRecords(HttpSession session, Constants.ResultStatus status, Constants.ExecType execType) {
        String hql = "select count(1) from Record where success=:success and exectype=:exectype and status in (:status) and (flownum is null or flownum=1)";
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            hql += " and userId = " + user.getUserId() + " and agentid IN (" + user.getAgentIds() + ")";
        }
        Map<String,String> params = ParamsMap.map().set("success",status.getStatus())
                .set("exectype",execType.getStatus())
                .set("status",Arrays.asList(Constants.RunStatus.STOPED.getStatus(), Constants.RunStatus.DONE.getStatus(), Constants.RunStatus.RERUNDONE.getStatus()));
        return queryDao.hqlCount(hql,params );
    }

    @Transactional
    public void deleteRecordBetweenTime(String startTime, String endTime) {
        if (notEmpty(startTime, endTime)) {
            String sql = "DELETE FROM T_RECORD WHERE DATE_FORMAT(startTime,'%Y-%m-%d') BETWEEN ? AND ?";
            queryDao.createSQLQuery(sql, startTime, endTime).executeUpdate();
        }
    }

    public Record getReRunningSubJob(Long recordId) {
        String sql = "SELECT * FROM T_RECORD WHERE parentId = ? ORDER BY redoCount DESC LIMIT 1";
        return queryDao.sqlUniqueQuery(Record.class, sql, recordId);
    }
}

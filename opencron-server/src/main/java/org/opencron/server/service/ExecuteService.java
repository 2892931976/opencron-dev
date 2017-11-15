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

import org.opencron.common.Constants;
import org.opencron.common.exception.PingException;
import org.opencron.common.job.Action;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.util.collection.ParamsMap;
import org.opencron.server.domain.Record;
import org.opencron.server.domain.Agent;
import org.opencron.server.domain.User;
import org.opencron.server.job.OpencronCaller;
import org.opencron.server.vo.JobVo;
import com.mysql.jdbc.PacketTooBigException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import static org.opencron.common.job.Opencron.*;

@Service
public class ExecuteService implements Job {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RecordService recordService;

    @Autowired
    private JobService jobService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private OpencronCaller caller;

    @Autowired
    private AgentService agentService;

    @Autowired
    private UserService userService;

    private Map<Long, Integer> reExecuteThreadMap = new HashMap<Long, Integer>(0);

    private static final String PACKETTOOBIG_ERROR = "在向MySQL数据库插入数据量过多,需要设定max_allowed_packet";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String key = jobExecutionContext.getJobDetail().getKey().getName();
        JobVo jobVo = (JobVo) jobExecutionContext.getJobDetail().getJobDataMap().get(key);
        try {
            ExecuteService executeService = (ExecuteService) jobExecutionContext.getJobDetail().getJobDataMap().get("jobBean");
            boolean success = executeService.executeJob(jobVo);
            this.loggerInfo("[opencron] job:{} at {}:{},execute:{}", jobVo, success ? "successful" : "failed");
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    /**
     * 基本方式执行任务，按任务类型区分
     */
    public boolean executeJob(final JobVo job) {

        JobType jobType = JobType.getJobType(job.getJobType());
        switch (jobType) {
            case SINGLETON:
                return executeSingleJob(job, job.getUserId());//单一任务
            case FLOW:
                return executeFlowJob(job);//流程任务
            default:
                return false;
        }
    }

    /**
     * 单一任务执行过程
     */
    private boolean executeSingleJob(JobVo job, Long userId) {

        if (!checkJobPermission(job.getAgentId(), userId)) return false;

        Record record = new Record(job);
        record.setJobType(JobType.SINGLETON.getCode());//单一任务
        try {
            //执行前先保存
            record = recordService.merge(record);
            //执行前先检测一次通信是否正常
            checkPing(job, record);
            Response response = responseToRecord(job, record);
            recordService.merge(record);
            if (!response.isSuccess()) {
                //当前的单一任务只运行一次未设置重跑.
                if (job.getRedo() == 0 || job.getRunCount() == 0) {
                    noticeService.notice(job, null);
                }
                this.loggerInfo("execute failed:jobName:{} at host:{},port:{},info:{}", job, record.getMessage());
                return false;
            } else {
                this.loggerInfo("execute successful:jobName:{} at host:{},port:{}", job, null);
            }
        } catch (PacketTooBigException e) {
            noticeService.notice(job, PACKETTOOBIG_ERROR);
            this.loggerError("execute failed:jobName:%s at host:%s,port:%d,info:%s", job, PACKETTOOBIG_ERROR, e);
        } catch (Exception e) {
            if (job.getRedo() == 0 || job.getRunCount() == 0) {
                noticeService.notice(job, null);
            }
            this.loggerError("execute failed:jobName:%s at host:%s,port:%d,info:%s", job, e.getMessage(), e);
        }
        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }

    /**
     * 流程任务 按流程任务处理方式区分
     */
    private boolean executeFlowJob(JobVo job) {
        if (!checkJobPermission(job.getAgentId(), job.getUserId())) return false;

        final long groupId = System.nanoTime() + Math.abs(new Random().nextInt());//分配一个流程组Id
        final Queue<JobVo> jobQueue = new LinkedBlockingQueue<JobVo>();
        jobQueue.add(job);
        jobQueue.addAll(job.getChildren());
        RunModel runModel = RunModel.getRunModel(job.getRunModel());
        switch (runModel) {
            case SEQUENCE:
                return executeSequenceJob(groupId, jobQueue);//串行任务
            case SAMETIME:
                return executeSameTimeJob(groupId, jobQueue);//并行任务
            default:
                return false;
        }
    }

    /**
     * 串行任务处理方式
     */
    private boolean executeSequenceJob(long groupId, Queue<JobVo> jobQueue) {
        for (JobVo jobVo : jobQueue) {
            if (!doFlowJob(jobVo, groupId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 并行任务处理方式
     */
    private boolean executeSameTimeJob(final long groupId, final Queue<JobVo> jobQueue) {
        final List<Boolean> result = new ArrayList<Boolean>(0);

        final Semaphore semaphore = new Semaphore(jobQueue.size());
        ExecutorService exec = Executors.newCachedThreadPool();

        for (final JobVo jobVo : jobQueue) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        semaphore.acquire();
                        result.add(doFlowJob(jobVo, groupId));
                        semaphore.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            exec.submit(task);
        }
        exec.shutdown();
        while (true) {
            if (exec.isTerminated()) {
                logger.info("[opencron]SameTimeJob,All doned!");
                return !result.contains(false);
            }
        }
    }

    /**
     * 流程任务（通用）执行过程
     */
    private boolean doFlowJob(JobVo job, long groupId) {
        Record record = new Record(job);
        record.setGroupId(groupId);//组Id
        record.setJobType(JobType.FLOW.getCode());//流程任务
        record.setFlowNum(job.getFlowNum());

        boolean success = true;

        try {
            //执行前先保存
            record = recordService.merge(record);
            //执行前先检测一次通信是否正常
            checkPing(job, record);

            Response result = responseToRecord(job, record);

            if (!result.isSuccess()) {
                recordService.merge(record);
                //被kill,直接退出
                if (StatusCode.KILL.getValue().equals(result.getExitCode())) {
                    recordService.flowJobDone(record);
                } else {
                    success = false;
                }
                return false;
            } else {
                //当前任务是流程任务的最后一个任务,则整个任务运行完毕
                if (job.getLastChild()) {
                    recordService.merge(record);
                    recordService.flowJobDone(record);
                } else {
                    //当前任务非流程任务最后一个子任务,全部流程任务为运行中...
                    record.setStatus(RunStatus.RUNNING.getStatus());
                    recordService.merge(record);
                }
                return true;
            }
        } catch (PingException e) {
            recordService.flowJobDone(record);//通信失败,流程任务挂起.
            return false;
        } catch (Exception e) {
            if (e instanceof PacketTooBigException) {
                record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at host:%s,port:%d,info:", job, PACKETTOOBIG_ERROR, e));
            } else {
                record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at host:%s,port:%d,info:%s", job, e.getMessage(), e));
            }
            record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
            record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
            record.setEndTime(new Date());
            recordService.merge(record);
            success = false;
            return false;
        } finally {
            //流程任务的重跑靠自身维护...
            if (!success) {
                Record red = recordService.get(record.getRecordId());
                if (job.getRedo() == 1 && job.getRunCount() > 0) {
                    int index = 0;
                    boolean flag;
                    do {
                        flag = reExecuteJob(red, job, JobType.FLOW);
                        ++index;
                    } while (!flag && index < job.getRunCount());

                    //重跑到截止次数还是失败,则发送通知,记录最终运行结果
                    if (!flag) {
                        noticeService.notice(job, null);
                        recordService.flowJobDone(record);
                    }
                } else {
                    noticeService.notice(job, null);
                    recordService.flowJobDone(record);
                }
            }
        }

    }

    /**
     * 多执行器同时 现场执行过程
     */
    public void batchExecuteJob(final Long userId, String command, String agentIds) {
        String[] arrayIds = agentIds.split(";");
        final Semaphore semaphore = new Semaphore(arrayIds.length);
        ExecutorService exec = Executors.newCachedThreadPool();
        for (String agentId : arrayIds) {
            Agent agent = agentService.getAgent(Long.parseLong(agentId));
            final JobVo jobVo = new JobVo(userId, command, agent);
            jobVo.setRunAs("root");
            jobVo.setSuccessExit("0");

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        semaphore.acquire();
                        executeSingleJob(jobVo, userId);
                        semaphore.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            exec.submit(task);
        }
        exec.shutdown();
        while (true) {
            if (exec.isTerminated()) {
                logger.info("[opencron]batchExecuteJob doned!");
                break;
            }
        }
    }

    /**
     * 失败任务的重执行过程
     */
    public boolean reExecuteJob(final Record parentRecord, JobVo job, JobType jobType) {

        if (parentRecord.getRedoCount().equals(reExecuteThreadMap.get(parentRecord.getRecordId()))) {
            return false;
        } else {
            reExecuteThreadMap.put(parentRecord.getRecordId(), parentRecord.getRedoCount());
        }

        parentRecord.setStatus(RunStatus.RERUNNING.getStatus());
        Record record = new Record(job);

        try {
            recordService.merge(parentRecord);
            /**
             * 当前重新执行的新纪录
             */
            job.setExecType(ExecType.RERUN.getStatus());
            record.setParentId(parentRecord.getRecordId());
            record.setGroupId(parentRecord.getGroupId());
            record.setJobType(jobType.getCode());
            parentRecord.setRedoCount(parentRecord.getRedoCount() + 1);//运行次数
            record.setRedoCount(parentRecord.getRedoCount());
            record = recordService.merge(record);

            //执行前先检测一次通信是否正常
            checkPing(job, record);

            Response result = responseToRecord(job, record);

            //当前重跑任务成功,则父记录执行完毕
            if (result.isSuccess()) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
                //重跑的某一个子任务被Kill,则整个重跑计划结束
            } else if (StatusCode.KILL.getValue().equals(result.getExitCode())) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
            } else {
                //已经重跑到最后一次了,还是失败了,则认为整个重跑任务失败,发送通知
                if (parentRecord.getRunCount().equals(parentRecord.getRedoCount())) {
                    noticeService.notice(job, null);
                }
                parentRecord.setStatus(RunStatus.RERUNUNDONE.getStatus());
            }
            this.loggerInfo("execute successful:jobName:{} at host:{},port:{}", job, null);
        } catch (Exception e) {
            if (e instanceof PacketTooBigException) {
                noticeService.notice(job, PACKETTOOBIG_ERROR);
                errorExec(record, this.loggerError("execute failed:jobName:%s at host:%s,port:%d,info:%s", job, PACKETTOOBIG_ERROR, e));
            }
            noticeService.notice(job, e.getMessage());
            errorExec(record, this.loggerError("execute failed:jobName:%s at host:%s,port:%d,info:%s", job, e.getMessage(), e));

        } finally {
            //如果已经到了任务重跑的截至次数直接更新为已重跑完成
            if (parentRecord.getRunCount().equals(parentRecord.getRedoCount())) {
                parentRecord.setStatus(RunStatus.RERUNDONE.getStatus());
            }
            try {
                recordService.merge(record);
                recordService.merge(parentRecord);
            } catch (Exception e) {
                if (e instanceof PacketTooBigException) {
                    record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at host:%s,port:%d,info:" + PACKETTOOBIG_ERROR, job, e.getMessage(), e));
                } else {
                    record.setMessage(this.loggerError("execute failed(flow job):jobName:%s at host:%s,port:%d,info:%s", job, e.getMessage(), e));
                }
            }

        }
        return record.getSuccess().equals(ResultStatus.SUCCESSFUL.getStatus());
    }

    /**
     * 终止任务过程
     */
    public boolean killJob(Record record) {

        final Queue<Record> recordQueue = new LinkedBlockingQueue<Record>();

        //单一任务
        if (JobType.SINGLETON.getCode().equals(record.getJobType())) {
            recordQueue.add(record);
        } else if (JobType.FLOW.getCode().equals(record.getJobType())) {
            //流程任务
            recordQueue.addAll(recordService.getRunningFlowJob(record.getRecordId()));
        }

        final List<Boolean> result = new ArrayList<Boolean>(0);

        final Semaphore semaphore = new Semaphore(recordQueue.size());
        ExecutorService exec = Executors.newCachedThreadPool();

        for (final Record cord : recordQueue) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    JobVo job = null;
                    try {
                        semaphore.acquire();
                        //临时的改成停止中...
                        cord.setStatus(RunStatus.STOPPING.getStatus());//停止中
                        cord.setSuccess(ResultStatus.KILLED.getStatus());//被杀.
                        recordService.merge(cord);
                        job = jobService.getJobVoById(cord.getJobId());
                        //向远程机器发送kill指令
                        caller.sentSync( Request.request(
                                        job.getHost(),
                                        job.getPort(),
                                        Action.KILL,
                                        job.getPassword(),
                                        Constants.RPC_TIMEOUT,
                                        job.getAgent().getProxyAgent()
                        ).putParam(
                                Constants.PARAM_PID_KEY,
                                cord.getPid())
                        );

                        cord.setStatus(RunStatus.STOPED.getStatus());
                        cord.setEndTime(new Date());
                        recordService.merge(cord);
                        loggerInfo("killed successful :jobName:{} at host:{},port:{},pid:{}", job, cord.getPid());
                    } catch (Exception e) {
                        if (e instanceof PacketTooBigException) {
                            noticeService.notice(job, PACKETTOOBIG_ERROR);
                            loggerError("killed error:jobName:%s at host:%s,port:%d,pid:%s", job, cord.getPid() + " failed info: " + PACKETTOOBIG_ERROR, e);
                        }
                        noticeService.notice(job, null);
                        loggerError("killed error:jobName:%s at host:%s,port:%d,pid:%s", job, cord.getPid() + " failed info: " + e.getMessage(), e);

                        logger.error("[opencron] job rumModel with SAMETIME error:{}", e.getMessage());

                        result.add(false);
                    }
                    semaphore.release();
                }
            };
            exec.submit(task);
        }
        exec.shutdown();
        while (true) {
            if (exec.isTerminated()) {
                logger.info("[opencron] SAMETIMEjob done!");
                return !result.contains(false);
            }
        }
    }

    /**
     * 向执行器发送请求，并封装响应结果
     */
    private Response responseToRecord(final JobVo job, final Record record) throws Exception {
        Response response = caller.sentSync( Request.request(
                job.getHost(),
                job.getPort(),
                Action.EXECUTE,
                job.getPassword(),
                job.getTimeout() * 6000,
                job.getAgent().getProxyAgent())
                .putParam(Constants.PARAM_COMMAND_KEY, job.getCommand())
                .putParam(Constants.PARAM_PID_KEY, record.getPid())
                .putParam(Constants.PARAM_TIMEOUT_KEY, job.getTimeout() + "")
                .putParam(Constants.PARAM_RUNAS_KEY,job.getRunAs())
                .putParam(Constants.PARAM_SUCCESSEXIT_KEY,job.getSuccessExit())
        );

        logger.info("[opencron]:execute response:{}", response.toString());
        record.setReturnCode(response.getExitCode());
        record.setMessage(response.getMessage());

        record.setSuccess(response.isSuccess() ? ResultStatus.SUCCESSFUL.getStatus() : ResultStatus.FAILED.getStatus());
        if (StatusCode.KILL.getValue().equals(response.getExitCode())) {
            record.setStatus(RunStatus.STOPED.getStatus());
            record.setSuccess(ResultStatus.KILLED.getStatus());//被kill任务失败
        } else if (StatusCode.TIME_OUT.getValue().equals(response.getExitCode())) {
            record.setStatus(RunStatus.STOPED.getStatus());
            record.setSuccess(ResultStatus.TIMEOUT.getStatus());//超时...
        } else {
            record.setStatus(RunStatus.DONE.getStatus());
        }

        record.setStartTime(new Date(response.getStartTime()));
        record.setEndTime(new Date(response.getEndTime()));
        return response;
    }

    /**
     * 调用失败后的处理
     */
    private void errorExec(Record record, String errorInfo) {
        record.setSuccess(ResultStatus.FAILED.getStatus());//程序调用失败
        record.setStatus(RunStatus.DONE.getStatus());//已完成
        record.setReturnCode(StatusCode.ERROR_EXEC.getValue());
        record.setEndTime(new Date());
        record.setMessage(errorInfo);
        recordService.merge(record);
    }


    /**
     * 任务执行前 检测通信
     */
    private void checkPing(JobVo job, Record record) throws PingException {
        boolean ping = ping(job.getAgent());
        if ( ! ping ) {
            record.setStatus(RunStatus.DONE.getStatus());//已完成
            record.setReturnCode(StatusCode.ERROR_PING.getValue());

            String format = "can't to communicate with agent:%s(%s:%d),execute job:%s failed";
            String content = String.format(format, job.getAgentName(), job.getHost(), job.getPort(), job.getJobName());

            record.setMessage(content);
            record.setSuccess(ResultStatus.FAILED.getStatus());
            record.setEndTime(new Date());
            recordService.merge(record);
            throw new PingException(content);
        }
    }

    public boolean ping(Agent agent) {
        try {
            Response response = caller.sentSync(Request.request(
                    agent.getHost(),
                    agent.getPort(),
                    Action.PING,
                    agent.getPassword(),
                    null,
                    agent.getProxyAgent()
            ));

            return response!=null && response.isSuccess();
        } catch (Exception e) {
            logger.error("[opencron]ping failed,host:{},port:{}", agent.getHost(), agent.getPort());
            return false;
        }
    }

    public String guid(Agent agent) {
        try {
            Response response = caller.sentSync(Request.request(
                    agent.getHost(),
                    agent.getPort(),
                    Action.GUID,
                    agent.getPassword(),
                    null,
                    agent.getProxyAgent())
            );
            return response.getMessage();
        } catch (Exception e) {
            logger.error("[opencron]getguid failed,host:{},port:{}", agent.getHost(), agent.getPort());
            return null;
        }
    }

    public String path(Agent agent) {
        try {
            Response response = caller.sentSync(Request.request(
                    agent.getHost(),
                    agent.getPort(),
                    Action.PATH,
                    null,
                    Constants.RPC_TIMEOUT,
                    agent.getProxyAgent())
            );
            return response.getMessage();
        } catch (Exception e) {
            logger.error("[opencron]ping failed,host:{},port:{}", agent.getHost(), agent.getPort());
            return null;
        }
    }

    /**
     * 修改密码
     */
    public boolean password(Agent agent, final String newPassword) {
        boolean ping = false;
        try {
            Response response = caller.sentSync(Request.request(
                    agent.getHost(),
                    agent.getPort(),
                    Action.PASSWORD,
                    agent.getPassword(),
                    Constants.RPC_TIMEOUT,
                    agent.getProxyAgent()
            ).putParam(
                    Constants.PARAM_NEWPASSWORD_KEY,
                    newPassword)
            );
            ping = response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ping;
    }

    /**
     * 监测执行器运行状态
     */
    public Response monitor(Agent agent) throws Exception {
        return caller.sentSync(
                Request.request(
                        agent.getHost(),
                        agent.getPort(),
                        Action.MONITOR,
                        agent.getPassword(),
                        Constants.RPC_TIMEOUT,
                        agent.getProxyAgent()
                ).setParams(ParamsMap.instance().fill("connType", ConnType.getByType(agent.getProxy()).getName())));
    }

    /**
     * 校验任务执行权限
     */
    private boolean checkJobPermission(Long jobAgentId, Long userId) {
        if (userId == null) return false;
        User user = userService.getUserById(userId);
        //超级管理员拥有所有执行器的权限
        if (user != null && user.getRoleId() == 999) return true;
        String agentIds = userService.getUserById(userId).getAgentIds();
        agentIds = "," + agentIds + ",";
        String thisAgentId = "," + jobAgentId + ",";
        return agentIds.contains(thisAgentId);
    }

    private void loggerInfo(String str, JobVo job, String message) {
        if (message != null) {
            logger.info(str, job.getJobName(), job.getHost(), job.getPort(), message);
        } else {
            logger.info(str, job.getJobName(), job.getHost(), job.getPort());
        }
    }

    private String loggerError(String str, JobVo job, String message, Exception e) {
        String errorInfo = String.format(str, job.getJobName(), job.getHost(), job.getPort(), message);
        logger.error(errorInfo, e);
        return errorInfo;
    }

}

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


package org.opencron.server.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "T_JOB")
public class Job implements Serializable {
    @Id
    @GeneratedValue
    private Long jobId;
    private Long agentId;
    private String jobName;
    private Integer cronType;
    private String cronExp;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String command;

    private String comment;
    private String runAs;
    private String successExit;
    private Long userId;
    private Date updateTime;
    private Integer redo;
    private Integer runCount;

    /**
     * 0:单一任务
     * 1:流程任务
     */
    private Integer jobType;

    private Long flowId;

    private Integer flowNum;

    private Integer runModel;//0:串行|1:并行

    //是否为流程任务的最后一个子任务
    private Boolean lastChild;

    private Boolean warning;

    private String mobiles;

    private Boolean pause;//任务是否暂停(true:已经暂停,false:未暂停)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String emailAddress;

    private Boolean deleted;//是否删除

    //运行超时的截止时间
    private Integer timeout;


    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Integer getCronType() {
        return cronType;
    }

    public void setCronType(Integer cronType) {
        this.cronType = cronType;
    }

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRunAs() {
        return runAs;
    }

    public void setRunAs(String runAs) {
        this.runAs = runAs;
    }

    public String getSuccessExit() {
        return successExit;
    }

    public void setSuccessExit(String successExit) {
        this.successExit = successExit;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getRedo() {
        return redo;
    }

    public void setRedo(Integer redo) {
        this.redo = redo;
    }

    public Integer getRunCount() {
        return runCount;
    }

    public void setRunCount(Integer runCount) {
        this.runCount = runCount;
    }

    public Integer getJobType() {
        return jobType;
    }

    public void setJobType(Integer jobType) {
        this.jobType = jobType;
    }

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public Integer getFlowNum() {
        return flowNum;
    }

    public void setFlowNum(Integer flowNum) {
        this.flowNum = flowNum;
    }

    public Integer getRunModel() {
        return runModel;
    }

    public void setRunModel(Integer runModel) {
        this.runModel = runModel;
    }

    public Boolean getLastChild() {
        return lastChild;
    }

    public void setLastChild(Boolean lastChild) {
        this.lastChild = lastChild;
    }

    public Boolean getWarning() {
        return warning;
    }

    public void setWarning(Boolean warning) {
        this.warning = warning;
    }

    public String getMobiles() {
        return mobiles;
    }

    public void setMobiles(String mobiles) {
        this.mobiles = mobiles;
    }

    public Boolean getPause() {
        return pause;
    }

    public void setPause(Boolean pause) {
        this.pause = pause;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "Job{" +
                "jobId=" + jobId +
                ", agentId=" + agentId +
                ", jobName='" + jobName + '\'' +
                ", cronType=" + cronType +
                ", cronExp='" + cronExp + '\'' +
                ", command='" + command + '\'' +
                ", comment='" + comment + '\'' +
                ", runAs='" + runAs + '\'' +
                ", successExit='" + successExit + '\'' +
                ", userId=" + userId +
                ", updateTime=" + updateTime +
                ", redo=" + redo +
                ", runCount=" + runCount +
                ", jobType=" + jobType +
                ", flowId=" + flowId +
                ", flowNum=" + flowNum +
                ", runModel=" + runModel +
                ", lastChild=" + lastChild +
                ", warning=" + warning +
                ", mobiles='" + mobiles + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", deleted=" + deleted +
                ", timeout=" + timeout +
                '}';
    }
}

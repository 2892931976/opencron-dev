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

package org.opencron.server.controller;

import javax.servlet.http.HttpSession;

import org.opencron.common.Constants;
import org.opencron.server.domain.Record;
import org.opencron.server.service.*;
import org.opencron.server.tag.PageBean;
import org.opencron.server.vo.RecordInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.opencron.common.util.CommonUtils.notEmpty;

@Controller
@RequestMapping("record")
public class RecordController extends BaseController {

    @Autowired
    private RecordService recordService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ExecuteService executeService;

    /**
     * 查询已完成任务列表
     *
     * @param pageBean
     * @param recordInfo
     * @param model
     * @return
     */
    @RequestMapping("done.htm")
    public String queryDone(HttpSession session, PageBean pageBean, RecordInfo recordInfo, String queryTime, Model model) {

        model.addAttribute("agents", agentService.getOwnerAgents(session));

        if (notEmpty(recordInfo.getSuccess())) {
            model.addAttribute("success", recordInfo.getSuccess());
        }
        if (notEmpty(recordInfo.getAgentId())) {
            model.addAttribute("agentId", recordInfo.getAgentId());
        }

        if (notEmpty(recordInfo.getAgentId())) {
            model.addAttribute("agentId", recordInfo.getAgentId());
            model.addAttribute("jobs", jobService.getJobByAgentId(recordInfo.getAgentId()));
        } else {
            model.addAttribute("jobs", jobService.getAll());
        }

        if (notEmpty(recordInfo.getJobId())) {
            model.addAttribute("jobId", recordInfo.getJobId());
        }
        if (notEmpty(queryTime)) {
            model.addAttribute("queryTime", queryTime);
        }
        if (notEmpty(recordInfo.getExecType())) {
            model.addAttribute("execType", recordInfo.getExecType());
        }
        recordService.query(session, pageBean, recordInfo, queryTime, true);

        return "/record/done";
    }

    @RequestMapping("running.htm")
    public String queryRunning(HttpSession session, PageBean pageBean, RecordInfo recordInfo, String queryTime, Model model, Boolean refresh) {

        model.addAttribute("agents", agentService.getOwnerAgents(session));

        if (notEmpty(recordInfo.getAgentId())) {
            model.addAttribute("agentId", recordInfo.getAgentId());
            model.addAttribute("jobs", jobService.getJobByAgentId(recordInfo.getAgentId()));
        } else {
            model.addAttribute("jobs", jobService.getAll());
        }

        if (notEmpty(recordInfo.getJobId())) {
            model.addAttribute("jobId", recordInfo.getJobId());
        }
        if (notEmpty(queryTime)) {
            model.addAttribute("queryTime", queryTime);
        }
        if (notEmpty(recordInfo.getExecType())) {
            model.addAttribute("execType", recordInfo.getExecType());
        }
        recordService.query(session, pageBean, recordInfo, queryTime, false);
        return refresh == null ? "/record/running" : "/record/refresh";
    }

    @RequestMapping("refresh.htm")
    public String refresh(HttpSession session, PageBean pageBean, RecordInfo recordInfo, String queryTime, Model model) {
        return this.queryRunning(session, pageBean, recordInfo, queryTime, model, true);
    }

    @RequestMapping("detail/{id}.htm")
    public String showDetail(Model model, @PathVariable("id") Long id) {
        RecordInfo recordInfo = recordService.getDetailById(id);
        if (recordInfo == null) {
            return "/error/404";
        }
        model.addAttribute("record", recordInfo);
        return "/record/detail";
    }

    @RequestMapping(value = "kill.do", method = RequestMethod.POST)
    @ResponseBody
    public boolean kill(HttpSession session, Long recordId) {
        Record record = recordService.get(recordId);
        if (Constants.RunStatus.RERUNNING.getStatus().equals(record.getStatus())) {
            //父记录临时改为停止中
            record.setStatus(Constants.RunStatus.STOPPING.getStatus());
            recordService.merge(record);
            //得到当前正在重跑的子记录
            record = recordService.getReRunningSubJob(recordId);
        }
        if (!jobService.checkJobOwner(session, record.getUserId())) return false;
        return executeService.killJob(record);
    }

}

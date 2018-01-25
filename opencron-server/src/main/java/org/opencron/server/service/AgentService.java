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

import java.util.*;

import org.opencron.common.Constants;
import org.opencron.common.util.CommonUtils;
import org.opencron.server.dao.QueryDao;
import org.opencron.server.domain.User;
import org.opencron.server.job.OpencronTools;
import org.opencron.server.tag.PageBean;
import org.apache.commons.codec.digest.DigestUtils;
import org.opencron.server.domain.Agent;
import org.opencron.server.vo.JobInfo;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;

import static org.opencron.common.util.CommonUtils.isEmpty;
import static org.opencron.common.util.CommonUtils.notEmpty;

@Service
@Transactional
public class AgentService {

    @Autowired
    private QueryDao queryDao;

    @Autowired
    private ExecuteService executeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private ConfigService configService;

    public List<Agent> getAgentByConnType(Constants.ConnType connType) {
        return queryDao.hqlQuery("from Agent where deleted=? and status=? and proxy=?", false,true,connType.getType());
    }

    public List<Agent> getAll() {
        List<Agent> agents = OpencronTools.CACHE.get(Constants.PARAM_CACHED_AGENT_ID_KEY, List.class);
        if (CommonUtils.isEmpty(agents)) {
            flushAgent();
        }
        return OpencronTools.CACHE.get(Constants.PARAM_CACHED_AGENT_ID_KEY, List.class);
    }


    private synchronized void flushAgent() {
        OpencronTools.CACHE.put(
                Constants.PARAM_CACHED_AGENT_ID_KEY,
                queryDao.hqlQuery("from Agent where deleted=?", false)
        );
    }

    public List<Agent> getOwnerAgentByConnStatus(HttpSession session, Constants.ConnStatus status) {
        String hql = "from Agent where deleted=? and status=?";
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            hql += " and agentId in (".concat(user.getAgentIds()).concat(")");
        }
        return queryDao.hqlQuery(hql, false,status.isValue());
    }

    public PageBean getOwnerAgent(HttpSession session, PageBean pageBean) {
        String hql = "from Agent where deleted=? ";
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            hql += " and agentId in (".concat(user.getAgentIds()).concat(")");
        }
        pageBean.verifyOrderBy("name", "name", "host", "port");
        hql += " order by " + pageBean.getOrderBy() + " " + pageBean.getOrder();
        queryDao.hqlPageQuery(hql,pageBean.getPageNo(),pageBean.getPageSize(),false);
        return pageBean;
    }

    public Agent getAgent(Long id) {
        Agent agent = queryDao.get(Agent.class, id);
        if (agent != null) {
            agent.setUsers(getAgentUsers(agent));
        }
        return agent;
    }

    private List<User> getAgentUsers(Agent agent) {
        String agentId = agent.getAgentId().toString();

        String hql = "from User where agentIds like ?";

        //1
        List<User> users = queryDao.hqlQuery(hql, agentId);
        if (isEmpty(users)) {
            //1,
            users = queryDao.hqlQuery(hql,agentId+",%");
        }
        if (isEmpty(users)) {
            //,1
            users = queryDao.hqlQuery(hql,",%"+agentId);
        }

        if (isEmpty(users)) {
            //,1,
            users = queryDao.hqlQuery(hql,",%"+agentId+",%");
        }

        return isEmpty(users) ? Collections.<User>emptyList() : users;
    }


    public Agent merge(Agent agent) {
        /**
         * 修改过agent
         */
        boolean update = false;
        if (agent.getAgentId() != null) {
            //从数据库获取最新的agent,防止已经被删除的agent当在监测时重新给改为非删除...
            Agent dbAgent = getAgent(agent.getAgentId());

            //已经删除的过滤掉..
            if (dbAgent.getDeleted()) {
                return agent;
            }
            update = true;
        }

        /**
         * fix bug.....
         * 修改了agent要刷新所有在任务队列里对应的作业,
         * 否则一段端口改变了,任务队列里的还是更改前的连接端口,
         * 当作业执行的时候就会连接失败...
         *
         */
        if (update) {
            agent = (Agent) queryDao.merge(agent);
            /**
             * 获取该执行器下所有的自动执行,并且是quartz类型的作业
             */
            List<JobInfo> jobInfos = jobService.getJobInfoByAgentId(agent.getAgentId(), Constants.CronType.QUARTZ);
            try {
                schedulerService.put(jobInfos, this.executeService);
            } catch (SchedulerException e) {
                /**
                 * 创新任务列表失败,抛出异常,整个事务回滚...
                 */
                throw new RuntimeException(e.getCause());
            }
        } else {
            agent = (Agent) queryDao.merge(agent);
        }

        /**
         * 同步缓存...
         */
        flushAgent();

        return agent;

    }

    public boolean existsName(Long id, String name) {
        String hql = "select count(1) from Agent where deleted=? and name=? ";
        if (notEmpty(id)) {
            hql += " and agentId !="+id;
        }
        return queryDao.hqlIntUniqueResult(hql,false,name) > 0;
    }

    public boolean checkDelete(Long id) {
        Agent agent = getAgent(id);
        if (agent == null) {
            return false;
        }
        //检查该执行器是否定义的有任务
        String hql = "select count(1) from Job where deleted=? and agentId=? ";
        return queryDao.hqlIntUniqueResult(hql,false,id) > 0;
    }

    public void delete(Long id) {
        Agent agent = getAgent(id);
        agent.setDeleted(true);
        queryDao.save(agent);
        flushAgent();
    }

    public boolean existshost(Long id, String host) {
        String hql = "select count(1) from Agent where deleted=? and host=? ";
        if (notEmpty(id)) {
            hql += " and agentId != "+id;
        }
        return queryDao.hqlIntUniqueResult(hql,false,host) > 0;
    }


    public String editPassword(Long id, Boolean type, String pwd0, String pwd1, String pwd2) {
        Agent agent = this.getAgent(id);
        boolean verify;
        if (type) {//直接输入的密钥
            agent.setPassword(pwd0);
            verify = executeService.ping(agent);
        } else {//密码...
            verify = DigestUtils.md5Hex(pwd0).equals(agent.getPassword());
        }
        if (verify) {
            if (pwd1.equals(pwd2)) {
                pwd1 = DigestUtils.md5Hex(pwd1);
                Boolean flag = executeService.password(agent, pwd1);
                if (flag) {
                    agent.setPassword(pwd1);
                    this.merge(agent);
                    flushAgent();
                    return "true";
                } else {
                    return "false";
                }
            } else {
                return "two";
            }
        } else {
            return "one";
        }
    }

    public List<Agent> getOwnerAgents(HttpSession session) {
        String hql = "from Agent where deleted=? ";
        if (!OpencronTools.isPermission(session)) {
            User user = OpencronTools.getUser(session);
            hql += " and agentid in (" + user.getAgentIds() + ")";
        }
        return queryDao.hqlQuery(hql,false);
    }

    public Agent getByHost(String host) {
        String hql = "from Agent where deleted=? and host=?";
        Agent agent = queryDao.hqlUniqueQuery(hql,false, host);
        if (agent != null) {
            agent.setUsers(getAgentUsers(agent));
        }
        return agent;
    }

    public Agent getAgentByMachineId(String machineId) {
        String hql = "from Agent where deleted=? and machineId=?";
        //不能保证macId的唯一性,可能两台机器存在同样的macId,这种概率可以忽略不计,这里为了程序的健壮性...
        List<Agent> agents = queryDao.hqlQuery(hql,false, machineId);
        if (CommonUtils.notEmpty(agents)) {
            return agents.get(0);
        }
        return null;
    }

    public List<Agent> getAgentByIds(String agentIds) {
        return queryDao.hqlQuery(String.format("from Agent where agentId in (%s)", agentIds));
    }

    public void doDisconnect(Agent agent) {
        agent.setStatus(false);
        if (CommonUtils.isEmpty(agent.getNotifyTime()) || new Date().getTime() - agent.getNotifyTime().getTime() >= configService.getSysConfig().getSpaceTime() * 60 * 1000) {
            noticeService.notice(agent);
            //记录本次任务失败的时间
            agent.setNotifyTime(new Date());
        }
        //save disconnect status to db.....
        this.merge(agent);

    }
}

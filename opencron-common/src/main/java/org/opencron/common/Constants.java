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
package org.opencron.common;

import org.opencron.common.util.SystemPropertyUtils;

import java.io.File;
import java.io.Serializable;
import java.util.regex.Pattern;

public class Constants {

    public static final int ZK_SESSION_TIMEOUT = 5000;

    public static final int RPC_TIMEOUT = 5000;

    public static final String META_INF_DIR = "META-INF/opencron/";

    public static final String ZK_REGISTRY_PATH = "/opencron";

    public static final String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

    /**
     * 注册中心是否同步存储文件，默认异步
     */
    public static final String REGISTRY_FILESAVE_SYNC_KEY = "save.file";

    public static final String FILE_KEY = "file";

    public static final String EMPTY_PROTOCOL = "empty";

    public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    public static final int HEADER_SIZE = 4;

    public static final int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    public static final String DEFAULT_KEY_PREFIX = "default.";

    //============================== param ==============================//

    public static final String PARAM_PROXYHOST_KEY                   = "proxyHost";
                                                                    
    public static final String PARAM_PROXYPORT_KEY                   = "proxyPort";
                                                                    
    public static final String PARAM_PROXYACTION_KEY                 = "proxyAction";
                                                                    
    public static final String PARAM_PROXYPASSWORD_KEY               = "proxyPassword";
                                                                    
    public static final String PARAM_PROXYPARAMS_KEY                 = "proxyParams";
                                                                    
    public static final String PARAM_MONITORPORT_KEY                 = "opencorn.monitorPort";
                                                                    
    public static final String PARAM_NEWPASSWORD_KEY                 = "newPassword";
                                                                    
    public static final String PARAM_PID_KEY                         = "pid";
                                                                    
    public static final String PARAM_COMMAND_KEY                     = "command";
                                                                    
    public static final String PARAM_TIMEOUT_KEY                     = "timeout";
                                                                    
    public static final String PARAM_RUNAS_KEY                       = "runAs";
                                                                    
    public static final String PARAM_SUCCESSEXIT_KEY                 = "successExit";
                                                                    
    public static final String PARAM_OPENCRON_AUTOREGKEY_KEY         = "opencron.autoRegKey";
                                                                    
    public static final String PARAM_OPENCRON_KEYPATH_KEY            = "opencron.keypath";
                                                                    
    public static final String PARAM_OPENCRON_SINGLELOGIN_KEY        = "opencron.singlelogin";
                                                                    
    public static final String PARAM_OPENCRON_PASSWORD_KEY           = "opencron.password";
                                                                    
    public static final String PARAM_OPENCRON_SHUTDOWN_KEY           = "opencron.shutdown";
                                                                    
    public static final String PARAM_DEF_PASSWORD_KEY                = "opencron";
                                                                    
    public static final String PARAM_OPENCRON_HOME_KEY               = "opencron.home";
                                                                    
    public static final String PARAM_OPENCRON_PORT_KEY               = "opencron.port";
                                                                    
    public static final String PARAM_DEF_OPENCRON_PORT_KEY           = "1577";
                                                                    
    public static final String PARAM_CACHED_AGENT_ID_KEY             = "opencron_agent";
                                                                    
    public static final String PARAM_CACHED_JOB_ID_KEY               = "opencron_job";
                                                                    
    public static final String PARAM_LOGIN_USER_KEY                  = "opencron_user";
                                                                    
    public static final String PARAM_LOGIN_USER_ID_KEY               = "opencron_user_id";
                                                                    
    public static final String PARAM_PERMISSION_KEY                  = "permission";
                                                                    
    public static final String PARAM_SSH_SESSION_ID_KEY              = "ssh_session_id";
                                                                    
    public static final String PARAM_HTTP_SESSION_ID_KEY             = "http_session_id";
                                                                    
    public static final String PARAM_CSRF_NAME_KEY                   = "csrf";
                                                                    
    public static final String PARAM_LOGIN_MSG_KEY                   = "loginMsg";
                                                                    
    public static final String PARAM_CONTEXT_PATH_NAME_KEY           = "contextPath";
                                                                    
    public static final String PARAM_SKIN_NAME_KEY                   = "skin";
                                                                    
    public static final String CHARSET_UTF8                          = "utf-8";
                                                                    
    public static final String CHARSET_ISO88591                      = "iso-8859-1";

    //============================== param end ==============================//
    /**
     * Name of the system property containing
     */
    public static final String OPENCRON_HOME = SystemPropertyUtils.get(PARAM_OPENCRON_HOME_KEY);

    /**
     * port
     */
    public static final Integer OPENCRON_PORT =  Integer.valueOf(SystemPropertyUtils.get(PARAM_OPENCRON_PORT_KEY,PARAM_DEF_OPENCRON_PORT_KEY)) ;
    /**
     * password
     */
    public static final String OPENCRON_PASSWORD = SystemPropertyUtils.get(PARAM_OPENCRON_PASSWORD_KEY,PARAM_DEF_PASSWORD_KEY);

    /**
     * serverurl
     */
    public static final String OPENCRON_SERVER = SystemPropertyUtils.get("opencron.server");
    /**
     * regkey
     */
    public static final String OPENCRON_REGKEY = SystemPropertyUtils.get("opencron.regkey");

    /**
     * pid
     */
    public static final File OPENCRON_PID_FILE = new File(SystemPropertyUtils.get("opencron.pid","/var/run/opencron.pid"));

    /**
     * password file
     */

    public static final File OPENCRON_PASSWORD_FILE = new File(OPENCRON_HOME + File.separator + ".password");

    /**
     * monitor file
     */
    public static final File OPENCRON_MONITOR_SHELL = new File(OPENCRON_HOME + "/bin/monitor.sh");

    /**
     * kill file
     */
    public static final File OPENCRON_KILL_SHELL = new File(OPENCRON_HOME + "/bin/kill.sh");


    public enum StatusCode implements Serializable {
        SUCCESS_EXIT(0x0, "正常退出"),
        ERROR_EXIT(0x1, "异常退出"),
        ERROR_PING(-0x63, "连接失败,ping不通"),
        KILL(0x89, "进程被kill"),
        NOTFOUND(0x7f, "未找到命令或文件"),
        ERROR_EXEC(-0x64, "连接成功，执行任务失败!"),
        ERROR_PASSWORD(-0x1f4, "密码不正确!"),
        TIME_OUT(0x1f8, "任务超时");

        private Integer value;
        private String description;

        StatusCode(Integer value, String description) {
            this.value = value;
            this.description = description;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public enum ExecType implements Serializable {

        AUTO(0x0, "auto", "自动模式,系统调用"),
        OPERATOR(0x1, "operator", "手动模式,手动调用"),
        RERUN(0x2, "rerun", "重跑模式"),
        BATCH(0x3, "batch", "现场执行");

        private Integer status;
        private String name;
        private String description;

        ExecType(Integer status, String name, String description) {
            this.status = status;
            this.name = name;
            this.description = description;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public static ExecType getByStatus(Integer status) {
            for (ExecType execType : ExecType.values()) {
                if (execType.getStatus().equals(status)) {
                    return execType;
                }
            }
            return null;
        }
    }

    public enum ConnStatus implements Serializable {
        CONNECTED(0x1,"通信成功"),
        DISCONNECTED(0x0,"通信失败");

        private Integer value;
        private String description;

        ConnStatus(Integer type, String description) {
            this.value = type;
            this.description = description;
            this.description = description;
        }

        public static ConnStatus getByValue(Integer value) {
            for (ConnStatus connStatus : ConnStatus.values()) {
                if (connStatus.getValue().equals(value)) {
                    return connStatus;
                }
            }
            return null;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public enum CronType implements Serializable {

        CRONTAB(0x0, "crontab", "crontab表达式"),
        QUARTZ(0x1, "quartz", "quartz表达式");

        private Integer type;
        private String name;
        private String description;

        CronType(Integer type, String name, String description) {
            this.type = type;
            this.name = name;
            this.description = description;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public static CronType getByType(Integer type) {
            for (CronType cronType : CronType.values()) {
                if (cronType.getType().equals(type)) {
                    return cronType;
                }
            }
            return null;
        }
    }

    public enum ResultStatus {
        FAILED(0x0, "失败"),
        SUCCESSFUL(0x1, "成功"),
        KILLED(0x2, "被杀"),
        TIMEOUT(0x3, "超时");

        private Integer status;
        private String description;

        ResultStatus(Integer status, String description) {
            this.status = status;
            this.description = description;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public enum RunStatus implements Serializable {

        RUNNING(0x0, "running", "正在运行"),
        DONE(0x1, "done", "已完成"),
        STOPPING(0x2, "stopping", "正在停止"),
        STOPED(0x3, "stoped", "已停止"),
        RERUNNING(0x4, "rerunning", "正在重跑"),
        RERUNUNDONE(0x5, "rerunundone", "重跑未完成"),
        RERUNDONE(0x6, "rerundone", "重跑完成");

        private Integer status;
        private String name;
        private String description;

        RunStatus(Integer status, String name, String description) {
            this.status = status;
            this.name = name;
            this.description = description;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public enum JobType implements Serializable {
        SINGLETON(0x0, "单一任务"),
        FLOW(0x1, "流程任务");

        private Integer code;
        private String description;

        JobType(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public static JobType getJobType(Integer type) {
            if (type == null) return null;
            for (JobType jobType : JobType.values()) {
                if (jobType.getCode().equals(type)) {
                    return jobType;
                }
            }
            return null;
        }
    }

    public enum MsgType {
        EMAIL(0x0, "邮件"),
        SMS(0x1, "短信"),
        WEBSITE(0x2, "站内信");

        private Integer value;
        private String desc;

        MsgType(Integer value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

    public enum RunModel {
        SEQUENCE(0x0, "串行"),
        SAMETIME(0x1, "并行");
        private Integer value;
        private String desc;

        RunModel(Integer value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public static RunModel getRunModel(Integer value) {
            for (RunModel model : RunModel.values()) {
                if (model.getValue().equals(value)) {
                    return model;
                }
            }
            return null;
        }
    }

    public enum ConnType {
        CONN(0x0, "conn", "直连"),
        PROXY(0x1, "proxy", "代理");

        private Integer type;
        private String name;
        private String desc;

        ConnType(Integer type, String name, String desc) {
            this.type = type;
            this.name = name;
            this.desc = desc;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public static ConnType getByType(Integer type) {
            for (ConnType connType : ConnType.values()) {
                if (connType.getType().equals(type)) {
                    return connType;
                }
            }
            return null;
        }

        public static ConnType getByName(String name) {
            for (ConnType connType : ConnType.values()) {
                if (connType.getName().equals(name)) {
                    return connType;
                }
            }
            return null;
        }

    }

}

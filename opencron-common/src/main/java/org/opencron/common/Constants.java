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
import java.util.regex.Pattern;

public class Constants {

    public static final int ZK_SESSION_TIMEOUT = 5000;

    public static final int RPC_TIMEOUT = 5000;

    public static final String META_INF_DIR = "META-INF/opencron/";

    public static final String ZK_REGISTRY_PATH = "/registry";

    public static final String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

    public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    public static final int HEADER_SIZE = 4;

    public static final int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    //============================== param ==============================//

    public static final String PARAM_PROXYHOST_KEY              = "proxyHost";

    public static final String PARAM_PROXYPORT_KEY              = "proxyPort";

    public static final String PARAM_PROXYACTION_KEY            = "proxyAction";

    public static final String PARAM_PROXYPASSWORD_KEY          = "proxyPassword";

    public static final String PARAM_PROXYPARAMS_KEY            = "proxyParams";

    public static final String PARAM_MONITORPORT_KEY            = "opencorn.monitorPort";

    public static final String PARAM_NEWPASSWORD_KEY            = "newPassword";

    public static final String PARAM_PID_KEY                    = "pid";

    public static final String PARAM_COMMAND_KEY                = "command";

    public static final String PARAM_TIMEOUT_KEY                = "timeout";

    public static final String PARAM_RUNAS_KEY                  = "runAs";

    public static final String PARAM_SUCCESSEXIT_KEY            = "successExit";

    public static final String PARAM_AUTOREGKEY_KEY             = "opencron.autoRegKey";

    public static final String PARAM_KEYPATH_KEY                = "opencron.keypath";

    public static final String PARAM_SINGLELOGIN_KEY            = "opencron.singlelogin";

    public static final String PARAM_PASSWORD_KEY               = "opencron.password";

    public static final String PARAM_DEF_PASSWORD_KEY           = "opencron";

    public static final String PARAM_OPENCRON_HOME_KEY          = "opencron.home";

    public static final String PARAM_OPENCRON_PORT_KEY          = "opencron.port";

    public static final String PARAM_DEF_OPENCRON_PORT_KEY      = "1577";

    public static final String PARAM_CACHED_AGENT_ID_KEY        = "opencron_agent";

    public static final String PARAM_CACHED_JOB_ID_KEY          = "opencron_job";

    public static final String PARAM_LOGIN_USER_KEY             = "opencron_user";

    public static final String PARAM_LOGIN_USER_ID_KEY          = "opencron_user_id";

    public static final String PARAM_PERMISSION_KEY             = "permission";

    public static final String PARAM_SSH_SESSION_ID_KEY         = "ssh_session_id";

    public static final String PARAM_HTTP_SESSION_ID_KEY        = "http_session_id";

    public static final String PARAM_CSRF_NAME_KEY              = "csrf";

    public static final String PARAM_LOGIN_MSG_KEY              = "loginMsg";

    public static final String PARAM_CONTEXT_PATH_NAME_KEY      = "contextPath";

    public static final String PARAM_SKIN_NAME_KEY              = "skin";

    public static final String CHARSET_UTF8                     = "utf-8";

    public static final String CHARSET_ISO88591                 = "iso-8859-1";

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
    public static final String OPENCRON_PASSWORD = SystemPropertyUtils.get(PARAM_PASSWORD_KEY,PARAM_DEF_PASSWORD_KEY);

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



}

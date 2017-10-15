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

package org.opencron.agent;


import org.opencron.common.util.SystemPropertyUtils;

import java.io.File;

public final class Configuration {

    /**
     * Name of the system property containing
     */
    public static final String OPENCRON_HOME = SystemPropertyUtils.get("opencron.home");

    /**
     * port
     */
    public static final String OPENCRON_PORT = SystemPropertyUtils.get("opencron.port");
    /**
     * password
     */
    public static final String OPENCRON_PASSWORD = SystemPropertyUtils.get("opencron.password");

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
    public static final File OPENCRON_PID_FILE = new File(SystemPropertyUtils.get("opencron.pid"));

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

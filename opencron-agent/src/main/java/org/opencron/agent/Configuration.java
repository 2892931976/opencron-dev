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

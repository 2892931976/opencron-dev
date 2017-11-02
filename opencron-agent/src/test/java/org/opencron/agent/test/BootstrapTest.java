package org.opencron.agent.test;

import org.apache.commons.codec.digest.DigestUtils;
import org.opencron.agent.AgentHandler;
import org.opencron.common.extension.ExtensionLoader;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.SystemPropertyUtils;
import org.opencron.rpc.Server;
import org.opencron.rpc.netty.NettyServer;
import org.slf4j.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;

public class BootstrapTest implements Serializable {

    private static final long serialVersionUID = 20150614L;

    private static final Logger logger = LoggerFactory.getLogger(SystemPropertyUtils.class);

    /**
     * thrift server
     */
    private Server server;

    /**
     * bootstrap instance....
     */
    private static BootstrapTest daemon;

    public static void main(String[] args) {

        if (daemon == null) {
            daemon = new BootstrapTest();
        }

        try {
            daemon.start();
            Thread.sleep(50000);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            handleThrowable(t);
            t.printStackTrace();
            System.exit(1);
        }
    }

    private void start() throws Exception {
        try {

            final int port = 1577;

            String password = DigestUtils.md5Hex("opencron").toLowerCase();
            SystemPropertyUtils.setProperty("opencron.port",port+"");
            SystemPropertyUtils.setProperty("opencron.password",password);

            this.server = ExtensionLoader.getExtensionLoader(Server.class).getExtension();
            //new thread to start for netty server
            new Thread(new Runnable() {
                @Override
                public void run() {
                    server.open(port, new AgentHandler());
                }
            }).start();

            logger.info("[opencron]agent started @ port:{},pid:{}", port, getPid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
    }

    private static Integer getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
        }
        return -1;
    }

}
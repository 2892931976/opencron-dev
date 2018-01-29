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

/**
 * Created by benjobs on 16/3/3.
 */

import org.apache.commons.codec.digest.DigestUtils;
import org.opencron.common.Constants;
import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.util.*;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.registry.URL;
import org.opencron.registry.api.RegistryService;
import org.opencron.rpc.ServerHandler;
import org.opencron.rpc.Server;
import org.slf4j.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.security.AccessControlException;
import java.util.Random;
import java.util.concurrent.Executors;

import static org.opencron.common.util.CommonUtils.isEmpty;
import static org.opencron.common.util.CommonUtils.notEmpty;

public class AgentBootstrap implements Serializable {


    private static final long serialVersionUID = 20150614L;


    private static Logger logger = LoggerFactory.getLogger(AgentBootstrap.class);

    /**
     * rpc server
     */
    private Server server = ExtensionLoader.load(Server.class);


    /**
     * rpc handler...
     */
    private ServerHandler handler = ExtensionLoader.load(ServerHandler.class);

    /**
     * agent port
     */
    private Integer port;

    /**
     * agent password
     */
    private String password;

    /**
     * bootstrap instance....
     */
    private static AgentBootstrap daemon;


    private volatile boolean stopAwait = false;

    /**
     * Server socket that is used to wait for the shutdown command.
     */
    private volatile ServerSocket awaitSocket = null;

    /**
     * The shutdown command string we are looking for.
     */
    private String shutdown = "stop";

    /**
     * A random number generator that is <strong>only</strong> used if
     * the shutdown command string is longer than 1024 characters.
     */
    private Random random = null;


    public static void main(String[] args) {
        if (daemon == null) {
            daemon = new AgentBootstrap();
        }

        try {
            if (isEmpty(args)) {
                logger.warn("Bootstrap: error,usage start|stop");
            } else {
                String command = args[0];
                if ("start".equals(command)) {
                    daemon.init();
                    daemon.start();
                    /**
                     * await for shundown
                     */
                    daemon.await();
                    daemon.stopServer();
                    System.exit(0);
                } else if ("stop".equals(command)) {
                    daemon.shutdown();
                } else {
                    logger.warn("Bootstrap: command \"" + command + "\" does not exist.");
                }
            }
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            handleThrowable(t);
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * init start........
     *
     * @throws Exception
     */
    private void init() throws Exception {
        port = Integer.valueOf(Constants.OPENCRON_PORT);
        String inputPassword = Constants.OPENCRON_PASSWORD;
        if (notEmpty(inputPassword)) {
            Constants.OPENCRON_PASSWORD_FILE.delete();
            this.password = DigestUtils.md5Hex(inputPassword).toLowerCase();
            IOUtils.writeText(Constants.OPENCRON_PASSWORD_FILE, this.password, Constants.CHARSET_UTF8);
        } else {
            boolean writeDefault = false;
            //.password file already exists
            if (Constants.OPENCRON_PASSWORD_FILE.exists()) {
                //read password from .password file
                String filePassowrd = IOUtils.readText(Constants.OPENCRON_PASSWORD_FILE, Constants.CHARSET_UTF8).trim().toLowerCase();
                if (notEmpty(filePassowrd)) {
                    this.password = filePassowrd;
                } else {
                    writeDefault = true;
                }
            } else {
                writeDefault = true;
            }

            if (writeDefault) {
                this.password = DigestUtils.md5Hex(AgentProperties.getProperty(Constants.PARAM_OPENCRON_PASSWORD_KEY)).toLowerCase();
                Constants.OPENCRON_PASSWORD_FILE.delete();
                IOUtils.writeText(Constants.OPENCRON_PASSWORD_FILE, this.password, Constants.CHARSET_UTF8);
            }
        }
        SystemPropertyUtils.setProperty(Constants.PARAM_OPENCRON_PORT_KEY, this.port.toString());
        SystemPropertyUtils.setProperty(Constants.PARAM_OPENCRON_PASSWORD_KEY, this.password);
    }

    private void start() {
        try {
            final int port = SystemPropertyUtils.getInt(Constants.PARAM_OPENCRON_PORT_KEY, 1577);
            //new thread to start for netty server
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    server.start(port, handler);
                }
            });
            /**
             * write pid to pidfile...
             */
            IOUtils.writeText(Constants.OPENCRON_PID_FILE, getPid(), Constants.CHARSET_UTF8);

            logger.info("[opencron]agent started @ port:{},pid:{}", port, getPid());

            String agentId = MacUtils.getMac();
            if ( agentId == null ) {
                throw new IllegalArgumentException("[opencron] getMac error.");
            }
            final String path = Constants.ZK_REGISTRY_AGENT_PATH + "/" + agentId + ":" + this.port;

            String registryAddress = AgentProperties.getProperty(Constants.PARAM_OPENCRON_REGISTRY_KEY);
            final URL url = URL.valueOf(registryAddress);
            final RegistryService registryService = new RegistryService();
            registryService.register(url,path,true);

            logger.info("[opencron] agent register to zookeeper done");

            //register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    if (logger.isInfoEnabled()) {
                        logger.info("[opencron] run shutdown hook now...");
                    }
                    registryService.unregister(url,path);
                }
            }, "OpencronShutdownHook"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void await() throws Exception {
        // Negative values - don't wait on port - opencron is embedded or we just don't like ports
        if (port == -2) {
            return;
        }

        if (port == -1) {
            try {
                while (!stopAwait) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        // continue and check the flag
                    }
                }
            } finally {
            }
            return;
        }

        Integer shutdownPort = Integer.valueOf(AgentProperties.getProperty(Constants.PARAM_OPENCRON_SHUTDOWN_KEY));

        // Set up a server socket to wait on
        try {
            awaitSocket = new ServerSocket(shutdownPort);
        } catch (IOException e) {
            logger.error("[opencron] agent .await: create[{}] ", shutdownPort, e);
            return;
        }

        try {
            // Loop waiting for a connection and a valid command
            while (!stopAwait) {
                ServerSocket serverSocket = awaitSocket;
                if (serverSocket == null) {
                    break;
                }
                // Wait for the next connection
                Socket socket = null;
                StringBuilder command = new StringBuilder();
                try {
                    InputStream stream;
                    long acceptStartTime = System.currentTimeMillis();
                    try {
                        socket = serverSocket.accept();
                        socket.setSoTimeout(10 * 1000);  // Ten seconds
                        stream = socket.getInputStream();
                    } catch (SocketTimeoutException ste) {
                        // This should never happen but bug 56684 suggests that
                        // it does.
                        logger.warn("[opencron] agentServer accept.timeout", Long.valueOf(System.currentTimeMillis() - acceptStartTime), ste);
                        continue;
                    } catch (AccessControlException ace) {
                        logger.warn("[opencron] agentServer .accept security exception: {}", ace.getMessage(), ace);
                        continue;
                    } catch (IOException e) {
                        if (stopAwait) {
                            break;
                        }
                        logger.error("[opencron] agent .await: accept: ", e);
                        break;
                    }

                    // Read a set of characters from the socket
                    int expected = 1024; // Cut off to avoid DoS attack
                    while (expected < shutdown.length()) {
                        if (random == null) {
                            random = new Random();
                        }
                        expected += (random.nextInt() % 1024);
                    }
                    while (expected > 0) {
                        int ch = -1;
                        try {
                            ch = stream.read();
                        } catch (IOException e) {
                            logger.warn("[opencron] agent .await: read: ", e);
                            ch = -1;
                        }
                        if (ch < 32)  // Control character or EOF terminates loop
                            break;
                        command.append((char) ch);
                        expected--;
                    }
                } finally {
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e) {
                    }
                }
                boolean match = command.toString().equals(shutdown);
                if (match) {
                    break;
                } else {
                    logger.warn("[opencron] agent .await: Invalid command '" + command.toString() + "' received");
                }
            }
        } finally {
            ServerSocket serverSocket = awaitSocket;
            awaitSocket = null;
            // Close the server socket and return
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

    }

    /**
     * @throws Exception
     */

    private void shutdown() throws Exception {

        String address = "localhost";

        Integer shutdownPort = Integer.valueOf(AgentProperties.getProperty(Constants.PARAM_OPENCRON_SHUTDOWN_KEY));

        // Stop the existing server
        try {
            Socket socket = new Socket(address, shutdownPort);
            OutputStream stream = socket.getOutputStream();
            for (int i = 0; i < shutdown.length(); i++) {
                stream.write(shutdown.charAt(i));
            }
            stream.flush();
            socket.close();
        } catch (ConnectException ce) {
            logger.error("[opencron] Agent.stop error:{} ", ce);
            System.exit(1);
        } catch (Exception e) {
            logger.error("[opencron] Agent.stop error:{} ", e);
            System.exit(1);
        }
    }

    private void stopServer() throws Throwable {
        this.server.destroy();
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


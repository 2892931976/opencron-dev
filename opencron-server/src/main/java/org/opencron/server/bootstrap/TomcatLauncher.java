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
package org.opencron.server.bootstrap;


import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.startup.Tomcat;
import org.opencron.common.Constants;
import org.opencron.common.util.MavenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author benjobs
 */
public class TomcatLauncher implements Launcher {

    private static final String currentPath = "";

    private Logger logger = LoggerFactory.getLogger(JettyLauncher.class);

    @Override
    public void start(boolean devMode, int port) throws Exception {

        String baseDir = currentPath;
        File webApp = new File(currentPath);

        if (devMode) {
            String artifact = MavenUtils.get(Thread.currentThread().getContextClassLoader()).getArtifactId();
            baseDir = artifact;
            webApp = new File(baseDir + "/src/main/webapp/");
        }

        Tomcat tomcat = new Tomcat();
        //host...
        tomcat.setPort(port);
        tomcat.getHost().setAppBase(currentPath);
        tomcat.setBaseDir(baseDir);
        tomcat.addWebapp(currentPath, webApp.getAbsolutePath());

        //init param
        StandardThreadExecutor executor = new StandardThreadExecutor();
        executor.setMaxThreads(Constants.WEB_THREADPOOL_SIZE);
        //一旦出现问题便于查找问题,设置标识.
        executor.setNamePrefix("opencron-tomcat-");

        tomcat.getConnector().getService().addExecutor(executor);
        tomcat.getServer().addLifecycleListener(new AprLifecycleListener());

        logger.info("[opencron] TomcatLauncher starting...");
        tomcat.start();
        tomcat.getServer().await();

    }

    @Override
    public void stop() {

    }
}

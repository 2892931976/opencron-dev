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

import static org.opencron.common.Constants.LauncherType;

import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.util.ClassLoaderUtils;
import org.opencron.common.util.MavenUtils;

import java.io.File;

/**
 * @author benjobs
 */
public class Startup {

    private static final int MIN_PORT = 0;

    private static final int MAX_PORT = 65535;

    private static final String libDirName = "lib";

    private static int startPort = 20501;

    private static boolean devMode = true;

    public static void main(String[] args) throws Exception {

        String portParam = System.getProperty("server.port");

        if (portParam == null) {
            System.out.printf("[opencron]Server At default port %d Starting...\n", startPort);
        } else {
            try {
                startPort = Integer.parseInt(portParam);
                if (startPort <= MIN_PORT || startPort > MAX_PORT) {
                    throw new IllegalArgumentException("[opencron] server port error: " + portParam);
                }
                System.out.printf("[opencron]server At port %d Starting...\n", startPort);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("[opencron] server port error: " + portParam);
            }
        }

        String launcher = System.getProperty("server.launcher");

        devMode = (launcher == null) ? true : false;

        LauncherType launcherType = (launcher == null || LauncherType.isTomcat(launcher)) ? LauncherType.TOMCAT : LauncherType.JETTY;

        String jarPath;
        if (devMode) {
            String artifact = MavenUtils.get(Thread.currentThread().getContextClassLoader()).getArtifactId();
            jarPath = artifact + File.separator + libDirName + File.separator + launcherType.getName();
            System.setProperty("catalina.home", artifact + File.separator + libDirName);
        } else {
            jarPath = libDirName + File.separator + launcherType.getName();
            System.setProperty("catalina.home", libDirName);
        }

        //load jars.
        ClassLoaderUtils.loadJars(jarPath);

        Launcher startLauncher = ExtensionLoader.load(Launcher.class, launcherType.getName());

        startLauncher.start(devMode, startPort);

    }

}


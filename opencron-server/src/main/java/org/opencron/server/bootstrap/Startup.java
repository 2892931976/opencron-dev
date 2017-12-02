package org.opencron.server.bootstrap;

import static org.opencron.common.Constants.LauncherType;

import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.util.ExtClasspathLoader;
import org.opencron.common.util.MavenUtils;

import java.io.File;

public class Startup {

    private static final int MIN_PORT = 0;

    private static final int MAX_PORT = 65535;

    private static int startPort = 8080;

    private static boolean devMode = true;

    private static String workspace = "work";

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
            jarPath = artifact + File.separator + workspace + File.separator + launcherType.getName();
            System.setProperty("catalina.home", artifact + File.separator + workspace);
        } else {
            jarPath = workspace + File.separator + launcherType.getName();
            System.setProperty("catalina.home", workspace);
        }

        //load jars.
        ExtClasspathLoader.scanJar(jarPath);

        Launcher startLauncher = ExtensionLoader.load(Launcher.class, launcherType.getName());

        startLauncher.start(devMode, startPort);

    }

}


package org.opencron.server.bootstrap;

import org.opencron.common.util.ExtClasspathLoader;
import org.opencron.common.util.MavenUtils;

import java.io.File;

public class Startup {

    private static final int MIN_PORT = 0;

    private static final int MAX_PORT = 65535;

    private static int startPort = 8080;

    private static boolean devMode = true;

    private static Launcher launcher;

    public static void main(String[] args)throws Exception {

        String portParam = System.getProperty("port");

        if (portParam == null ) {
            System.out.printf("[opencron]Server At default port {} Starting...", startPort);
        }else {
            try {
                startPort = Integer.parseInt(portParam);
                if (startPort <= MIN_PORT || startPort > MAX_PORT) {
                    throw new IllegalArgumentException("[opencron] server port error: " + portParam);
                }
                System.out.printf("[opencron]TomcatServer At port {} Starting...", startPort);
            }catch (NumberFormatException e) {
                throw new IllegalArgumentException("[opencron] server port error: " + portParam);
            }
        }

        String dev = System.getProperty("dev");

        devMode =  ( dev == null || dev.trim().equals("false") ) ? false : true;

        String startLauncher = System.getProperty("launcher");

        startLauncher = (startLauncher == null||startLauncher.trim().equals("tomcat")) ? "tomcat":"jetty";

        initEnv(startLauncher);

        launcher.start(devMode,startPort);

    }

    private static void initEnv(String startLauncher) {
        String jarPath = null;
        if (devMode) {
            String artifact =  MavenUtils.get(Thread.currentThread().getContextClassLoader()).getArtifactId();
            if (startLauncher.equals("jetty")) {
                jarPath = "./".concat(artifact).concat("/jetty");
            }else {//tomcat...
                jarPath = "./".concat(artifact).concat("/tomcat");
            }
            System.setProperty("catalina.home","./".concat(artifact));
        }else {
            if (startLauncher.equals("jetty")) {
                jarPath = "./jetty";
            }else {//tomcat
                jarPath = "./tomcat";
            }
            System.setProperty("catalina.home","./");
        }
        //load jars.
        ExtClasspathLoader.scanJar(jarPath);

    }

}


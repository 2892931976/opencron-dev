package org.opencron.server.bootstrap;

import org.opencron.common.util.ExtClasspathLoader;
import org.opencron.common.util.MavenUtils;

import java.io.File;

public class Startup {

    private static final int MIN_PORT = 0;

    private static final int MAX_PORT = 65535;

    private static int startPort = 8080;

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

        String artifact = MavenUtils.get(Thread.currentThread().getContextClassLoader()).getArtifactId();;
        String jarPath = null;
        File warFile = null;

        String dev = System.getProperty("dev");

        //dev model
        if (dev == null || dev.trim().equals("true")) {
            //start jetty...
            jarPath = "./".concat(artifact).concat("/jetty");
            warFile = new File( "./".concat(artifact).concat("/target/").concat(artifact).concat(".war") );
            System.setProperty("catalina.home","./".concat(artifact));
            ExtClasspathLoader.scanJar(jarPath);

            JettyLauncher jettyLauncher = new JettyLauncher();
            jettyLauncher.start(artifact,warFile,false,startPort);
        }else {
            //server.sh脚本启动的...
            String launcher = System.getProperty("server.launcher");

            if (launcher.equals("jetty")) {
                System.setProperty("catalina.home","./");

                jarPath = "./jetty";
                ExtClasspathLoader.scanJar(jarPath);

                JettyLauncher jettyLauncher = new JettyLauncher();
                jettyLauncher.start(artifact,warFile,true,startPort);
            }else if(launcher.equals("tomcat")){
                System.setProperty("catalina.home","./".concat(artifact));

                jarPath = "./tomcat";
                ExtClasspathLoader.scanJar(jarPath);

                TomcatLauncher tomcatLauncher = new TomcatLauncher();
                tomcatLauncher.startup(artifact,startPort);
            }

        }
    }

}


package org.opencron.server.bootstrap;

import org.opencron.common.util.ExtClasspathLoader;
import org.opencron.common.util.MavenUtils;

import java.io.File;

public class Startup {

    public static void main(String[] args) {

        MavenUtils mavenUtils = MavenUtils.get(Thread.currentThread().getContextClassLoader());

        String launcher = System.getProperty("server.launcher");

        String artifact = null;
        String jettyJarPath = null;
        File warFile = null;

        if (launcher == null) {
            artifact = mavenUtils.getArtifactId();
            jettyJarPath = "./".concat(artifact).concat("/jetty");
            warFile = new File( "./".concat(artifact).concat("/target/").concat(artifact).concat(".war") );
            System.setProperty("catalina.home","./".concat(artifact));
        }else if (launcher.equals("jetty")){
            jettyJarPath = "./jetty";
            System.setProperty("catalina.home","./");
        }
        ExtClasspathLoader.scanJar(jettyJarPath);
        JettyLauncher jettyLauncher = new JettyLauncher();
        jettyLauncher.start(artifact,warFile,launcher!=null,args);

    }

}


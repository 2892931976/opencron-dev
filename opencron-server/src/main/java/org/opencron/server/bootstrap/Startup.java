package org.opencron.server.bootstrap;

import org.opencron.common.util.ExtClasspathLoader;
import org.opencron.common.util.MavenUtils;
import java.io.File;

public class Startup {

    public static void main(String[] args) {

        MavenUtils mavenUtils = MavenUtils.get(Startup.class.getClassLoader());

        String artifactName = mavenUtils.getArtifactId();

        System.setProperty("catalina.home", "./".concat(artifactName));

        //add jetty jar...
        String jettyJarPath = "./"+artifactName+"/jetty-lib";

        ExtClasspathLoader.scanJar(jettyJarPath);

        File warFile = new File("./".concat(artifactName).concat("/target/").concat(artifactName.concat(".war")));

        JettyLauncher jettyLauncher = new JettyLauncher();
        jettyLauncher.start(artifactName,warFile,args);

    }

}


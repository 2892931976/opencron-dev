package org.opencron.server.bootstrap;


import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class TomcatLauncher {

    public void startup(String artifct,int startPort) throws Exception {

        //get webapp path...
        String webappPath = "./".concat(artifct).concat("/src/main/webapp/");
        File webApp = new File(webappPath);

        //appBase
        String appBase = System.getProperty("user.dir") + File.separator + ".";

        Tomcat tomcat = new Tomcat();

        tomcat.setHostname("localhost");
        tomcat.setPort(startPort);
        tomcat.setBaseDir(".");

        StandardServer server = (StandardServer) tomcat.getServer();
        AprLifecycleListener listener = new AprLifecycleListener();
        server.addLifecycleListener(listener);
        tomcat.getHost().setAppBase(appBase);
        tomcat.addWebapp("", webApp.getAbsolutePath());

        tomcat.start();
        tomcat.getServer().await();
    }

}

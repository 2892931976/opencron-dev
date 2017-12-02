package org.opencron.server.bootstrap;


import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.startup.Tomcat;
import org.opencron.common.Constants;
import org.opencron.common.util.MavenUtils;

import java.io.File;

public class TomcatLauncher implements Launcher {

    private static final String currentPath = "";

    @Override
    public void start(boolean devMode,int port) throws Exception {

        //get webapp path...
        File webApp = null;
        String baseDir = null;
        if (devMode) {
            String artifact = MavenUtils.get(Thread.currentThread().getContextClassLoader()).getArtifactId();
            baseDir = artifact;
            webApp = new File(baseDir+"/src/main/webapp/");
        }else {
            baseDir = currentPath;
            webApp = new File(currentPath);
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

        tomcat.start();
        tomcat.getServer().await();
    }

    @Override
    public void stop() {

    }
}

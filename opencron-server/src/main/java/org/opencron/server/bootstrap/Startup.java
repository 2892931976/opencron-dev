package org.opencron.server.bootstrap;

import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jsp.JettyJspServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;
import org.opencron.common.util.CommonUtils;
import org.opencron.common.util.ExtClasspathLoader;
import org.opencron.common.util.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Startup {

    private static final String warName = "opencron-server.war";

    private static final String artifactName = "opencron-server";

    private static int startPort = 8080;

    public static void main(String[] args) {

        System.setProperty("catalina.home","./".concat(artifactName));

        Logger logger = LoggerFactory.getLogger(Startup.class);

        File warFile = new File("./".concat(artifactName).concat("/target/").concat(warName));
        if (!warFile.exists()) {
            throw new IllegalArgumentException("[opencron] start server error,please build project with maven first!");
        }

        //add jetty jar...
        ExtClasspathLoader.loadJarByPath("/Users/benjobs/GitHub/opencron-dev/opencron-server/lib");

        if ( CommonUtils.notEmpty(args) ) {
            Integer port = CommonUtils.toInt(args[0]);
            if (port == null || NetUtils.isInvalidPort(port)) {
                throw new IllegalArgumentException("[opencron] server port error: " + port );
            }
            startPort = port;
            logger.info("[opencron]Server At port {} Starting...",startPort);
        }else {
            logger.info("[opencron]Server At default port {} Starting...",startPort);
        }

        Server server = new Server(startPort);

        WebAppContext appContext = new WebAppContext();

        appContext.setWar(warFile.getAbsolutePath());

        //init param
        appContext.setThrowUnavailableOnStartupException(true);	// 在启动过程中允许抛出异常终止启动并退出 JVM
        appContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        appContext.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");

        //for jsp support
        appContext.addBean(new JspStarter(appContext));
        appContext.addServlet(JettyJspServlet.class, "*.jsp");

        appContext.setContextPath("/");
        appContext.setParentLoaderPriority(true);
        appContext.setClassLoader(Thread.currentThread().getContextClassLoader());

        server.setStopAtShutdown(true);
        server.setHandler(appContext);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class JspStarter extends AbstractLifeCycle implements ServletContextHandler.ServletContainerInitializerCaller {

        JettyJasperInitializer jasperInitializer;
        ServletContextHandler context;

        public JspStarter(ServletContextHandler context) {
            this.jasperInitializer = new JettyJasperInitializer();
            this.context = context;
            this.context.setAttribute("org.apache.tomcat.JarScanner", new StandardJarScanner());
        }

        @Override
        protected void doStart() throws Exception {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(context.getClassLoader());
            try {
                jasperInitializer.onStartup(null, context.getServletContext());
                super.doStart();
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }

}

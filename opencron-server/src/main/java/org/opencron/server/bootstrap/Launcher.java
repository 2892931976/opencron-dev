package org.opencron.server.bootstrap;

public interface Launcher {

    void start(boolean devMode,int port);

    void stop();

}

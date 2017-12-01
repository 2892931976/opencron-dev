package org.opencron.server.bootstrap;


import org.opencron.common.ext.SPI;

@SPI
public interface Launcher {

    void start(boolean devMode,int port) throws Exception;

    void stop();

}

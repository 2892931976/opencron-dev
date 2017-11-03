package org.opencron.rpc;


import org.opencron.common.ext.SPI;

@SPI
public interface Server {

    void start(int prot, ServerHandler handler);

    void stop() throws Throwable;

}

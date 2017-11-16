package org.opencron.rpc;


import org.opencron.common.ext.SPI;

@SPI("mina")
public interface Server {

    void start(int port, ServerHandler handler);

    void destroy() throws Throwable;

}

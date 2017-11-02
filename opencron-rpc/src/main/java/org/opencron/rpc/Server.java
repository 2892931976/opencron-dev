package org.opencron.rpc;


import org.opencron.common.extension.SPI;

@SPI
public interface Server {

    boolean isBound();

    void open(int prot, ServerHandler handler);

    void close() throws Throwable;

}

package org.opencron.rpc;


import org.opencron.common.extension.SPI;

@SPI("netty")
public interface Server {

    boolean isBound();

    void open(int prot, RpcHandler handler);

    void close() throws Throwable;

}

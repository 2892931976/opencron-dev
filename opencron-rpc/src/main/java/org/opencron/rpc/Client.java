package org.opencron.rpc;

import org.opencron.common.extension.SPI;

@SPI("netty")
public interface Client extends RpcInvoker {

    void open();

    void close() throws Throwable;

}

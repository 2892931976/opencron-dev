package org.opencron.rpc;

import org.opencron.common.ext.SPI;

@SPI
public interface Client extends RpcInvoker {

    void connect();

    void disconnect() throws Throwable;

}

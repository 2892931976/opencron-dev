package org.opencron.rpc;

import org.opencron.common.ext.SPI;

@SPI
public interface Client extends ClientInvoker {

    void open();

    void close() throws Throwable;

}

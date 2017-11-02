package org.opencron.rpc;

import org.opencron.common.extension.SPI;

@SPI
public interface Client extends ClientInvoker {

    void open();

    void close() throws Throwable;

}

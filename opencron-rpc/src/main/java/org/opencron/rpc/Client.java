package org.opencron.rpc;

public interface Client {

    void open();

    void close() throws Throwable;

}

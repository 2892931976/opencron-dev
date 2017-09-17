package org.opencron.transport.netty.core;

/**
 * Created by Ricky on 2017/5/9.
 */
public interface InvokeCallback {

    void onSuccess(Object result);

    void onFailure(Throwable err);
}
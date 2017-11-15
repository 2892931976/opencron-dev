package org.opencron.rpc.mina;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.rpc.Promise;
import org.slf4j.Logger;

public class MinaClientHandler extends IoHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MinaClientHandler.class);

    private Promise.Getter promiseGetter;

    public MinaClientHandler(Promise.Getter promiseGetter) {
        this.promiseGetter = promiseGetter;
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        Response response = (Response) message;
        logger.info("Rpc client receive response id:{}", response.getId());
        Promise promise = promiseGetter.getPromise(response.getId());
        promise.setResult(response);
        if (promise.isAsync()) {   //异步调用
            logger.info("Rpc client async callback invoke");
            promise.execCallback();
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        super.exceptionCaught(session, cause);
    }

}


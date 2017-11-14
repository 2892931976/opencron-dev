package org.opencron.rpc.mina;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.rpc.RpcFuture;
import org.slf4j.Logger;

public class MinaClientHandler extends IoHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MinaClientHandler.class);

    private RpcFuture.Getter rpcFutureGetter;

    public MinaClientHandler(RpcFuture.Getter rpcFutureGetter) {
        this.rpcFutureGetter = rpcFutureGetter;
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        Response response = (Response) message;
        logger.info("Rpc client receive response id:{}", response.getId());
        RpcFuture rpcFuture = rpcFutureGetter.getRpcFuture(response.getId());
        rpcFuture.setResult(response);
        if (rpcFuture.isAsync()) {   //异步调用
            logger.info("Rpc client async callback invoke");
            rpcFuture.execCallback();
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        super.exceptionCaught(session, cause);
    }

}


package org.opencron.rpc.mina;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.rpc.RpcFuture;
import org.opencron.rpc.netty.NettyClient;
import org.slf4j.Logger;

public class MinaClientHandler extends IoHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MinaClientHandler.class);

    private MinaClient minaClient;

    public MinaClientHandler(MinaClient minaClient) {
        this.minaClient = minaClient;
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        Response response = (Response) message;
        logger.info("Rpc client receive response id:{}", response.getId());
        RpcFuture future = minaClient.futureTable.get(response.getId());
        future.setResult(response);
        if (future.isAsync()) {   //异步调用
            logger.info("Rpc client async callback invoke");
            future.execCallback();
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        super.exceptionCaught(session, cause);
    }

}


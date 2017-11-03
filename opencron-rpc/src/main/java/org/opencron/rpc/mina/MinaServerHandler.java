package org.opencron.rpc.mina;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.rpc.ServerHandler;
import org.slf4j.Logger;

public class MinaServerHandler extends IoHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(MinaServerHandler.class);

    private ServerHandler handler;

    public MinaServerHandler(ServerHandler handler){
        this.handler = handler;
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        Request request = (Request) message;
        Response response = handler.handle(request);
        session.write(response);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
        session.close(true);
    }
}
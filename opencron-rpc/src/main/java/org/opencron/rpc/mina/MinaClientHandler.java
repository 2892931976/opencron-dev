package org.opencron.rpc.mina;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.slf4j.Logger;

public class MinaClientHandler extends IoHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MinaClientHandler.class);

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        Response response = (Response) message;
        if (response!=null) {
            System.out.println(response.getMessage());
        }

    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        super.exceptionCaught(session, cause);
    }

}


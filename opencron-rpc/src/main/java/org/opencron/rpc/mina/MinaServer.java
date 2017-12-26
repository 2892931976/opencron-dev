package org.opencron.rpc.mina;


import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.rpc.Server;
import org.opencron.rpc.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.opencron.common.util.ExceptionUtils.stackTrace;

public class MinaServer implements Server {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private NioSocketAcceptor acceptor;

    private InetSocketAddress socketAddress;

    @Override
    public void start(final int port, ServerHandler handler) {

        final MinaServerHandler serverHandler = new MinaServerHandler(handler);
        this.socketAddress = new InetSocketAddress(port);

        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("threadPool", new ExecutorFilter(Executors.newCachedThreadPool()));
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(Response.class, Request.class)));
        acceptor.setHandler(serverHandler);
        try {
            acceptor.bind(this.socketAddress);
            logger.info("[opencron] MinaServer start at address:{} success", port);
        } catch (IOException e) {
            logger.error("[opencron] MinaServer start failure: {}", stackTrace(e));
        }
    }

    @Override
    public void destroy() throws Throwable {
        try {
            if (acceptor != null) {
                acceptor.dispose();
            }
            logger.info("[opencron] MinaServer stoped!");
        } catch (Throwable e) {
            logger.error("[opencron] MinaServer stop error:{}", stackTrace(e));
        }
    }

}

package org.opencron.rpc.mina;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.HttpUtils;
import org.opencron.rpc.Client;
import org.opencron.rpc.ClientAsyncCallback;
import org.slf4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


public class MinaClient implements Client {

    private static Logger logger = LoggerFactory.getLogger(MinaClient.class);

    private NioSocketConnector connector;

    @Override
    public void connect() {

        connector = new NioSocketConnector();
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(Request.class,Response.class)));
        connector.setHandler(new MinaClientHandler());
        connector.setConnectTimeoutMillis(5000);

        DefaultSocketSessionConfig sessionConfiguration = (DefaultSocketSessionConfig) connector.getSessionConfig();
        sessionConfiguration.setReadBufferSize(1024);
        sessionConfiguration.setSendBufferSize(512);
        sessionConfiguration.setReuseAddress(true);
        sessionConfiguration.setTcpNoDelay(true);
        sessionConfiguration.setKeepAlive(true);
        sessionConfiguration.setSoLinger(-1);
        sessionConfiguration.setWriteTimeout(5);
    }

    @Override
    public void disconnect() throws Throwable {
        if (this.connector != null) {
            this.connector.dispose();
            this.connector = null;
        }
    }

    @Override
    public Response sentSync(final Request request) throws Exception {
        ConnectFuture future = connector.connect(HttpUtils.parseSocketAddress(request.getAddress()));
        long start = System.currentTimeMillis();
        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
        final CountDownLatch finish = new CountDownLatch(1); // resolve future.awaitUninterruptibly() dead lock
        future.addListener(new IoFutureListener() {
            public void operationComplete(IoFuture future) {
                try {
                    if (future.isDone()) {
                        IoSession newSession = future.getSession();
                        newSession.write(request);
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    finish.countDown();
                }
            }
        });
        return null;
    }

    @Override
    public void sentOneway(Request request) throws Exception {

    }

    @Override
    public void sentAsync(Request request, ClientAsyncCallback callback) throws Exception {

    }

}

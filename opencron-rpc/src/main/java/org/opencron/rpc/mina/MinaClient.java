package org.opencron.rpc.mina;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.HttpUtils;
import org.opencron.rpc.Client;
import org.opencron.rpc.InvokeCallback;
import org.opencron.rpc.Promise;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MinaClient implements Client {

    private static Logger logger = LoggerFactory.getLogger(MinaClient.class);

    private NioSocketConnector connector;

    protected final ConcurrentHashMap<Integer, Promise> promiseTable =  new ConcurrentHashMap<Integer, Promise>(256);

    private final ConcurrentHashMap<String, ConnectWrapper> connectTable = new ConcurrentHashMap<String, ConnectWrapper>();

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public MinaClient() {
        this.connect();
    }

    @Override
    public void connect() {

        connector = new NioSocketConnector();
        connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MinaCodecAdapter(Request.class,Response.class)));
        connector.setHandler(new MinaClientHandler(new Promise.Getter() {
            @Override
            public Promise getPromise(Integer id) {
                return promiseTable.get(id);
            }
        }));

        connector.setConnectTimeoutMillis(5000);

        DefaultSocketSessionConfig sessionConfiguration = (DefaultSocketSessionConfig) connector.getSessionConfig();
        sessionConfiguration.setTcpNoDelay(true);
        sessionConfiguration.setKeepAlive(true);
        sessionConfiguration.setWriteTimeout(5);

        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5, new ThreadFactory() {
            private final AtomicInteger idGenerator = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "minaRpc "+ this.idGenerator.incrementAndGet());
            }
        });

        this.scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                scanPromiseTable();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
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
        final ConnectFuture connect = getOrCreateConnect(request);
        if (connect != null && connect.isConnected()) {
            final Promise promise = new Promise(request.getTimeOut());
            this.promiseTable.put(request.getId(), promise);
            //写数据
            connect.addListener(new IoFutureListener() {
                @Override
                public void operationComplete(IoFuture future) {
                    if (future.isDone()) {
                        logger.info("send success, request id:{}", request.getId());
                        promise.setSendRequestSuccess(true);
                        return;
                    } else {
                        logger.info("send failure, request id:{}", request.getId());
                        promiseTable.remove(request.getId());
                        promise.setSendRequestSuccess(false);
                        promise.setFailure(connect.getException());
                    }
                }
            });
            connect.getSession().write(request);
            return promise.get();
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }

    @Override
    public void sentOneway(final Request request) throws Exception {
        ConnectFuture connect = getOrCreateConnect(request);
        if (connect != null && connect.isConnected()) {
            connect.addListener(new IoFutureListener() {
                @Override
                public void operationComplete(IoFuture future) {
                    if (future.isDone()) {
                        logger.info("send success, request id:{}", request.getId());
                    } else {
                        logger.info("send failure, request id:{}", request.getId(), future);
                    }
                }
            });
            connect.getSession().write(request);
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }

    @Override
    public void sentAsync(final Request request,final InvokeCallback callback) throws Exception {
        final ConnectFuture connect = getOrCreateConnect(request);
        if (connect != null && connect.isConnected()) {
            final Promise promise = new Promise(request.getTimeOut(),callback);
            this.promiseTable.put(request.getId(), promise);
            //写数据
            connect.addListener(new IoFutureListener<IoFuture>() {
                @Override
                public void operationComplete(IoFuture future) {
                    if (future.isDone()) {
                        logger.info("send success, request id:{}", request.getId());
                        promise.setSendRequestSuccess(true);
                        return;
                    } else {
                        logger.info("send failure, request id:{}", request.getId());
                        promiseTable.remove(request.getId());
                        promise.setSendRequestSuccess(false);
                        promise.setFailure(connect.getException());
                        //回调
                        callback.failure(connect.getException());
                    }
                }
            });
            connect.getSession().write(request);
        } else {
            throw new IllegalArgumentException("channel not active. request id:"+request.getId());
        }
    }


    private ConnectFuture getOrCreateConnect(Request request) {

        ConnectWrapper connectWrapper = this.connectTable.get(request.getAddress());

        if (connectWrapper!=null) {
            if(connectWrapper.isActive()){
                return connectWrapper.getConnectFuture();
            }
            connectWrapper.close();
        }

        synchronized (this){
            // 发起异步连接操作
            ConnectFuture connectFuture = connector.connect(HttpUtils.parseSocketAddress(request.getAddress()));
            connectWrapper = new ConnectWrapper(connectFuture);
            this.connectTable.put(request.getAddress(), connectWrapper);
        }

        if (connectWrapper != null) {
            ConnectFuture connectFuture = connectWrapper.getConnectFuture();
            long timeout = 5000;
            if (connectFuture.awaitUninterruptibly(timeout)) {
                if (connectWrapper.isActive()) {
                    logger.info("getOrCreateConnect: connect remote host[{}] success, {}", request.getAddress(), connectFuture.toString());
                    return connectWrapper.getConnectFuture();
                } else {
                    logger.warn("getOrCreateConnect: connect remote host[" + request.getAddress() + "] failed, " + connectFuture.toString(), connectFuture.getException());
                }
            } else {
                logger.warn("getOrCreateConnect: connect remote host[{}] timeout {}ms, {}", request.getAddress(), timeout, connectFuture);
            }
        }
        return null;
    }

    /**定时清理超时Future**/
    private void scanPromiseTable() {
        Iterator<Map.Entry<Integer, Promise>> it = this.promiseTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Promise> next = it.next();
            Promise rep = next.getValue();

            if ((rep.getBeginTimestamp() + rep.getTimeoutMillis() + 1000) <= System.currentTimeMillis()) {  //超时
                it.remove();
            }
        }
    }
}

package org.opencron.rpc.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.rpc.Promise;
import org.slf4j.Logger;


@ChannelHandler.Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<Response> {

    private Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    private Promise.Getter promiseGetter;

    public NettyClientHandler(Promise.Getter promiseGetter) {
        this.promiseGetter = promiseGetter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
        logger.info("[opencron] nettyRpc client receive response id:{}", response.getId());
        Promise promise = promiseGetter.getPromise(response.getId());
        promise.setResult(response);
        if (promise.isAsync()) {   //异步调用
            logger.info("[opencron] nettyRpc client async callback invoke");
            promise.execCallback();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.error("捕获异常", cause);
    }
}

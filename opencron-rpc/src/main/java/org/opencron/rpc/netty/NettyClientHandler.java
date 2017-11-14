package org.opencron.rpc.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.rpc.RpcFuture;
import org.slf4j.Logger;


@ChannelHandler.Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<Response> {

    private Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    private RpcFuture.Getter rpcFutureGetter;

    public NettyClientHandler(RpcFuture.Getter rpcFutureGetter) {
        this.rpcFutureGetter = rpcFutureGetter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
        logger.info("Rpc client receive response id:{}", response.getId());
        RpcFuture rpcFuture = rpcFutureGetter.getRpcFuture(response.getId());
        rpcFuture.setResult(response);
        if (rpcFuture.isAsync()) {   //异步调用
            logger.info("Rpc client async callback invoke");
            rpcFuture.execCallback();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.error("捕获异常", cause);
    }
}

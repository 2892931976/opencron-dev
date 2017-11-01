package org.opencron.rpc.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.opencron.common.job.Response;
import org.opencron.common.logging.LoggerFactory;
import org.slf4j.Logger;


@ChannelHandler.Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<Response> {

    private Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);

    private NettyClient nettyClient;

    public NettyClientHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
        logger.info("Rpc client receive response id:{}", response.getId());
        NettyFuture future = nettyClient.futureTable.get(response.getId());
        future.setResult(response);
        if (future.isAsync()) {   //异步调用
            logger.info("Rpc client async callback invoke");
            future.execCallback();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.error("捕获异常", cause);
    }
}

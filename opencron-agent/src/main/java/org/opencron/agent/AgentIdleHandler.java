package org.opencron.agent;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.opencron.common.utils.LoggerFactory;
import org.slf4j.Logger;

public class AgentIdleHandler extends ChannelInboundHandlerAdapter {

    private String password;

    private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public AgentIdleHandler(String password) {
        this.password = password;
    }

    @Override
    public void channelRead(ChannelHandlerContext handlerContext, Object msg) throws Exception {
        //request password。...
        String reqpwd = msg.toString();

        if (!this.password.equalsIgnoreCase(reqpwd)) {
            logger.error("[opencron] heartbeat password error!,with server {}",handlerContext.channel().remoteAddress());
        }

        //write result.
        handlerContext.writeAndFlush(Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer(this.password.equalsIgnoreCase(reqpwd) ? "1" : "0",
                        CharsetUtil.UTF_8)));

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext handlerContext, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (this == handlerContext.pipeline().last()) {
            logger.info("[opencron] agent {} shutdowd...",handlerContext.channel().id());
        }
        handlerContext.channel().close();
        handlerContext.close();
    }

    @ChannelHandler.Sharable
    public static class AcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(ChannelHandlerContext handlerContext, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleState state = ((IdleStateEvent) evt).state();
                if (state == IdleState.READER_IDLE) {
                    logger.warn("[opencron] heartbeat READER_IDLE,with server {}",handlerContext.channel().remoteAddress());
                }
            } else {
                super.userEventTriggered(handlerContext, evt);
            }
        }
    }

}
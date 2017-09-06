package org.opencron.agent;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.utils.LoggerFactory;
import org.slf4j.Logger;

public class AgentIdleHandler extends ChannelInboundHandlerAdapter {

    private String password;

    private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public AgentIdleHandler(String password){
        this.password = password;
    }

    @Override
    public void channelRead(ChannelHandlerContext handlerContext, Object msg) throws Exception {

        Response response;

        Request request = JSON.parseObject(msg.toString(),Request.class);

        if (this.password.equalsIgnoreCase(request.getPassword())) {
            response = Response.response(request).setSuccess(true).end();
        }else {
            handlerContext.close();
            response = Response.response(request).setSuccess(false).end();
            logger.error("[opencron] heartbeat password error!,with server {}",handlerContext.channel().remoteAddress());
        }

        ByteBuf heartbeatResponse = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(JSON.toJSONString(response), CharsetUtil.UTF_8));

        handlerContext.writeAndFlush(heartbeatResponse.duplicate());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext handlerContext, Throwable cause) throws Exception {
        cause.printStackTrace();
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
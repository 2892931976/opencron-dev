package org.opencron.rpc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.opencron.common.Constants;
import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.opencron.common.util.ExceptionUtils.stackTrace;

public class NettyCodecAdapter<T> {

    private static Logger logger = LoggerFactory.getLogger(NettyCodecAdapter.class);

    private static Serializer serializer = ExtensionLoader.load(Serializer.class);

    public static NettyCodecAdapter getCodecAdapter() {
        return new NettyCodecAdapter();
    }

    public Encoder getEncoder(Class<T> type) throws Exception {
        return new Encoder(type);
    }

    public Decoder getDecoder(Class<T> type) throws IOException {
        return new Decoder(type);
    }

    private class Encoder<T> extends MessageToByteEncoder {

        private Class<?> type = null;

        public Encoder(Class<T> type) {
            this.type = type;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            try {
                if (type.isInstance(msg)) {
                    byte[] data = serializer.encode(msg);
                    out.writeInt(data.length);
                    out.writeBytes(data);
                }else {
                    logger.error("[opencron] NettyCodecAdapter encode error: this encode target is not instanceOf {}",this.type.getName());
                }
            } catch (Exception e) {
                logger.error("[opencron] NettyCodecAdapter encode error:", stackTrace(e));
            }

        }
    }

    private class Decoder<T> extends ByteToMessageDecoder {

        private Class<T> type;

        public Decoder(Class<T> type) {
            this.type = type;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            try {
                if (in.readableBytes() < Constants.HEADER_SIZE) {
                    return;
                }
                in.markReaderIndex();
                int dataLength = in.readInt();
                if (in.readableBytes() < dataLength) {
                    in.resetReaderIndex();
                    return;
                }
                byte[] data = new byte[dataLength];
                in.readBytes(data);
                Object object = serializer.decode(data, type);
                out.add(object);
            }catch (Exception e) {
                logger.error("[opencron] NettyCodecAdapter decode error:", stackTrace(e));
            }
        }
    }

}

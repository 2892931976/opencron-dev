package org.opencron.rpc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.opencron.common.Constants;
import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NettyCodecAdapter<T> {

    private static Logger logger = LoggerFactory.getLogger(NettyCodecAdapter.class);

    private static Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension();

    public static NettyCodecAdapter getCodecAdapter(){
        return new NettyCodecAdapter();
    }

    public Encoder getEncoder(Class<T> type) throws Exception {
        return new Encoder(type);
    }

    public Decoder getDecoder(Class<T> type, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) throws IOException {
        return new Decoder(type,maxFrameLength,lengthFieldOffset,lengthFieldLength);
    }

    private class Encoder<T> extends MessageToByteEncoder {

        private Class<T> type;

        public Encoder(Class<T> type) {
            this.type = type;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            try {
                byte[] data = serializer.encode(msg);
                out.writeInt(data.length);
                out.writeBytes(data);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private class Decoder<T> extends LengthFieldBasedFrameDecoder {

        private Class<T> type;

        public Decoder(Class<T> type, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) throws IOException {
            super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
            this.type = type;
        }

        @Override
        public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
            if (in.readableBytes() < Constants.HEADER_SIZE) {
                return null;
            }
            in.markReaderIndex();
            int dataLength = in.readInt();
            if (in.readableBytes() < dataLength) {
                logger.error("[opencron]serializer error!body length < {}", dataLength);
                in.resetReaderIndex();
                return null;
            }

            byte[] data = new byte[dataLength];
            in.readBytes(data);

            try {
                return serializer.decode(data, type);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("[opencron]serializer decode error");
            }
        }

    }

}

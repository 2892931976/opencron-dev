package org.opencron.rpc.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;
import org.opencron.common.Constants;
import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MinaCodecAdapter implements ProtocolCodecFactory {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension();

    private Class<?> encodeClass;

    private Class<?> decodeClass;

    public MinaCodecAdapter(Class<?> encodeClass,Class<?> decodeClass){
        this.encodeClass = encodeClass;
        this.decodeClass = decodeClass;
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception {
        return new MinaEncoder(this.encodeClass);
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception {
        return new MinaDecoder(this.decodeClass);
    }

    final class MinaDecoder<T> extends CumulativeProtocolDecoder {

        private Class<T> type;

        public MinaDecoder(Class<T> type) {
            this.type = type;
        }

        @Override
        public boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            if (in.limit()<= 0 || in.remaining() < Constants.HEADER_SIZE) {
                return false;
            }
            in.mark();
            int dataLength = in.getInt();

            if (in.remaining() < dataLength) {
                //logger.warn("[opencron]serializer error!body length < {}", dataLength);
                in.reset();
                return false;
            }
            byte[] data = new byte[dataLength];
            in.get(data);
            Object obj = serializer.decode(data,type);
            out.write(obj);
            return true;
        }
    }

    final class MinaEncoder<T> implements ProtocolEncoder {

        private Class<T> type;

        public MinaEncoder(Class<T> type) {
            this.type = type;
        }

        @Override
        public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) throws Exception {
            if (type.isInstance(msg)) {
                byte[] data = serializer.encode(msg);
                IoBuffer buffer = IoBuffer.allocate(100);
                buffer.setAutoExpand(true);
                buffer.setAutoShrink(true);
                buffer.putInt(data.length);
                buffer.put(data);
                buffer.flip();
                session.write(buffer);
            }
        }

        @Override
        public void dispose(IoSession session) throws Exception {

        }

    }



}

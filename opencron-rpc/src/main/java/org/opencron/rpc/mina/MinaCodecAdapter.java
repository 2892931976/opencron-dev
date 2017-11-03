package org.opencron.rpc.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;
import org.opencron.common.Constants;
import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.job.Request;
import org.opencron.common.job.Response;
import org.opencron.common.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MinaCodecAdapter implements ProtocolCodecFactory {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension();

    @Override
    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception {
        return new MinaEncoder(Request.class);
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception {
        return new MinaDecoder(Response.class);
    }

    class MinaDecoder extends CumulativeProtocolDecoder {

        private Class<?> type;

        public MinaDecoder(Class<?> type) {
            this.type = type;
        }

        @Override
        protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            if (in.remaining() < Constants.HEADER_SIZE) {
                return false;
            }
            in.mark();	// mark 2 reset, reset call rollback to mark place
            int dataLength = in.getInt();	// data length

            if (in.remaining() < dataLength) {
                logger.error("[opencron]serializer error!body length < {}", dataLength);
                in.reset();
                return false;
            }

            byte[] datas = new byte[dataLength];	// data
            in.get(datas);
            Object obj = serializer.decode(datas,type);
            out.write(obj);
            return true;
        }

    }

    class MinaEncoder implements ProtocolEncoder {

        private Class<?> type;

        public MinaEncoder(Class<?> type) {
            this.type = type;
        }

        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            if (type.isInstance(message)) {
                byte[] datas = serializer.encode(message);
                IoBuffer buffer = IoBuffer.allocate(100);
                buffer.setAutoExpand(true);
                buffer.setAutoShrink(true);
                buffer.putInt(datas.length);
                buffer.put(datas);

                buffer.flip();
                session.write(buffer);
            }
        }

        @Override
        public void dispose(IoSession session) throws Exception {

        }

    }



}

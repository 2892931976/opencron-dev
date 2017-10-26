/**
 * Copyright (c) 2015 The Opencron Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.opencron.common.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author benjobs
 */
public class Decoder<T> extends LengthFieldBasedFrameDecoder {

    private Logger logger = LoggerFactory.getLogger(getClass());

    //判断传送客户端传送过来的数据是否按照协议传输，头部信息的大小应该是 byte+byte+int = 1+1+4 = 6
    private static final int HEADER_SIZE = 6;

    private Serializer serializer = SerializerFactory.getSerializer();
    private Class<T> clazz;

    public Decoder(Class<T> clazz, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) throws IOException {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
        this.clazz = clazz;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (in.readableBytes() < HEADER_SIZE) {
            return null;
        }
        in.markReaderIndex();

        //注意在读的过程中，readIndex的指针也在移动
        byte type = in.readByte();
        byte flag = in.readByte();
        //logger.info("read type:{}, flag:{}, length:{}", type, flag, dataLength);

        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            logger.error("[opencron]serializer error!body length < {}", dataLength);
            in.resetReaderIndex();
            return null;
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);

        try{
            return serializer.decode(data, clazz);
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("[opencron]serializer decode error");
        }
    }
}

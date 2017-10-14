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
package org.opencron.common.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.opencron.common.serialization.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author benjobs
 */
public class KryoSerializer implements Serializer {

    private static final ThreadLocal<Kryo> THREAD_LOCAL = new ThreadLocal<Kryo>(){
        @Override
        protected Kryo initialValue() {

            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            return kryo;
        }
    };

    @Override
    public byte[] encode(Object msg) throws IOException {
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Output output = new Output(bos)){

            Kryo kryo = THREAD_LOCAL.get();
            kryo.writeObject(output, msg);
            return output.toBytes();
        }
    }

    @Override
    public <T> T decode(byte[] buf, Class<T> type) throws IOException {

        try (ByteArrayInputStream bis = new ByteArrayInputStream(buf);
             Input input = new Input(bis)) {

            Kryo kryo = THREAD_LOCAL.get();
            return kryo.readObject(input, type);
        }
    }
}

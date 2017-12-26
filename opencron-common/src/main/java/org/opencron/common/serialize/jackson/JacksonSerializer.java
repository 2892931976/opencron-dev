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
package org.opencron.common.serialize.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencron.common.serialize.Serializer;

import java.io.IOException;

/**
 * @author benjobs
 */

public class JacksonSerializer implements Serializer {

    private static final String CHARSET = "UTF-8";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] encode(Object msg) throws IOException {
        String jsonString = objectMapper.writeValueAsString(msg);
        return jsonString.getBytes(CHARSET);
    }

    @Override
    public <T> T decode(byte[] buf, Class<T> type) throws IOException {
        String jsonString = new String(buf, CHARSET);
        return objectMapper.readValue(jsonString, type);
    }
}
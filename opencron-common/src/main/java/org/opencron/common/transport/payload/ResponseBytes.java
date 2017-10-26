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
package org.opencron.common.transport.payload;

/**
 * 响应的消息体bytes载体, 避免在IO线程中序列化/反序列化, jupiter-transport这一层不关注消息体的对象结构.
 */
public class ResponseBytes extends BytesHolder {

    // 用于映射 <id, request, response> 三元组
    private final long id; // request.invokeId
    private byte status;

    public ResponseBytes(long id) {
        this.id = id;
    }

    public long id() {
        return id;
    }

    public byte status() {
        return status;
    }

    public void status(byte status) {
        this.status = status;
    }
}

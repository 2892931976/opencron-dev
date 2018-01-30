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

package org.opencron.common.util;

import java.util.*;

public class ConsistentHash<T> {

    private final MurmurHash murmurHash;
    private final int numberOfReplicas;
    private final SortedMap<Long, T> circle = new TreeMap<Long, T>();

    public ConsistentHash(int numberOfReplicas, Collection<T> nodes) {
        this.murmurHash = new MurmurHash();
        this.numberOfReplicas = numberOfReplicas;
        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(murmurHash.hash64(node.toString() + i), node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(murmurHash.hash64(node.toString() + i));
        }
    }

    /**
     * 获得一个最近的顺时针节点
     * @param key 为给定键取Hash，取得顺时针方向上最近的一个虚拟节点对应的实际节点
     * @return
     */
    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = murmurHash.hash64((String) key);
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    public long getSize() {
        return circle.size();
    }

    public static void main(String[] args) {

        Set<String> nodes = new HashSet();
        nodes.add("A");
        nodes.add("B");
        nodes.add("C");

        ConsistentHash<String> consistentHash = new ConsistentHash<String>(123, nodes);

        System.out.println(consistentHash.get("1")); //B
        System.out.println(consistentHash.get("2")); //B
        System.out.println(consistentHash.get("3")); //B
        System.out.println(consistentHash.get("4")); //B
        System.out.println(consistentHash.get("5")); //A
        System.out.println(consistentHash.get("6")); //A
        System.out.println(consistentHash.get("7")); //A
        System.out.println(consistentHash.get("8")); //B
        System.out.println(consistentHash.get("9")); //C
        System.out.println(consistentHash.get("10"));//C
        System.out.println(consistentHash.get("11"));//B
        System.out.println(consistentHash.get("12"));//B
        System.out.println(consistentHash.get("13"));//B
        System.out.println(consistentHash.get("14"));//B
        System.out.println(consistentHash.get("15"));//B
    }


}


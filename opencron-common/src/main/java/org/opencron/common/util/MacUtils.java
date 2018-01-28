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

import org.opencron.common.exception.UnknownException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class MacUtils {

    public static String getMac() {
        try {
            byte[] mac = new byte[0];
            mac = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();
            StringBuffer buffer = new StringBuffer("");
            for (int i = 0; i < mac.length; i++) {
                if (i > 0) {
                    buffer.append(":");
                }
                int temp = mac[i] & 0xff;
                String str = Integer.toHexString(temp);
                if (str.length() == 1) {
                    buffer.append("0" + str);
                } else {
                    buffer.append(str);
                }
            }
            return buffer.toString();
        } catch (Exception e) {
            throw new UnknownException("[opencron] getMacAddress error");
        }
    }

    public static Set<String> getAllMac() {
        List<String> list = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface network = enumeration.nextElement();
                if (network != null) {
                    if (network.getHardwareAddress() != null) {
                        byte[] address = network.getHardwareAddress();

                        StringBuffer buffer = new StringBuffer("");
                        for (int i = 0; i < address.length; i++) {
                            if (i > 0) {
                                buffer.append(":");
                            }
                            String str = Integer.toHexString(address[i] & 0xff);
                            if (str.length() == 1) {
                                buffer.append("0" + str);
                            } else {
                                buffer.append(str);
                            }
                        }
                        list.add(buffer.toString());
                    }
                } else {
                    throw new UnknownException("[opencron] getAllMac error");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        //按照字典顺序排序
        return new TreeSet<String>(list);
    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        System.out.println(MacUtils.getMac());
        System.out.println(MacUtils.getAllMac());
    }
}
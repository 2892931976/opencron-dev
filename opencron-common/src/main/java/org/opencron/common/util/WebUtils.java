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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * Created by benjobs on 2017/6/21.
 */
public class WebUtils implements Serializable{

    public static void writeXml(HttpServletResponse response, String xml) {
        response.setCharacterEncoding("UTF-8");
        setContentLength(response,xml);
        response.setContentType("text/xml");
        write(response, xml);
    }

    public static void writeTxt(HttpServletResponse response, String txt) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain");
        setContentLength(response,txt);
        write(response, txt);
    }

    public static void writeHtml(HttpServletResponse response, String html) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        setContentLength(response,html);
        write(response, html);
    }

    public static void writeJson(HttpServletResponse response, String json) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        setContentLength(response,json);
        write(response, json);
    }

    public static void write404(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(404);
        write(response, "{\"status\":404}");
    }

    private static void write(HttpServletResponse response, String content) {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    private static void setContentLength(HttpServletResponse response,String text){
        try {
            byte[] data = String.valueOf(text).getBytes("UTF-8");
            response.setHeader("Content-Length", "" + data.length);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static String getWebUrlPath(HttpServletRequest request) {
        String port = request.getServerPort() == 80 ? "" : (":"+request.getServerPort());
        String path = request.getContextPath().replaceAll("/$","");
        return request.getScheme()+"://"+request.getServerName()+port+path;
    }

    /**
     * 从web的作用域中获取对象...
     * @param key
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T>T getObject(String key, Object obj, Class<T> clazz) {
        AssertUtils.notNull(key,obj,clazz);
        if (obj instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) obj;
            return (T) request.getAttribute(key);
        }else if(obj instanceof HttpSession){
            HttpSession session = (HttpSession) obj;
            return (T) session.getAttribute(key);
        }else if(obj instanceof ServletContext){
            ServletContext servletContext = (ServletContext) obj;
            return (T) servletContext.getAttribute(key);
        }
        throw new IllegalArgumentException("obj must be {HttpServletRequest|HttpSession|ServletContext} ");
    }

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)){
            ip = request.getRemoteAddr();
        }
        return ip.equals("0:0:0:0:0:0:0:1")?"127.0.0.1":ip;
    }
}

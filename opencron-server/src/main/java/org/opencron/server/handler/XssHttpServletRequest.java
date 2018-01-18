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
package org.opencron.server.handler;


import org.opencron.common.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.opencron.common.util.CommonUtils.isEmpty;

public class XssHttpServletRequest extends HttpServletRequestWrapper {

    public XssHttpServletRequest(HttpServletRequest servletRequest) {
        super(servletRequest);
    }

    @Override
    public String getParameter(String parameter) {
        Object v = super.getParameter(parameter);
        return getValue(v);
    }

    @Override
    public String[] getParameterValues(String parameter) {
        Object v = super.getParameterValues(parameter);
        return getValues(v);
    }

    @Override
    public Map getParameterMap() {
        Map<String, String[]> map = super.getParameterMap();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String[] values = entry.getValue();
            for (int i = 0; i < values.length; i++) {
                values[i] = cleanXSS(values[i]);
            }
            map.put(entry.getKey(), values);
        }
        return map;
    }

    @Override
    public Enumeration getParameterNames() {

        class MyEnumeration implements Enumeration {
            private int count;
            private int length;
            private Object[] data;

            MyEnumeration(Object[] data) {
                this.count = 0;
                this.length = data.length;
                this.data = data;
            }

            @Override
            public boolean hasMoreElements() {
                return (count < length);
            }

            @Override
            public Object nextElement() {
                return data[count++];
            }
        }
        Enumeration enumeration = super.getParameterNames();
        List<Object> list = new ArrayList<Object>();
        while (enumeration.hasMoreElements()) {
            Object v = enumeration.nextElement();
            String value = null;
            if (v == null) {
                continue;
            } else if (v instanceof String[]) {
                String[] strArr = (String[]) v;
                if (isEmpty(strArr)) continue;
                value = strArr[0];
            } else if (v instanceof String) {
                value = (String) v;
            } else {
                value = v.toString();
            }
            if (value == null) {
                continue;
            }
            value = cleanXSS(value);
            list.add(value);
        }
        return new MyEnumeration(list.toArray());
    }

    private String getValue(Object v) {
        String value;
        if (v == null) {
            return null;
        } else if (v instanceof String[]) {
            String[] arrays = (String[]) v;
            if (isEmpty(arrays)) return null;
            value = arrays[0];
        } else if (v instanceof String) {
            value = (String) v;
        } else {
            value = v.toString();
        }
        return cleanXSS(value);
    }

    private String[] getValues(Object obj) {
        String[] values;
        if (obj == null) {
            return null;
        } else if (obj instanceof String[]) {
            values = (String[]) obj;
        } else if (obj instanceof String) {
            values = new String[]{(String) obj};
        } else {
            values = new String[]{obj.toString()};
        }
        if (values == null) {
            return null;
        }
        for (int index = 0; index < values.length; index++) {
            values[index] = cleanXSS(values[index]);
        }
        return values;
    }

    private String cleanXSS(String value) {
        if (value == null) return null;
        return StringUtils.htmlEncode(value);
    }

}

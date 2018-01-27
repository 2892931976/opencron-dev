package org.opencron.server.handler;

import static org.opencron.common.util.CommonUtils.*;

import org.opencron.common.util.StringUtils;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;


public class OpencronServlet extends DispatcherServlet {

    private final String DEF_ENCODING = "UTF-8";

    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setCharacterEncoding(DEF_ENCODING);
        response.setCharacterEncoding(DEF_ENCODING);
        super.doDispatch(new OpencronServletRequest(request), response);
    }

    class OpencronServletRequest extends HttpServletRequestWrapper {

        public OpencronServletRequest(HttpServletRequest servletRequest) {
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
            try {
                value = URLDecoder.decode(value, DEF_ENCODING);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return StringUtils.htmlEncode(value);
        }

    }
}
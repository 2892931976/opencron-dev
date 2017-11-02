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

package org.opencron.common.extension;

import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.CommonUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.opencron.common.util.AssertUtils.checkNotNull;

/**
 *
 * @author benjobs...
 */
public final class ExtensionLoader<T> {

    private Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String META_DIR = "META-INF/opencron/";

    private final Class<T> type;

    private SPI spi;

    private final ClassLoader loader;

    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();

    private LinkedHashMap<String, T> extInstances = new LinkedHashMap<>();

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return ExtensionLoader.getExtensionLoader(type, Thread.currentThread().getContextClassLoader());
    }

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type, ClassLoader loader) {
        return new ExtensionLoader(type, loader);
    }

    public T getExtension() {
        for (Map.Entry<String,T> entry: extInstances.entrySet()) {
            if (entry.getKey().equals( getSpiName(this.spi.value()))) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException(this.type.getName() + " impl could not be found");
    }

    private ExtensionLoader(Class<T> type, ClassLoader loader) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }
        this.type = checkNotNull(type, "type interface cannot be null");
        this.spi = this.type.getAnnotation(SPI.class);
        this.loader = (loader == null) ? ClassLoader.getSystemClassLoader() : loader;
        loadFile();
    }

    private void loadFile() {
        String fileName = META_DIR + this.type.getName();
        try {
            Enumeration<java.net.URL> urls = ClassLoader.getSystemResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                final int ci = line.indexOf('#');
                                if (ci >= 0) line = line.substring(0, ci);
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            name = line.substring(0, i).trim();
                                            line = line.substring(i + 1).trim();
                                        }
                                        if (CommonUtils.notEmpty(name,line)) {
                                            Class<?> clazz = Class.forName(line, true, this.loader);
                                            if (!this.type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        this.type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }
                                            name = getSpiName(name);
                                            if (extInstances.containsKey(name)) {
                                                Object obj = extInstances.get(name);
                                                //check exists....
                                                if (!obj.getClass().equals(clazz)) {
                                                    throw new IllegalStateException("[opencron]: spi'implements name is not unique,already exists instance["+name+"],class: " + obj.getClass().getName() + ",this class:"+this.type.getName());
                                                }
                                            }else {
                                                T instance = this.type.cast(clazz.newInstance());
                                                extInstances.put(name, instance);
                                            }
                                        }
                                    } catch (Throwable t) {
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        } finally {
                            reader.close();
                        }
                    } catch (Throwable t) {
                        logger.error("Exception when load extension class(interface: " +
                                type + ", class file: " + url + ") in " + url, t);
                    }
                } // end of while urls
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", description file: " + fileName + ").", t);
        }

    }

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    public String getSpiName(String spiName) {
        return this.type.getName()+":"+spiName;
    }
}

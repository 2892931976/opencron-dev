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

package org.opencron.common.ext;

import org.opencron.common.Constants;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.CommonUtils;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static org.opencron.common.util.AssertUtils.checkNotNull;

/**
 * @author benjobs...
 */
public final class ExtensionLoader<T> {

    private Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private final Class<T> type;

    private Class<T> instanceType = null;

    private SPI spi;

    private final ClassLoader loader;

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return ExtensionLoader.getExtensionLoader(type, Thread.currentThread().getContextClassLoader());
    }

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type, ClassLoader loader) {
        return new ExtensionLoader(type, loader);
    }

    public T getExtension() {
        try {
            if (instanceType != null) {
                T instance = instanceType.newInstance();
                this.type.cast(instance);
                return instance;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
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
            throw new IllegalArgumentException("Extension type(" + type + ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }
        this.type = checkNotNull(type, "type interface cannot be null");
        this.spi = this.type.getAnnotation(SPI.class);
        this.loader = (loader == null) ? ClassLoader.getSystemClassLoader() : loader;
        loadFile();
    }

    private void loadFile() {
        String fileName = Constants.META_INF_DIR + this.type.getName();
        try {
            Enumeration<URL> urls = ClassLoader.getSystemResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    Scanner scanner = null;
                    try {
                        scanner = new Scanner(new InputStreamReader(url.openStream(), "utf-8"));
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            if (CommonUtils.notEmpty(line)) {
                                //已经注释或者不是K=V结构的统统跳过.
                                if (line.indexOf("#") == 0) {
                                    continue;
                                }

                                line = line.trim();
                                try {
                                    String[] args = line.split("=");
                                    if (args.length != 2) {
                                        throw new IllegalStateException("invalid SPI configuration:" + line + "please check config: " + url);
                                    }
                                    String name = args[0].trim();
                                    line = args[1].trim();
                                    if (CommonUtils.notEmpty(name, line)) {
                                        if (name.equals(this.spi.value())) {
                                            Class clazz = Class.forName(line, false, this.loader);
                                            if (!this.type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        this.type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }
                                            this.instanceType = clazz;
                                            break;
                                        }
                                    }
                                } catch (Throwable t) {
                                    throw new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        logger.error("Exception when load extension class(interface: " + type + ", class file: " + url + ") in " + url, t);
                    } finally {
                        scanner.close();
                    }
                } // end of while urls
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " + type + ", description file: " + fileName + ").", t);
        }

    }

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

}

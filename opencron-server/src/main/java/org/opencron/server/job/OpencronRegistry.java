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

package org.opencron.server.job;

import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.registry.URL;
import org.opencron.registry.api.NotifyListener;
import org.opencron.registry.api.RegistryService;
import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * @author benjobs.
 */
public class OpencronRegistry implements InitializingBean, DisposableBean, NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(OpencronRegistry.class);

    private static final URL SUBSCRIBE = null;

    private RegistryService registryService = ExtensionLoader.load(RegistryService.class);

    /**
     * 每台server启动起来都必须往注册中心注册信息...注册中心在重新统一分配任务到每台server上...
     *
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        logger.info("[opencron]server registry Starting...");
        registryService.subscribe(SUBSCRIBE, this);
    }

    public void destroy() throws Exception {
        registryService.unsubscribe(SUBSCRIBE, this);
    }

    public void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

    }
}
    

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

import net.sf.ehcache.store.chm.ConcurrentHashMap;
import org.opencron.common.Constants;
import org.opencron.common.logging.LoggerFactory;
import org.opencron.common.util.MacUtils;
import org.opencron.common.util.PropertyPlaceholder;
import org.opencron.common.util.StringUtils;
import org.opencron.registry.URL;
import org.opencron.registry.api.RegistryService;
import org.opencron.registry.zookeeper.ChildListener;
import org.opencron.server.service.AgentService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.opencron.common.util.CommonUtils.uuid;

/**
 * @author benjobs.
 */

@Component
public class OpencronRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OpencronRegistry.class);

    private RegistryService registryService = new RegistryService();

    private final URL registryURL = URL.valueOf(PropertyPlaceholder.get(Constants.PARAM_OPENCRON_REGISTRY_KEY));

    private final String registryPath = Constants.ZK_REGISTRY_SERVER_PATH + "/" + StringUtils.join(MacUtils.getAllMac(),"_") + "@" + uuid();

    private Map<String,String> agentMap = new ConcurrentHashMap<String, String>(0);

    @Autowired
    private AgentService agentService;

    /**
     * 每台server启动起来都必须往注册中心注册信息...注册中心在重新统一分配任务到每台server上...
     *
     * @throws Exception
     */
    @PostConstruct
    public void initialization() throws Exception {

        //将server加入到注册中心
        registryService.register(registryURL,registryPath,true);

        //register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (logger.isInfoEnabled()) {
                    logger.info("[opencron] run shutdown hook now...");
                }
                registryService.unregister(registryURL,registryPath);
            }
        }, "OpencronShutdownHook"));

        //agent添加,删除监控...
        registryService.getZKClient(registryURL).addChildListener(Constants.ZK_REGISTRY_AGENT_PATH, new ChildListener() {
            @Override
            public synchronized void childChanged(String path, List<String> children) {
                if (agentMap.isEmpty()) {
                    for (String agent:children) {
                        agentMap.put(agent,agent);
                        logger.info("[opencron] agent connected! info:{}",agent);
                        agentService.doConnect(agent);
                    }
                }else {
                    Map<String,String> map = new ConcurrentHashMap<String, String>(agentMap);
                    for (String agent:children) {
                        map.remove(agent);
                        if (!agentMap.containsKey(agent)) {
                            //新增...
                            agentMap.put(agent,agent);
                            logger.info("[opencron] agent connected! info:{}",agent);
                            agentService.doConnect(agent);
                        }
                    }

                    for (String child:map.keySet()) {
                        agentMap.remove(child);
                        logger.info("[opencron] agent doDisconnect! info:{}",child);
                        agentService.doDisconnect(child);
                    }
                }
            }
        });
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("[opencron] run destroy now...");
        }
        registryService.unregister(registryURL,registryPath);
    }
}
    

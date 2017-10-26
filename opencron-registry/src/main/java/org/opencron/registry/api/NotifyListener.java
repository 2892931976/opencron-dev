/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencron.registry.api;


import org.opencron.common.URL;

public interface NotifyListener {

    /**
     * 当agent或者server发生变更时触发。
     * agent:激活或者销毁
     * server:新增或者移除时
     *
     * URL:----->
     *      server:
     *         server://ip:port?action=active
     *         server://ip:port?action=stop
     *      agent:
     *         agent://ip:port?action=active
     *         agent://ip:port?action=stop
     */
    void notify(URL url);

}
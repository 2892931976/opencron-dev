package org.opencron.registry.api;


public interface Registry {

    /**
     * server或者agent激活或者新增的时候
     * server:一台新的server加入到集群的时候
     * agent:启动的时候
     */

    void register();

    /**
     * server或者agent激活或启动的时候
     * server:一台server停止服务从集群中移除的时候
     * agent:停止的时候
     */
    void unregister();

}

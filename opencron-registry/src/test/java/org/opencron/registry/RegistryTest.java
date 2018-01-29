package org.opencron.registry;

import org.junit.Before;
import org.junit.Test;
import org.opencron.registry.zookeeper.ChildListener;
import org.opencron.registry.zookeeper.ZookeeperClient;
import org.opencron.registry.zookeeper.zkclient.ZkclientZookeeperClient;

import java.io.IOException;
import java.util.List;

public class RegistryTest {

    private ZookeeperClient zookeeperClient;

    String url = "zookeeper://127.0.0.1:2181";

    @Before
    public void init() {
        zookeeperClient = new ZkclientZookeeperClient(URL.valueOf(url));
    }

    @Test
    public void create(){
        zookeeperClient.create("/opencron/agent/123322242",true);
    }

    @Test
    public void delete(){
        zookeeperClient.delete("/opencron/agent");
    }

    @Test
    public void lister() throws IOException {
        zookeeperClient.addChildListener("/opencron",new ChildListener(){
            @Override
            public void childChanged(String path, List<String> children) {
                System.out.println("root:----->"+path);
                for (String child:children) {
                    System.out.println(child);
                }
            }
        });

        System.in.read();
    }

}

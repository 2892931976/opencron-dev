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

  //  @Before
    public void init() {
        URL url = new URL("zookeeper","127.0.0.1",2181);
        zookeeperClient = new ZkclientZookeeperClient(url);
    }

    @Test
    public void create(){
        String url = "zookeeper://127.0.0.1:2181";
        zookeeperClient.create("/opencron",false);
    }

    @Test
    public void delete(){
        zookeeperClient.delete("/opencron");
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

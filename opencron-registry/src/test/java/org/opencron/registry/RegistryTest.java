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
    public void create() throws IOException {
        zookeeperClient.create("/opencron/agent/6",true);
        System.in.read();
    }

    @Test
    public void delete() throws IOException {
        zookeeperClient.delete("/opencron/agent/2");
        System.in.read();
    }

    @Test
    public void lister() throws IOException {



        zookeeperClient.addChildListener("/opencron/agent",new ChildListener(){
            @Override
            public void childChanged(String path, List<String> children) {
                System.out.println("add:----->"+path);
                for (String child:children) {
                    System.out.println(child);
                }
            }
        });

        zookeeperClient.removeChildListener("/opencron/agent",new ChildListener(){
            @Override
            public void childChanged(String path, List<String> children) {
                System.out.println("remove:----->"+path);
                for (String child:children) {
                    System.out.println(child);
                }
            }
        });


        System.in.read();
    }

    @Test
    public void get(){
        List<String> paths = zookeeperClient.getChildren("/opencron/agent");

        for (String path:paths)
            System.out.println(path);
    }

}

package org.opencron.agent;


import org.junit.Test;
import org.opencron.common.utils.ProtostuffUtil;

public class ProtostuffTest {

public static class Person{
    int id;
    String name;
    public Person(){

    }

    public Person(int id, String name){
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }

}

@Test
public void demo(){
    Person p = new Person(1,"ff");
    byte[] arr = ProtostuffUtil.serializer(p);
    Person result = ProtostuffUtil.deserializer(arr, Person.class);
    System.out.println(result.getName());
}
}
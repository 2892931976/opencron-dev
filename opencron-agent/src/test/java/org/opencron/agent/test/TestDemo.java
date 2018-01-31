package org.opencron.agent.test;

import org.junit.Test;
import org.opencron.common.util.SystemPropertyUtils;

public class TestDemo {

    @Test
    public void test1(){
        SystemPropertyUtils.setProperty("xx","123322242");
        SystemPropertyUtils.setProperty("xx","123322242x");

        System.out.println(SystemPropertyUtils.get("xx","123"));
    }
}

package org.opencron.config.spring.support;

import org.springframework.beans.factory.InitializingBean;

public class RegistryConfig implements InitializingBean{

    private String address;

    @Override
    public void afterPropertiesSet() throws Exception {
    }

}

/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencron.config.spring.scheam;

import com.google.common.base.Strings;
import org.opencron.config.spring.support.RegistryConfig;
import org.opencron.config.spring.support.ServerConfig;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Jupiter
 * org.jupiter.spring.schema
 *
 * @author jiachun.fjc
 */
public class OpencronBeanDefinitionParser implements BeanDefinitionParser {

    private final Class<?> beanClass;

    public OpencronBeanDefinitionParser(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        if (beanClass == ServerConfig.class) {
            return parseServerConfig(element, parserContext);
        } else if (beanClass == RegistryConfig.class) {
            return parseRegistryConfig(element, parserContext);
        } else {
            throw new BeanDefinitionValidationException("Unknown class to definition: " + beanClass.getName());
        }
    }

    private BeanDefinition parseServerConfig(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addProperty(def, element, "address", true);
        addPropertyReference(def, element, "acceptor", false);

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "registryServerAddresses", false);
                    addPropertyReference(def, (Element) item, "providerInterceptors", false);
                    addPropertyReference(def, (Element) item, "flowController", false);
                }
            }
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseRegistryConfig(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addProperty(def, element, "appName", false);
        addProperty(def, element, "registryType", false);
        addPropertyReference(def, element, "connector", false);

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "registryServerAddresses", false);
                    addProperty(def, (Element) item, "providerServerAddresses", false);
                }
            }
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseJupiterProvider(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addPropertyReference(def, element, "server", true);
        addPropertyReference(def, element, "providerImpl", true);

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "weight", false);
                    addPropertyReference(def, (Element) item, "providerInterceptors", false);
                    addPropertyReference(def, (Element) item, "executor", false);
                    addPropertyReference(def, (Element) item, "flowController", false);
                    addPropertyReference(def, (Element) item, "providerInitializer", false);
                    addPropertyReference(def, (Element) item, "providerInitializerExecutor", false);
                }
            }
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition registerBean(RootBeanDefinition definition, Element element, ParserContext parserContext) {
        String id = element.getAttribute("id");
        if (Strings.isNullOrEmpty(id)) {
            id = beanClass.getSimpleName();
        }
        if (parserContext.getRegistry().containsBeanDefinition(id)) {
            throw new IllegalStateException("Duplicate jupiter bean id: " + id);
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());

        return definition;
    }

    private static void addProperty(RootBeanDefinition definition, Element element, String propertyName, boolean required) {
        String ref = element.getAttribute(propertyName);
        if (required) {
            checkAttribute(propertyName, ref);
        }
        if (!Strings.isNullOrEmpty(ref)) {
            definition.getPropertyValues().addPropertyValue(propertyName, ref);
        }
    }

    private static void addPropertyReference(RootBeanDefinition definition, Element element, String propertyName, boolean required) {
        String ref = element.getAttribute(propertyName);
        if (required) {
            checkAttribute(propertyName, ref);
        }
        if (!Strings.isNullOrEmpty(ref)) {
            definition.getPropertyValues().addPropertyValue(propertyName, new RuntimeBeanReference(ref));
        }
    }

    private static String checkAttribute(String attributeName, String attribute) {
        if (Strings.isNullOrEmpty(attribute)) {
            throw new BeanDefinitionValidationException("attribute [" + attributeName + "] is required.");
        }
        return attribute;
    }
}

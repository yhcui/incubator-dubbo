/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.spring.schema;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ConfigCenterBean;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * DubboNamespaceHandler
 *
 * @export
 */
public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    @Override
    public void init() {
        /** 应用信息配置 */
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));

        /** 模块信息配置 */
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));

        /** 注册中心配置 */
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("config-center", new DubboBeanDefinitionParser(ConfigCenterBean.class, true));
        registerBeanDefinitionParser("metadata-report", new DubboBeanDefinitionParser(MetadataReportConfig.class, true));
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));

        /** 服务提供者暴露服务配置 service代表一个类 ServiceBean也是服务暴漏的一个节点 */
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));

        /**
         * 服务引用配置, 时机有两个
         * 第一个是在 Spring 容器调用 ReferenceBean 的 afterPropertiesSet 方法时引用服务，
         * 第二个是在 ReferenceBean 对应的服务被注入到其他类中时引用。
         * 这两个引用服务的时机区别在于，第一个是饿汉式的，第二个是懒汉式的。
         * 默认情况下，Dubbo 使用懒汉式引用服务。
         * 如果需要使用饿汉式，可通过配置 <dubbo:reference> 的 init 属性开启
         *
         * 在 Dubbo 中，我们可以通过两种方式引用远程服务。
         * 第一种是使用服务直联的方式引用服务，第二种方式是基于注册中心进行引用。
         * 服务直联的方式仅适合在调试或测试服务的场景下使用，不适合在线上环境使用
         *
         * 当我们的服务被注入到其他类中时，Spring 会第一时间调用 ReferenceBean的getObject 方法，并由该方法执行服务引用逻辑
         * */
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false));
        registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser());
    }

}

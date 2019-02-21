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
package org.apache.dubbo.common;

/**
 *
 * 包含了一个获取配置信息的方法 getUrl，实现该接口的类可以向外提供配置信息
 *
 * Node. (API/SPI, Prototype, ThreadSafe)
 */
public interface Node {

    /**
     * 获取注册的url - 配置信息
     * get url.
     *
     * @return url.
     */
    URL getUrl();

    /**
     * 是否可用
     * is available.
     *
     * @return available.
     */
    boolean isAvailable();

    /**
     * 消毁
     * destroy.
     */
    void destroy();

}
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
package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.support.FailoverCluster;

/**
 *
 * 为了避免单点故障，现在的应用通常至少会部署在两台服务器上。
 * 对于一些负载比较高的服务，会部署更多的服务器。这样，在同一环境下的服务提供者数量会大于1。
 * 对于服务消费者来说，同一环境下出现了多个服务提供者。这时会出现一个问题，服务消费者需要决定选择哪个服务提供者进行调用。
 * 另外服务调用失败时的处理措施也是需要考虑的，是重试呢，还是抛出异常，亦或是只打印异常等。
 * 为了处理这些问题，Dubbo 定义了集群接口 Cluster 以及 Cluster Invoker。
 *
 *
 * 集群 Cluster 用途是将多个服务提供者合并为一个 Cluster Invoker，并将这个 Invoker 暴露给服务消费者
 * 集群模块是服务提供者和服务消费者的中间层，为服务消费者屏蔽了服务提供者的情况，这样服务消费者就可以专心处理远程调用相关事宜
 * 集群工作过程可分为两个阶段
 * 第一个阶段是在服务消费者初始化期间，集群 Cluster 实现类为服务消费者创建 Cluster Invoker 实例
 * 第二个阶段是在服务消费者进行远程调用时
 *
 * Dubbo 提供了多种集群实现，包含但不限于 Failover Cluster、Failfast Cluster 和 Failsafe Cluster 等
 * Failover Cluster: 失败转移
 * Failfast Cluster: 快速失败
 * Failsafe Cluster: 失败安全
 * Failback Cluster: 失败自动恢复
 * Forking Cluster: 并行调用多个服务提供者
 *
 * 集群接口 Cluster 和 Cluster Invoker，这两者是不同的。
 * Cluster 是接口，而 Cluster Invoker 是一种 Invoker。
 * 服务提供者的选择逻辑，以及远程调用失败后的的处理逻辑均是封装在 Cluster Invoker 中
 *
 * Cluster. (SPI, Singleton, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Computer_cluster">Cluster</a>
 * <a href="http://en.wikipedia.org/wiki/Fault-tolerant_system">Fault-Tolerant</a>
 * http://dubbo.apache.org/zh-cn/docs/source_code_guide/cluster.html
 */
@SPI(FailoverCluster.NAME)
public interface Cluster {

    /**
     * Merge the directory invokers to a virtual invoker.
     *
     * @param <T>
     * @param directory
     * @return cluster invoker
     * @throws RpcException
     */
    @Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;

}
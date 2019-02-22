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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * 集群工作过程可分为两个阶段，第一个阶段是在服务消费者初始化期间。
 * 第二个阶段是在服务消费者进行远程调用时，此时 AbstractClusterInvoker 的 invoke 方法会被调用。
 * 列举 Invoker，负载均衡等操作均会在此阶段被执行
 *
 * AbstractClusterInvoker
 */
public abstract class AbstractClusterInvoker<T> implements Invoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClusterInvoker.class);

    protected final Directory<T> directory;

    protected final boolean availablecheck;

    private AtomicBoolean destroyed = new AtomicBoolean(false);

    private volatile Invoker<T> stickyInvoker = null;

    public AbstractClusterInvoker(Directory<T> directory) {
        this(directory, directory.getUrl());
    }

    public AbstractClusterInvoker(Directory<T> directory, URL url) {
        if (directory == null) {
            throw new IllegalArgumentException("service directory == null");
        }

        this.directory = directory;
        //sticky: invoker.isAvailable() should always be checked before using when availablecheck is true.
        this.availablecheck = url.getParameter(Constants.CLUSTER_AVAILABLE_CHECK_KEY, Constants.DEFAULT_CLUSTER_AVAILABLE_CHECK);
    }

    @Override
    public Class<T> getInterface() {
        return directory.getInterface();
    }

    @Override
    public URL getUrl() {
        return directory.getUrl();
    }

    @Override
    public boolean isAvailable() {
        Invoker<T> invoker = stickyInvoker;
        if (invoker != null) {
            return invoker.isAvailable();
        }
        return directory.isAvailable();
    }

    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            directory.destroy();
        }
    }

    /**
     *
     * 主要是对粘滞连接特性的支持上
     *
     * Select a invoker using loadbalance policy.</br>
     * a) Firstly, select an invoker using loadbalance. If this invoker is in previously selected list, or,
     * if this invoker is unavailable, then continue step b (reselect), otherwise return the first selected invoker</br>
     * <p>
     * b) Reselection, the validation rule for reselection: selected > available. This rule guarantees that
     * the selected invoker has the minimum chance to be one in the previously selected list, and also
     * guarantees this invoker is available.
     *
     * @param loadbalance load balance policy
     * @param invocation  invocation
     * @param invokers    invoker candidates
     * @param selected    exclude selected invokers or not
     * @return the invoker which will final to do invoke.
     * @throws RpcException
     */
    protected Invoker<T> select(LoadBalance loadbalance, Invocation invocation,
        List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {

        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }

        // 获取调用方法名
        String methodName = invocation == null ? StringUtils.EMPTY : invocation.getMethodName();

        // 获取 sticky 配置，sticky 表示粘滞连接。所谓粘滞连接是指让服务消费者尽可能的调用同一个服务提供者，除非该提供者挂了再进行切换
        boolean sticky = invokers.get(0).getUrl()
            .getMethodParameter(methodName, Constants.CLUSTER_STICKY_KEY, Constants.DEFAULT_CLUSTER_STICKY);

        // 检测 invokers 列表是否包含 stickyInvoker，如果不包含，说明 stickyInvoker 代表的服务提供者挂了，此时需要将其置空
        // ignore overloaded method
        if (stickyInvoker != null && !invokers.contains(stickyInvoker)) {
            stickyInvoker = null;
        }


        /**
         * 在 sticky 为 true，且 stickyInvoker != null 的情况下。
         * 如果 selected 包含stickyInvoker，表明 stickyInvoker 对应的服务提供者可能因网络原因未能成功提供服务。
         * 但是该提供者并没挂，此时 invokers 列表中仍存在该服务提供者对应的 Invoker，出现这种情况，需要对stickyInvoker进行可用性检查
         * */
        //ignore concurrency problem
        if (sticky && stickyInvoker != null && (selected == null || !selected.contains(stickyInvoker))) {
            // availablecheck 表示是否开启了可用性检查，如果开启了，则调用 stickyInvoker 的
            // isAvailable 方法进行检查，如果检查通过，则直接返回 stickyInvoker。
            if (availablecheck && stickyInvoker.isAvailable()) {
                return stickyInvoker;
            }
        }

        // 如果线程走到当前代码处，说明前面的 stickyInvoker 为空，或者不可用。此时继续调用 doSelect 选择 Invoker
        Invoker<T> invoker = doSelect(loadbalance, invocation, invokers, selected);

        // 如果 sticky 为 true，则将负载均衡组件选出的 Invoker 赋值给 stickyInvoker
        if (sticky) {
            stickyInvoker = invoker;
        }
        return invoker;
    }

    /**
     * 第一是通过负载均衡组件选择 Invoker。
     * 第二是，如果选出来的 Invoker 不稳定，或不可用，此时需要调用 reselect 方法进行重选
     *
     * 若 reselect 选出来的 Invoker 为空，此时定位 invoker 在 invokers 列表中的位置 index，
     * 然后获取 index + 1 处的 invoker，这也可以看做是重选逻辑的一部分
     *
     * @author cuiyuhui
     * @created
     * @param
     * @return
     */
    private Invoker<T> doSelect(LoadBalance loadbalance, Invocation invocation,
        List<Invoker<T>> invokers, List<Invoker<T>> selected) throws RpcException {

        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }

        /** 通过负载均衡组件选择 Invoker */
        Invoker<T> invoker = loadbalance.select(invokers, getUrl(), invocation);

        /** 如果 selected 包含负载均衡选择出的 Invoker，或者该 Invoker 无法经过可用性检查，此时进行重选 */
        //If the `invoker` is in the  `selected` or invoker is unavailable && availablecheck is true, reselect.
        if ((selected != null && selected.contains(invoker))
                || (!invoker.isAvailable() && getUrl() != null && availablecheck)) {
            try {
                // 进行重选
                Invoker<T> rinvoker = reselect(loadbalance, invocation, invokers, selected, availablecheck);
                if (rinvoker != null) {
                    // 如果 rinvoker 不为空，则将其赋值给 invoker
                    invoker = rinvoker;
                } else {
                    // 若 reselect 选出来的 Invoker 为空，此时定位 invoker 在 invokers 列表中的位置 index，然后获取 index + 1 处的 invoker
                    // rinvoker 为空，定位 invoker 在 invokers 中的位置
                    //Check the index of current selected invoker, if it's not the last one, choose the one at index+1.
                    int index = invokers.indexOf(invoker);
                    try {

                        //Avoid collision
                        // 获取 index + 1 位置处的 Invoker
                        invoker = invokers.get((index + 1) % invokers.size());
                    } catch (Exception e) {
                        logger.warn(e.getMessage() + " may because invokers list dynamic change, ignore.", e);
                    }
                }
            } catch (Throwable t) {
                logger.error("cluster reselect fail reason is :" + t.getMessage() + " if can not solve, you can set cluster.availablecheck=false in url", t);
            }
        }
        return invoker;
    }

    /**
     *
     * 第一是查找可用的 Invoker，并将其添加到 reselectInvokers 集合中。
     *   第一件事情又可进行细分，一开始，reselect 从 invokers 列表中查找有效可用的 Invoker，若未能找到，此时再到 selected 列表中继续查找
     * 第二，如果 reselectInvokers 不为空，则通过负载均衡组件再次进行选择
     *
     * Reselect, use invokers not in `selected` first, if all invokers are in `selected`,
     * just pick an available one using loadbalance policy.
     *
     * @param loadbalance
     * @param invocation
     * @param invokers
     * @param selected
     * @return
     * @throws RpcException
     */
    private Invoker<T> reselect(LoadBalance loadbalance, Invocation invocation,
        List<Invoker<T>> invokers, List<Invoker<T>> selected, boolean availablecheck) throws RpcException {

        // 重新进行select的列表。即从这个列表中再次获取相应的Invoker
        //Allocating one in advance, this list is certain to be used.
        List<Invoker<T>> reselectInvokers = new ArrayList<>(
            invokers.size() > 1 ? (invokers.size() - 1) : invokers.size());

        // 填充reselectInvokers列表
        // First, try picking a invoker not in `selected`.
        for (Invoker<T> invoker : invokers) {
            // 如果 availablecheck 为true 则进行available验证
            if (availablecheck && !invoker.isAvailable()) {
                continue;
            }

            // 如果 selected 列表不包含当前 invoker，则将其添加到 reselectInvokers 中
            if (selected == null || !selected.contains(invoker)) {
                reselectInvokers.add(invoker);
            }
        }

        if (!reselectInvokers.isEmpty()) {
            // 通过负载均衡组件进行选择
            return loadbalance.select(reselectInvokers, getUrl(), invocation);
        }

        // 若线程走到此处，说明 reselectInvokers 集合为空，此时不会调用负载均衡组件进行筛选。
        // 这里从 selected 列表中查找可用的 Invoker，并将其添加到 reselectInvokers 集合中
        // Just pick an available invoker using loadbalance policy
        if (selected != null) {
            for (Invoker<T> invoker : selected) {
                if ((invoker.isAvailable()) // available first
                        && !reselectInvokers.contains(invoker)) {
                    reselectInvokers.add(invoker);
                }
            }
        }
        if (!reselectInvokers.isEmpty()) {
            // 再次进行选择，并返回选择结果
            return loadbalance.select(reselectInvokers, getUrl(), invocation);
        }

        return null;
    }

    /**
     *
     * TODO - 调用方是否为消费者????
     * 1. 查询出Invoker List
     * 2. 加载LoadBalance
     * 3. 调用抽象方法doInvoke
     * @author cuiyuhui
     * @created
     * @param
     * @return
     */
    @Override
    public Result invoke(final Invocation invocation) throws RpcException {
        checkWhetherDestroyed();

        /** 绑定 attachments 到 invocation 中 */
        // binding attachments into invocation.
        Map<String, String> contextAttachments = RpcContext.getContext().getAttachments();
        if (contextAttachments != null && contextAttachments.size() != 0) {
            ((RpcInvocation) invocation).addAttachments(contextAttachments);
        }

        /** 列举 Invoker -- 从Directory中获取  */
        List<Invoker<T>> invokers = list(invocation);

        /** 加载 LoadBalance */
        LoadBalance loadbalance = initLoadBalance(invokers, invocation);
        RpcUtils.attachInvocationIdIfAsync(getUrl(), invocation);

        /** 调用 doInvoke 进行后续操作 */
        return doInvoke(invocation, invokers, loadbalance);
    }

    protected void checkWhetherDestroyed() {

        if (destroyed.get()) {
            throw new RpcException("Rpc cluster invoker for " + getInterface() + " on consumer " + NetUtils.getLocalHost()
                    + " use dubbo version " + Version.getVersion()
                    + " is now destroyed! Can not invoke any more.");
        }
    }

    @Override
    public String toString() {
        return getInterface() + " -> " + getUrl().toString();
    }

    protected void checkInvokers(List<Invoker<T>> invokers, Invocation invocation) {
        if (CollectionUtils.isEmpty(invokers)) {
            throw new RpcException(RpcException.NO_INVOKER_AVAILABLE_AFTER_FILTER, "Failed to invoke the method "
                    + invocation.getMethodName() + " in the service " + getInterface().getName()
                    + ". No provider available for the service " + directory.getUrl().getServiceKey()
                    + " from registry " + directory.getUrl().getAddress()
                    + " on the consumer " + NetUtils.getLocalHost()
                    + " using the dubbo version " + Version.getVersion()
                    + ". Please check if the providers have been started and registered.");
        }
    }
    /** 抽象方法，由子类实现 */
    protected abstract Result doInvoke(Invocation invocation, List<Invoker<T>> invokers,
                                       LoadBalance loadbalance) throws RpcException;

    /**
     * 从Directory中列出所有的Invoker
     * @author cuiyuhui
     * @created
     * @param
     * @return
     */
    protected List<Invoker<T>> list(Invocation invocation) throws RpcException {
        return directory.list(invocation);
    }

    /**
     *
     * 根据invokers和invocation获取LoadBalance
     * 最终是根据url中配置的方法中获取对应的LoadBalance
     *
     * Init LoadBalance.
     * <p>
     * if invokers is not empty, init from the first invoke's url and invocation
     * if invokes is empty, init a default LoadBalance(RandomLoadBalance)
     * </p>
     *
     * @param invokers   invokers
     * @param invocation invocation
     * @return LoadBalance instance. if not need init, return null.
     */
    protected LoadBalance initLoadBalance(List<Invoker<T>> invokers, Invocation invocation) {
        if (CollectionUtils.isNotEmpty(invokers)) {
            // 获取配置的负载均衡算法，如何没有，使用默认负载均衡算法
            return ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(invokers.get(0).getUrl()
                    .getMethodParameter(RpcUtils.getMethodName(invocation), Constants.LOADBALANCE_KEY, Constants.DEFAULT_LOADBALANCE));
        } else {
            // 获取默认负载均衡算法，默认为random
            return ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(Constants.DEFAULT_LOADBALANCE);
        }
    }
}

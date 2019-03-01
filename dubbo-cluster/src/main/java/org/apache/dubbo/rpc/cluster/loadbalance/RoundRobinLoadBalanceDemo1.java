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
package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.AtomicPositiveInteger;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author cuiyuhui
 * @created
 * @param
 * @return
 * @deprecated
 * @version dubbo 2.6.4
 */
public class RoundRobinLoadBalanceDemo1 extends AbstractLoadBalance {
    public static final String NAME = "roundrobin";

    private final ConcurrentMap<String, AtomicPositiveInteger> sequences =
            new ConcurrentHashMap<String, AtomicPositiveInteger>();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // key = 全限定类名 + "." + 方法名，比如 com.xxx.DemoService.sayHello
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        int length = invokers.size();
        // 最大权重
        int maxWeight = 0;
        // 最小权重
        int minWeight = Integer.MAX_VALUE;

        //key:invoker, value:权重
        final LinkedHashMap<Invoker<T>, IntegerWrapper> invokerToWeightMap = new LinkedHashMap<Invoker<T>, IntegerWrapper>();
        // 权重总和
        int weightSum = 0;

        // 下面这个循环主要用于查找最大和最小权重，计算权重总和等.length是invokers的长度
        for (int i = 0; i < length; i++) {

            // 获取循环中的权重
            int weight = getWeight(invokers.get(i), invocation);
            // 获取最大和最小权重
            maxWeight = Math.max(maxWeight, weight);
            minWeight = Math.min(minWeight, weight);
            if (weight > 0) {
                // 将 weight 封装到 IntegerWrapper 中
                invokerToWeightMap.put(invokers.get(i), new IntegerWrapper(weight));
                // 累加权重
                weightSum += weight;
            }
        }

        // 查找 key 对应的对应 AtomicPositiveInteger 实例，为空则创建。
        // 这里可以把 AtomicPositiveInteger 看成一个黑盒，大家只要知道
        // AtomicPositiveInteger 用于记录服务的调用编号即可。至于细节，
        AtomicPositiveInteger sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new AtomicPositiveInteger());
            sequence = sequences.get(key);
        }

        // 获取当前的调用编号 -- 并且自加1, key调用次数
        int currentSequence = sequence.getAndIncrement();
        // 权重不相等 -- 如果最小权重小于最大权重，表明服务提供者之间的权重是不相等的
        // maxWeight == 0 代表minWeight也是0 -- 这个地方的 maxWeight > 0 需要判断?
        if (maxWeight > 0 && minWeight < maxWeight) {

            /**
             *
             * 因为mod是全局取出来的,所以是可以实现
             * 使用调用编号对权重总和进行取余操作
             *
             * */
            int mod = currentSequence % weightSum;
            /**
             假设我们有三台服务器 servers = [A, B, C]，对应的权重为 weights = [2, 5, 1]。
             接下来对上面的逻辑进行简单的模拟。
             mod = 0：满足条件，此时直接返回服务器 A
             mod = 1：需要进行一次递减操作才能满足条件，此时返回服务器 B
             mod = 2：需要进行两次递减操作才能满足条件，此时返回服务器 C
             mod = 3：需要进行三次递减操作才能满足条件，经过递减后，服务器权重为 [1, 4, 0]，此时返回服务器 A
             mod = 4：需要进行四次递减操作才能满足条件，经过递减后，服务器权重为 [0, 4, 0]，此时返回服务器 B
             mod = 5：需要进行五次递减操作才能满足条件，经过递减后，服务器权重为 [0, 3, 0]，此时返回服务器 B
             mod = 6：需要进行六次递减操作才能满足条件，经过递减后，服务器权重为 [0, 2, 0]，此时返回服务器 B
             mod = 7：需要进行七次递减操作才能满足条件，经过递减后，服务器权重为 [0, 1, 0]，此时返回服务器 B

             经过8次调用后，我们得到的负载均衡结果为 [A, B, C, A, B, B, B, B]，次数比 A:B:C = 2:5:1，等于权重比。
             当 sequence = 8 时，mod = 0，此时重头再来。从上面的模拟过程可以看出，
             当 mod >= 3 后，服务器 C 就不会被选中了，因为它的权重被减为0了。
             当 mod >= 4 后，服务器 A 的权重被减为0，此后 A 就不会再被选中。
             */
            // 进行 maxWeight 次遍历
            for (int i = 0; i < maxWeight; i++) {
                // 遍历 invokerToWeightMap
                for (Map.Entry<Invoker<T>, IntegerWrapper> each : invokerToWeightMap.entrySet()) {
                    // 获取 Invoker
                    final Invoker<T> k = each.getKey();
                    // 获取权重包装类 IntegerWrapper
                    final IntegerWrapper v = each.getValue();

                    /**
                     * 在某些情况下存在着比较严重的性能问题
                     * 问题出在了 Invoker 的返回时机上，
                     * RoundRobinLoadBalance 需要在mod == 0 && v.getValue() > 0 条件成立的情况下才会被返回相应的 Invoker。
                     * 假如 mod 很大，比如 10000，50000，甚至更大时，doSelect 方法需要进行很多次计算才能将 mod 减为0。
                     * 由此可知，doSelect 的效率与 mod 有关，时间复杂度为 O(mod)。
                     * mod 又受最大权重 maxWeight 的影响，因此当某个服务提供者配置了非常大的权重，
                     * 此时 RoundRobinLoadBalance 会产生比较严重的性能问题
                     * issues https://github.com/apache/incubator-dubbo/issues/2578
                     * */
                    // 如果 mod = 0，且权重大于0，此时返回相应的 Invoker
                    if (mod == 0 && v.getValue() > 0) {
                        return k;
                    }

                    // mod != 0，且权重大于0，此时对权重和 mod 分别进行自减操作
                    if (v.getValue() > 0) {
                        v.decrement();
                        mod--;
                    }
                }
            }
        }

        // 服务提供者之间的权重相等，此时通过轮询选择 Invoker
        return invokers.get(currentSequence % length);
    }

    // IntegerWrapper 是一个 int 包装类，主要包含了一个自减方法。
    private static final class IntegerWrapper {
        private int value;

        public IntegerWrapper(int value) {
            this.value = value;
        }

        public void decrement() {
            this.value--;
        }

        public int getValue() {
            return this.value;
        }
    }
}

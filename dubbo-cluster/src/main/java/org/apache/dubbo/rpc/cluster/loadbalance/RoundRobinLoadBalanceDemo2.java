package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.AtomicPositiveInteger;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <p>TODO</p>
 * <p>
 * <PRE>
 * <BR>    修改记录
 * <BR>-----------------------------------------------
 * <BR>    修改日期         修改人          修改内容
 * </PRE>
 * 最大公因数，也称最大公约数、最大公因子，指两个或多个整数共有约数中最大的一个。
 * a，b的最大公约数记为（a，b），同样的，a，b，c的最大公约数记为（a，b，c），多个整数的最大公约数也有同样的记号。
 * 求最大公约数有多种方法，常见的有质因数分解法、短除法、辗转相除法、更相减损法。
 * 与最大公约数相对应的概念是最小公倍数，a，b的最小公倍数记为[a，b]
 * 参考:
 * https://blog.csdn.net/gqtcgq/article/details/52076997
 * https://blog.csdn.net/zhangskd/article/details/50194069
 *
 * 加权轮询: 后端序列是这样的：{ c, b, a, a, a, a, a }，会有5个连续的请求落在后端a上，分布不太均匀
 *
 * 平滑的加权轮询:它和前者的区别是,每7个请求对应的后端序列为 { a, a, b, a, c, a, a }，
 * 转发给后端a的5个请求现在分散开来，不再是连续的。
 * @author cuiyh9
 * @version 1.0
 * @Date Created in 2019年03月01日 20:42
 * @since 1.0
 * @deprecated
 */
public class RoundRobinLoadBalanceDemo2 extends AbstractLoadBalance {
    public static final String NAME = "roundrobin";

    private final ConcurrentMap<String, AtomicPositiveInteger> sequences = new ConcurrentHashMap<String, AtomicPositiveInteger>();

    private final ConcurrentMap<String, AtomicPositiveInteger> indexSeqs = new ConcurrentHashMap<String, AtomicPositiveInteger>();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        int length = invokers.size();
        // 最大权重
        int maxWeight = 0;
        //最小权重
        int minWeight = Integer.MAX_VALUE;

        // 权重大于0的invoker集合
        final List<Invoker<T>> invokerToWeightList = new ArrayList<>();

        // 查找最大和最小权重，并将权重大于0的invoker放到invokerToWeightList集合中
        for (int i = 0; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation);
            maxWeight = Math.max(maxWeight, weight);
            minWeight = Math.min(minWeight, weight);
            if (weight > 0) {
                invokerToWeightList.add(invokers.get(i));
                // 此处没有累加权重
            }
        }

        //key:服务全路径, 获取当前服务对应的调用序列对象 AtomicPositiveInteger
        AtomicPositiveInteger sequence = sequences.get(key);
        if (sequence == null) {
            // 此处多线程并发访问时是否存在问题? 不会,incrementAndGet方法也是线程安全的，此处只要保证放一个即可
            // 创建 AtomicPositiveInteger，默认值为0
            sequences.putIfAbsent(key, new AtomicPositiveInteger());
            sequence = sequences.get(key);
        }

        // 获取下标序列对象 AtomicPositiveInteger
        AtomicPositiveInteger indexSeq = indexSeqs.get(key);
        if (indexSeq == null) {
            // 创建 AtomicPositiveInteger，默认值为 -1
            indexSeqs.putIfAbsent(key, new AtomicPositiveInteger(-1));
            indexSeq = indexSeqs.get(key);
        }
        /**
         每进行一轮循环，重新计算 currentWeight。如果当前 Invoker 权重大于 currentWeight，则返回该 Invoker
         下面举例说明，假设服务器 [A, B, C] 对应权重 [5, 2, 1]。
         第一轮循环，currentWeight = 1，可返回 A 和 B
         第二轮循环，currentWeight = 2，返回 A
         第三轮循环，currentWeight = 3，返回 A
         第四轮循环，currentWeight = 4，返回 A
         第五轮循环，currentWeight = 0，返回 A, B, C
         如上，这里的一轮循环是指 index 再次变为0所经历过的循环，这里可以把 index = 0 看做是一轮循环的开始。
         每一轮循环的次数与 Invoker 的数量有关，Invoker 数量通常不会太多，所以我们可以认为上面代码的时间复杂度为常数级。

         下面举例说明，假设服务器 [A, B, C] 对应权重 [5, 2, 1]。
         第一次
         indexSeq = 0, length = 3 , index = 0
         sequence = 0, maxWeight = 5, currentWeight = 0
         执行 index = 0
            sequence = 1, maxWeight = 5, currentWeight = 1
            getWeight =  5, currentWeight = 1, 反回A

         第二次
            indexSeq = 1, length = 3 , index = 1
            sequence = 1, maxWeight = 5, currentWeight = 1
            getWeight =  2, currentWeight = 1, 反回B

         第三次
             indexSeq = 2, length = 3 , index = 2
             sequence = 2, maxWeight = 5, currentWeight = 2
             getWeight =  1, currentWeight = 1, 不做处理

         第四次
             indexSeq = 3, length = 3 , index = 0
             sequence = 3, maxWeight = 5, currentWeight = 3
             getWeight =  5, currentWeight = 3, 反回A
         第五次
             indexSeq = 4, length = 3 , index = 1
             sequence = 4, maxWeight = 5, currentWeight = 4
             getWeight =  2, currentWeight = 4, 不做处理
         第六次
             indexSeq = 5, length = 3 , index = 2
             sequence = 5, maxWeight = 5, currentWeight = 0
             getWeight =  1, currentWeight = 0, 不做处理
         第七次
             indexSeq = 6, length = 3 , index = 0
             sequence = 6, maxWeight = 5, currentWeight = 1
             执行 index = 0
                 sequence = 6, maxWeight = 5, currentWeight = 1
                 getWeight =  5, currentWeight = 1, 反回A
             ....
         */
        if (maxWeight > 0 && minWeight < maxWeight) {

            //获取权重大于0的集合
            length = invokerToWeightList.size();
            while (true) {
                // 获取每次循环 拿到对比权重值的下标
                int index = indexSeq.incrementAndGet() % length;
                // 当前权重值
                int currentWeight = sequence.get() % maxWeight;

                // index为0 计算当前调用的权重算法进行了变更
                if (index == 0) {
                    // 1 = 1 % 5; 1 = 6 % 5;
                    currentWeight = sequence.incrementAndGet() % maxWeight;
                }

                // 检测 Invoker 的权重是否大于 currentWeight，大于则返回
                if (getWeight(invokerToWeightList.get(index), invocation) > currentWeight) {
                    return invokerToWeightList.get(index);
                }
            }
        }

        // 所有 Invoker 权重相等，此时进行普通的轮询即可
        return invokers.get(sequence.incrementAndGet() % length);
    }
    /**
     * 问题:新的 RoundRobinLoadBalance 在某些情况下选出的服务器序列不够均匀。
     * 比如，服务器 [A, B, C] 对应权重 [5, 1, 1]。
     * 进行7次负载均衡后，选择出来的序列为 [A, A, A, A, A, B, C]。
     * 前5个请求全部都落在了服务器 A上，这将会使服务器 A 短时间内接收大量的请求，压力陡增。
     * 而 B 和 C 此时无请求，处于空闲状态。
     * 而我们期望的结果是这样的 [A, A, B, A, C, A, A]，不同服务器可以穿插获取请求
     * */
}

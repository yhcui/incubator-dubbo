package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.bytecode.ClassGenerator;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.rpc.service.EchoService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 系统生成的Proxy代理类例子 -- 消费端
 * <p>
 * <PRE>
 * <BR>    修改记录
 * <BR>-----------------------------------------------
 * <BR>    修改日期         修改人          修改内容
 * </PRE>
 *
 * @author cuiyh9
 * @version 1.0
 * @Date Created in 2019年03月05日 14:52
 * @since 1.0
 */
public class DemoProxy0 implements ClassGenerator.DC, EchoService, DemoService {
    // 方法数组
    public static Method[] methods;
    private InvocationHandler handler;

    public DemoProxy0() {
    }

    public DemoProxy0(InvocationHandler invocationHandler) {
        this.handler = invocationHandler;
    }



    @Override
    public String sayHello(String string) {
        // 将参数存储到 Object 数组中
        Object[] arrobject = new Object[]{string};
        // 调用 InvocationHandler 实现类的 invoke 方法得到调用结果
        Object object = null;
        try {
            object = this.handler.invoke(this, methods[0], arrobject);
            // 返回调用结果
            return (String)object;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }

    /** 回声测试方法 */
    @Override
    public Object $echo(Object object) {
        Object[] arrobject = new Object[]{object};
        Object object2 = null;
        try {
            object2 = this.handler.invoke(this, methods[1], arrobject);
            return object2;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }
}

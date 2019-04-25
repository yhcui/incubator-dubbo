package com.dubbo.demo.cuiyh.javaspi;

import com.dubbo.demo.cuiyh.dubbospi.Fruit;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.junit.Test;

/**
 * <p>TODO</p>
 * <p>
 * <PRE>
 * <BR>    修改记录
 * <BR>-----------------------------------------------
 * <BR>    修改日期         修改人          修改内容
 * </PRE>
 *
 * @author cuiyh9
 * @version 1.0
 * @Date Created in 2019年04月25日 16:55
 * @since 1.0
 */
public class DubboSpiRobotTest {
    @Test
    public void testSayHello() throws Exception {
        ExtensionLoader<Fruit> extensionLoader =
                ExtensionLoader.getExtensionLoader(Fruit.class);
        Fruit fruit = extensionLoader.getExtension("grape");
        fruit.sayHello();
        Fruit peach = extensionLoader.getExtension("peach");
        peach.sayHello();
    }
}

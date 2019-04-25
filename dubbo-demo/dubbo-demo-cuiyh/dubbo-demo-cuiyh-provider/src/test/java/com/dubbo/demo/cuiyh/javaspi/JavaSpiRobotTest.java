package com.dubbo.demo.cuiyh.javaspi;

import org.junit.Test;

import java.util.ServiceLoader;

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
 * @Date Created in 2019年04月25日 16:25
 * @since 1.0
 */
public class JavaSpiRobotTest {

    @Test
    public void testSayHello() throws Exception {
        ServiceLoader<Robot> serviceLoader = ServiceLoader.load(Robot.class);
        System.out.println("JAVA SPI");
        serviceLoader.forEach(Robot::sayHello);
        serviceLoader.iterator();
    }
}

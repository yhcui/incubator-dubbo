package org.apache.spi.demo;

import org.apache.dubbo.common.extension.ExtensionLoader;

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
 * @Date Created in 2019年02月06日 17:35
 * @since 1.0
 */
public class DubboSPITest {

    public static void main(String[] args) {
        ExtensionLoader<Robot> extensionLoader = ExtensionLoader.getExtensionLoader(Robot.class);
        Robot optimusPrimeRobot = extensionLoader.getExtension("optimusPrime");
        optimusPrimeRobot.sayHello();

        Robot obumblebeeRobot = extensionLoader.getExtension("bumblebee");
        obumblebeeRobot.sayHello();

    }
}

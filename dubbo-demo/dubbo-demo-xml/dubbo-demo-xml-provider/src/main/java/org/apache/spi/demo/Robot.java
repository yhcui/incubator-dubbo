package org.apache.spi.demo;

import org.apache.dubbo.common.extension.SPI;

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
 * @Date Created in 2019年02月06日 17:27
 * @since 1.0
 */
@SPI
public interface Robot {

    void sayHello();

}

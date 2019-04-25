package com.dubbo.demo.cuiyh.dubbospi;

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
 * @Date Created in 2019年04月25日 17:08
 * @since 1.0
 */
@SPI
public interface Fruit {
    void sayHello();
}

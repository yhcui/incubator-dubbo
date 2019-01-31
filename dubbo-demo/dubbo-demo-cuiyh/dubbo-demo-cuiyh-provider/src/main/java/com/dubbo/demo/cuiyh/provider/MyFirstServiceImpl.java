package com.dubbo.demo.cuiyh.provider;

import com.dubbo.demo.cuiyh.consumer.MyFirstService;

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
 * @Date Created in 2019年01月31日 16:28
 * @since 1.0
 */
public class MyFirstServiceImpl implements MyFirstService {
    @Override
    public String first(String name) {
        System.out.println("Hello ," + name);
        return "return" + name;
    }
}

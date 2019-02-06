package com.dubbo.demo.cuiyh.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * DubboNamespaceHandler
 * ServiceBean -- spring 生命周期
 * <p>
 * <PRE>
 * <BR>    修改记录
 * <BR>-----------------------------------------------
 * <BR>    修改日期         修改人          修改内容
 * </PRE>
 *
 * @author cuiyh9
 * @version 1.0
 * @Date Created in 2019年01月31日 16:13
 * @since 1.0
 */
public class CuiyhProvider {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-provider.xml");
        context.start();
        System.out.println("cuiyh provider start-----");
        System.in.read();

    }
}

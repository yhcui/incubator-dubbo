package org.apache.spi.demo.adaptive;

import org.apache.dubbo.common.URL;
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
 * @Date Created in 2019年02月08日 13:39
 * @since 1.0
 */
public class AdaptiveWheelMaker implements  WheelMaker {

    @Override
    public Wheel makeWheel(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }

        String wheelMakerName = url.getParameter("Wheel.maker");
        if (wheelMakerName == null) {
            throw  new IllegalArgumentException("wheelMakerName == null");
        }

        WheelMaker wheelMaker = ExtensionLoader.getExtensionLoader(WheelMaker.class)
                .getExtension(wheelMakerName);


        return wheelMaker.makeWheel(url);
    }

}

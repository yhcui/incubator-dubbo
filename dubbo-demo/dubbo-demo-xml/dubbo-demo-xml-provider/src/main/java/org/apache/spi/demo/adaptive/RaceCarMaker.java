package org.apache.spi.demo.adaptive;

import org.apache.dubbo.common.URL;

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
 * @Date Created in 2019年02月08日 13:44
 * @since 1.0
 */
public class RaceCarMaker implements CarMaker {

    WheelMaker wheelMaker;

    public void setWheelMaker(WheelMaker wheelMaker) {
        this.wheelMaker = wheelMaker;
    }

    @Override
    public Car makeCar(URL url) {
        Wheel wheel = wheelMaker.makeWheel(url);
        return null;
    }
}

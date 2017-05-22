package xzy.device;

/**
 * Created by zhiyongxu on 2017/3/14.
 */

public class DeviceObject {
    public String id;
    public String model;
    public String deviceType;
    public String firmware;
    public String mainboardFirmware;
    public String mainboard;
    public int rssi;
    boolean isOpened=false;

    public boolean isOpened() {
        return isOpened;
    }

}

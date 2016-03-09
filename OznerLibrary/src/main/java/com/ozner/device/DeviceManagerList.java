package com.ozner.device;

import android.content.Context;

import com.ozner.AirPurifier.AirPurifierManager;
import com.ozner.MusicCap.MusicCap;
import com.ozner.MusicCap.MusicCapMgr;
import com.ozner.WaterPurifier.WaterPurifierManager;
import com.ozner.WaterReplenishmentMeter.WaterReplenishmentMeterMgr;
import com.ozner.cup.CupManager;
import com.ozner.tap.TapManager;

import java.util.ArrayList;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class DeviceManagerList extends ArrayList<BaseDeviceManager> {
    CupManager cupManager;
    TapManager tapManager;
    WaterPurifierManager waterPurifierManager;
    AirPurifierManager airPurifierManager;
    WaterReplenishmentMeterMgr waterReplenishmentMeterMgr;
    MusicCapMgr musicCapMgr;
    public DeviceManagerList(Context context) {
        cupManager = new CupManager(context);
        tapManager = new TapManager(context);
        waterPurifierManager = new WaterPurifierManager(context);
        airPurifierManager = new AirPurifierManager(context);
        waterReplenishmentMeterMgr = new WaterReplenishmentMeterMgr(context);
        musicCapMgr = new MusicCapMgr(context);

        add(cupManager);
        add(tapManager);
        add(waterPurifierManager);
        add(airPurifierManager);
        add(waterReplenishmentMeterMgr);
        add(musicCapMgr);

    }

    public TapManager tapManager() {
        return tapManager;
    }

    public CupManager cupManager() {
        return cupManager;
    }

    public WaterPurifierManager waterPurifierManager() {
        return waterPurifierManager;
    }

    public AirPurifierManager airPurifierManager() {
        return airPurifierManager;
    }
    public WaterReplenishmentMeterMgr waterReplenishmentMeterMgr() {
        return waterReplenishmentMeterMgr;
    }

}

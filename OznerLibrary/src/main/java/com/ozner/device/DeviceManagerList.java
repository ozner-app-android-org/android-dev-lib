package com.ozner.device;

import android.content.Context;

import com.ozner.WaterPurifier.WaterPurifierManager;
import com.ozner.cup.CupManager;
import com.ozner.tap.TapManager;

/**
 * Created by xzyxd on 2015/11/2.
 */
public class DeviceManagerList {
    CupManager cupManager;
    TapManager tapManager;
    WaterPurifierManager waterPurifierManager;
    public DeviceManagerList(Context context)
    {
        cupManager=new CupManager(context);
        tapManager=new TapManager(context);
        waterPurifierManager=new WaterPurifierManager(context);
    }
    public TapManager tapManager()
    {
        return tapManager;
    }
    public CupManager cupManager()
    {
        return cupManager;
    }
    public WaterPurifierManager waterPurifierManager()
    {
        return waterPurifierManager;
    }
}

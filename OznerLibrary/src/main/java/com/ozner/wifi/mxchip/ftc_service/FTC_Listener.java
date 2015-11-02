package com.ozner.wifi.mxchip.ftc_service;

public interface FTC_Listener {
    public void onFTCfinished(String jsonString);

    public void isSmallMTU(int MTU);
}

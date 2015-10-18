package com.ozner.wifi.mxchip.ftc_service;

import java.net.Socket;

public interface FTC_Listener {
    public void onFTCfinished(Socket s, String jsonString);

    public void isSmallMTU(int MTU);
}

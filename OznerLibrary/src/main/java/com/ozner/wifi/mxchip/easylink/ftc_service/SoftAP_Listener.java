/**
 *
 */
package com.ozner.wifi.mxchip.easylink.ftc_service;

/**
 * @author Perry
 * @date 2014-12-3
 */
public interface SoftAP_Listener {

    public void onSoftAPconfigOK(int status_code);

    public void onSoftAPconfigFail(int status_code);

    public void onDeviceRegisterOK();

    public void onDeviceRegisterFail();

    public void onAPConnectOK();

    public void onAPConnectFail();

    public void onBindOK(String json);

    public void onBindFail();

}

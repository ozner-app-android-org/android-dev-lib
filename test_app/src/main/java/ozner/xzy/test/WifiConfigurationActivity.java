package ozner.xzy.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.ozner.wifi.mxchip.ConfigurationDevice;
import com.ozner.wifi.mxchip.WifiConfiguration;


/**
 * A placeholder fragment containing a simple view.
 */
public class WifiConfigurationActivity extends Activity implements WifiConfiguration.WifiConfigurationListener {
    public static final String ActivityResultKey="Result";

    EditText wifi_ssid;
    EditText wifi_passwd;
    CircularProgressButton nextButton;
    CheckBox showpasswd;
    WifiManager wifiManager;
    SharedPreferences wifiPreferences;
    Monitor monitor;
    ConfigurationDevice selectionDevice = null;
    WifiConfiguration wifiConfiguration;
    Handler handler;
    private void loadDevice()
    {
        if (selectionDevice==null)
        {
            findViewById(R.id.deviceInfoPanel).setVisibility(View.INVISIBLE);
        }else {
            ((TextView) this.findViewById(R.id.name)).setText("名称:" + selectionDevice.name);
            ((TextView) this.findViewById(R.id.type)).setText("类型:" + selectionDevice.Type);
            ((TextView) this.findViewById(R.id.rom)).setText("rom:" + selectionDevice.firmware);
            ((TextView) this.findViewById(R.id.ip)).setText("IP:" + selectionDevice.localIP);
            ((TextView) this.findViewById(R.id.connected)).setText("激活:" + selectionDevice.activated);
            findViewById(R.id.deviceInfoPanel).setVisibility(View.VISIBLE);
            if ((selectionDevice.activated) && (!selectionDevice.activeDeviceID.isEmpty())) {
                nextButton.setCompleteText("完成");
            }
            nextButton.setProgress(100);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (selectionDevice!=null) {
            outState.putString("selectDevice", selectionDevice.toJSON());
        }else
        {
            outState.remove("selectDevice");
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler=new Handler();
        setContentView(R.layout.activity_wifi_configuration);
        wifiConfiguration = new WifiConfiguration(this);
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiPreferences = this.getSharedPreferences("WifiPassword", Context.MODE_PRIVATE);
        monitor = new Monitor();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.registerReceiver(monitor, filter);

        wifi_ssid = (EditText) this.findViewById(R.id.wifi_ssid);
        wifi_passwd = (EditText) this.findViewById(R.id.wifi_password);
        nextButton = (CircularProgressButton) this.findViewById(R.id.NextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickStartButton();
            }
        });
        nextButton.setIndeterminateProgressMode(true);

        showpasswd = (CheckBox) this.findViewById(R.id.showPassword);
        showpasswd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    wifi_passwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else
                    wifi_passwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        });

        loadWifi();
        if (savedInstanceState!=null) {
            if (savedInstanceState.containsKey("selectDevice")) {
                selectionDevice = ConfigurationDevice.loadByJSON(savedInstanceState.getString("selectDevice"));
                nextButton.setProgress(100);
            }
        }

        //Intent intent=new Intent(this,WifiActivateActivity.class);
        //intent.putExtra("json", "{\"Type\":\"FOG_HAOZE_AIR\",\"ap\":\"ITDEV\",\"firmware\":\"FOG_HAOZE_AIR@004\",\"devPasswd\":\"12345678\",\"activated\":false,\"localPort\":8000,\"name\":\"EMW3162(C04DF4)\",\"connected\":false,\"localIP\":\"10.203.1.74\",\"loginId\":\"admin\"}");


        //this.startActivityForResult(intent, 0x100);

    }

    private void loadWifi() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                String ssid = wifiInfo.getSSID().replace("\"", "");
                wifi_ssid.setText(ssid);
                String pwd = wifiPreferences.getString("password." + ssid, "");
                wifi_passwd.setText(pwd);

            } else
                wifi_ssid.setText("");
        } else {
            wifi_ssid.setText("");
        }
    }

    @Override
    public void onWifiConfiguration(ConfigurationDevice device) {
        selectionDevice=device;
        handler.post(new Runnable() {
            @Override
            public void run() {
                loadDevice();
            }
        });
    }

    @Override
    public void onWifiConfigurationStart() {
        nextButton.setProgress(0);
        nextButton.setProgress(50);
    }

    @Override
    public void onWifiConfigurationStop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                nextButton.setProgress(selectionDevice==null?-1:100);
            }
        });
    }

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                loadWifi();
            }
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                loadWifi();
            }
        }
    }

    @Override
    protected void onDestroy() {
        wifiConfiguration.stopWifiConfiguration();
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode==0x100) && (resultCode==0x100))
        {
            Intent intent=new Intent();
            intent.putExtra(ActivityResultKey,selectionDevice.toJSON());
            setResult(RESULT_OK, intent);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void onClickStartButton() {
        if(nextButton.getProgress()==100) {
            if (selectionDevice!=null) {
                if ((selectionDevice.activated) && (!selectionDevice.activeDeviceID.isEmpty())) {
                    Intent intent = new Intent();
                    intent.putExtra(ActivityResultKey, selectionDevice.toJSON());
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Intent intent = new Intent(this, WifiActivateActivity.class);
                    intent.putExtra("json", selectionDevice.toJSON());
                    this.startActivity(intent);
                }
            }
        }else
        {
            if (wifiManager.getConnectionInfo().getSupplicantState()!=SupplicantState.COMPLETED)
            {
                Toast toast = Toast.makeText(this, "没有连接WIFI", Toast.LENGTH_LONG);
                toast.show();
                return;
            }
            String ssid=wifi_ssid.getText().toString().trim();
            if (ssid.isEmpty())
            {
                Toast toast=Toast.makeText(this, "没有设置SSID", Toast.LENGTH_LONG);
                toast.show();
            }

            SharedPreferences.Editor editor=wifiPreferences.edit();
            try {
                editor.putString("password." + ssid, wifi_passwd.getText().toString());
            }finally {
                editor.commit();
            }
            wifiConfiguration.startWifiConfiguration(ssid, wifi_passwd.getText().toString(), this);
        }
    }
    private void startConfiguration()
    {

    }


}

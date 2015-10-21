package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;


/**
 * A placeholder fragment containing a simple view.
 */
public class AddWifiActivityFragment extends Fragment {
    EditText wifi_ssid;
    EditText wifi_passwd;
    Button nextButton;
    CheckBox showpasswd;
    WifiManager wifiManager;
    SharedPreferences wifiPreferences;
    Monitor monitor;

    public AddWifiActivityFragment() {

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

    private void nextPage() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.getActivity().unregisterReceiver(monitor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        wifiManager = (WifiManager) this.getActivity().getSystemService(Context.WIFI_SERVICE);
        wifiPreferences = getActivity().getSharedPreferences("WifiPassword", Context.MODE_PRIVATE);
        monitor = new Monitor();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.getActivity().registerReceiver(monitor, filter);

        View rootView = inflater.inflate(R.layout.fragment_add_wifi, container, false);
        wifi_ssid = (EditText) rootView.findViewById(R.id.wifi_ssid);
        wifi_passwd = (EditText) rootView.findViewById(R.id.wifi_password);
        nextButton = (Button) rootView.findViewById(R.id.NextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextPage();
            }
        });
        showpasswd = (CheckBox) rootView.findViewById(R.id.showPassword);
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
        return rootView;
    }
}

package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

import com.ozner.WaterPurifier.WaterPurifier;
import com.ozner.application.OznerBLEService;
import com.ozner.device.OznerDeviceManager;
import com.ozner.ui.library.RoundDrawable;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    private static final int WifiActivityRequestCode = 0x100;
    final Monitor mMonitor = new Monitor();
    ListView listView;
    DeviceListAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BitmapDrawable drawable=(BitmapDrawable)this.getResources().getDrawable(R.drawable.user1);
        RoundDrawable icon=new RoundDrawable(this);
        icon.setBitmap(drawable.getBitmap());
        icon.setText("净水家测试");
        Toolbar toolbar=  (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(icon);
        findViewById(R.id.addWifiButton).setOnClickListener(this);
        adapter = new DeviceListAdapter(this, WaterPurifier.class);
        listView = (ListView) findViewById(R.id.devicesList);
        listView.setAdapter(adapter);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OznerApplication.ACTION_ServiceInit);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_ADD);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_CHANGE);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_REMOVE);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_OWNER_CHANGE);
        this.registerReceiver(mMonitor, intentFilter);
    }


    private OznerBLEService.OznerBLEBinder getService() {
        OznerBaseApplication app = (OznerBaseApplication) getApplication();
        return app.getService();
    }

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter.reload();
        }
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mMonitor);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        adapter.reload();
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case WifiActivityRequestCode:
                if (requestCode == RESULT_OK) {

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addWifiButton:
                Intent intent = new Intent(this, WifiConfigurationActivity.class);
                startActivityForResult(intent, WifiActivityRequestCode);
                break;
        }
    }
}

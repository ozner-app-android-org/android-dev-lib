package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.ozner.AirPurifier.AirPurifier_Bluetooth;
import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.MusicCap.MusicCap;
import com.ozner.WaterPurifier.WaterPurifier;
import com.ozner.WaterReplenishmentMeter.WaterReplenishmentMeter;
import com.ozner.application.OznerBLEService;
import com.ozner.cup.Cup;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.kettle.Kettle;
import com.ozner.tap.Tap;
import com.ozner.ui.library.RoundDrawable;

import ozner.xzy.test.qcode.QCodeActivity;

public class MainActivity extends ActionBarActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private static final int WifiActivityRequestCode = 0x100;
    final Monitor mMonitor = new Monitor();
    ListView listView;
    MyAdapter adapter;

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        OznerDevice device=adapter.getItem(i);
        if (device instanceof Tap)
        {
            Intent intent=new Intent(this,TapActivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent, 0);
            return;
        }

        if (device instanceof Cup)
        {
            Intent intent=new Intent(this,CupActivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent,0);
            return;
        }

        if (device instanceof AirPurifier_MXChip)
        {
            Intent intent=new Intent(this,AirPurifierAcivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent,0);
            return;
        }
        if (device instanceof AirPurifier_Bluetooth)
        {
            Intent intent=new Intent(this,BluetoothAirPurifierActivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent,0);
            return;
        }
        if (device instanceof WaterPurifier)
        {
            Intent intent=new Intent(this,WaterPurifierActivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent,0);
            return;
        }
        if (device instanceof WaterReplenishmentMeter)
        {
            Intent intent=new Intent(this,WaterReplenishmentMeterActivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent,0);
            return;
        }
        if (device instanceof MusicCap)
        {
            Intent intent=new Intent(this,MusicCapActivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent,0);
            return;
        }
        if (device instanceof Kettle)
        {
            Intent intent=new Intent(this,MusicCapActivity.class);
            intent.putExtra("Address", device.Address());
            startActivityForResult(intent,0);
            return;
        }
    }

    class MyAdapter extends ArrayAdapter<OznerDevice>
    {
        LayoutInflater layoutInflater;
        public MyAdapter(Context context) {
            super(context, R.layout.list_item_device);
            layoutInflater=LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DeviceItemView view=(DeviceItemView)convertView;
            if (view==null)
            {
                view=(DeviceItemView)layoutInflater.inflate(R.layout.list_item_device,null);
            }
            OznerDevice device=getItem(position);
            view.loadDevice(device);

            return view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter=new MyAdapter(this);

        BitmapDrawable drawable = (BitmapDrawable) this.getResources().getDrawable(R.drawable.user1);
        RoundDrawable icon = new RoundDrawable(this);
        icon.setBitmap(drawable.getBitmap());
        icon.setText("净水家测试");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(icon);
        FloatingActionButton wifi = (FloatingActionButton) findViewById(R.id.addWifiButton);
        wifi.setIcon(R.drawable.ic_settings_wifi);
        wifi.setOnClickListener(this);
        FloatingActionButton blue = (FloatingActionButton) findViewById(R.id.addBluetoothButton);
        blue.setIcon(R.drawable.ic_settings_bluetooth);
        blue.setOnClickListener(this);
        FloatingActionButton ui = (FloatingActionButton) findViewById(R.id.addUIButton);
        ui.setIcon(R.drawable.ic_close_press);
        ui.setOnClickListener(this);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(OznerApplication.ACTION_ServiceInit);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_ADD);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_CHANGE);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_DEVICE_REMOVE);
        intentFilter.addAction(OznerDeviceManager.ACTION_OZNER_MANAGER_OWNER_CHANGE);
        this.registerReceiver(mMonitor, intentFilter);
        listView = (ListView) findViewById(R.id.devicesList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

    }


    private OznerBLEService.OznerBLEBinder getService() {
        OznerBaseApplication app = (OznerBaseApplication) getApplication();
        return app.getService();
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mMonitor);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        load();



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addWifiButton: {
//                BaseDeviceIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager().createNewIO("MXCHIP_HAOZE_Water", "C8:93:46:C0:13:D7", "MXCHIP_HAOZE_Water");
//                OznerDevice device = null;
//                try {
//                    device = OznerDeviceManager.Instance().getDevice(io);
//                    OznerDeviceManager.Instance().save(device);
//                } catch (NotSupportDeviceException e) {
//                    e.printStackTrace();
//                }
                Intent intent = new Intent(this, WifiConfigurationActivity.class);
                startActivityForResult(intent, WifiActivityRequestCode);
                break;
            }
            case R.id.addBluetoothButton: {
                Intent intent = new Intent(this, AddDeviceActivity.class);
                startActivityForResult(intent, 0);
                break;
            }
            case R.id.addUIButton: {
                Intent intent = new Intent(this, QCodeActivity.class);
                startActivityForResult(intent, 999);
                //intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                break;
                //Intent intent = new Intent(this, UIActivity.class);
                //startActivityForResult(intent, 0);
                //break;
            }
        }
    }
    private void load(){
        if (OznerDeviceManager.Instance()==null)
        {
            return;
        }

        if (OznerDeviceManager.Instance().getDevices()==null)
        {
            return;
        }
        adapter.clear();
        for (OznerDevice device : OznerDeviceManager.Instance().getDevices())
        {
            adapter.add(device);
        }
    }

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            load();
        }
    }
}

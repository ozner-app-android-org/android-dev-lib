package ozner.xzy.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.FirmwareTools;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.kettle.Kettle;
import com.ozner.tap.Tap;
import com.ozner.util.GetPathFromUri4kitkat;
import com.ozner.util.dbg;

import java.text.SimpleDateFormat;
import java.util.Date;

public class KettleSetupActivity extends Activity  {
    Kettle kettle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_kettle_setup);
        kettle =(Kettle) OznerDeviceManager.Instance().getDevice(getIntent().getStringExtra("Address"));
        if (kettle == null)
            return;

        load();
        super.onCreate(savedInstanceState);
    }

    private void load() {

    }





}

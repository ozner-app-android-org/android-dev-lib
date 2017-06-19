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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.ozner.bluetooth.BluetoothIO;
import com.ozner.device.BaseDeviceIO;
import com.ozner.device.FirmwareTools;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.kettle.Kettle;
import com.ozner.kettle.KettleSetting;
import com.ozner.kettle.PreservationMode;
import com.ozner.tap.Tap;
import com.ozner.util.GetPathFromUri4kitkat;
import com.ozner.util.dbg;

import java.text.SimpleDateFormat;
import java.util.Date;

public class KettleSetupActivity extends Activity {
    Kettle kettle;
    EditText reservationTime;
    EditText boilingTemperature;
    EditText preservationTime;
    EditText preservationTemperature;
    RadioButton heating;
    RadioButton boiling;
    RadioButton reservationOn;
    RadioButton reservationOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_kettle_setup);
        kettle = (Kettle) OznerDeviceManager.Instance().getDevice(getIntent().getStringExtra("Address"));
        if (kettle == null)
            return;

        this.findViewById(R.id.Submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KettleSetting setting = (KettleSetting) kettle.Setting();

                int iReservationTime = 0;
                int iboilingTemperature = 0;
                int ipreservationTime = 0;
                int ipreservationTemperature = 0;
                try {
                    iReservationTime = Integer.parseInt(reservationTime.getText().toString());
                    if ((iReservationTime < 0) || (iReservationTime > 24 * 60))
                        throw new NumberFormatException();

                } catch (NumberFormatException e) {
                    Toast toast = Toast.makeText(KettleSetupActivity.this, "错误的预约时间", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                try {
                    iboilingTemperature = Integer.parseInt(boilingTemperature.getText().toString());
                    if ((iboilingTemperature < 0) || (iboilingTemperature > 100))
                        throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    Toast toast = Toast.makeText(KettleSetupActivity.this, "错误的煮沸温度", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                try {
                    ipreservationTime = Integer.parseInt(preservationTime.getText().toString());
                    if ((ipreservationTime < 0) || (ipreservationTime > 24 * 60))
                        throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    Toast toast = Toast.makeText(KettleSetupActivity.this, "错误的保温时间", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                try {
                    ipreservationTemperature = Integer.parseInt(preservationTemperature.getText().toString());
                    if ((ipreservationTemperature < 0) || (ipreservationTemperature > 100))
                        throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    Toast toast = Toast.makeText(KettleSetupActivity.this, "错误的保温温度", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (!(reservationOn.isChecked() | reservationOff.isChecked()))
                {
                    Toast toast = Toast.makeText(KettleSetupActivity.this, "没有设置预约开关", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (reservationOn.isChecked())
                    setting.reservationEnable(true);
                else
                    setting.reservationEnable(false);

                if (!(heating.isChecked() | boiling.isChecked()))
                {
                    Toast toast = Toast.makeText(KettleSetupActivity.this, "没有设置保温模式", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                setting.reservationEnable(reservationOn.isChecked());
                setting.reservationTime(iReservationTime);
                setting.boilingTemperature(iboilingTemperature);
                if (heating.isChecked())
                    setting.preservationMode(PreservationMode.heating);
                if (boiling.isChecked())
                    setting.preservationMode(PreservationMode.boiling);
                setting.preservationTime(ipreservationTime);
                setting.preservationTemperature(ipreservationTemperature);
                OznerDeviceManager.Instance().save(kettle);
            }
        });
        reservationTime = (EditText) findViewById(R.id.reservationTime);
        boilingTemperature = (EditText) findViewById(R.id.boilingTemperature);
        preservationTime = (EditText) findViewById(R.id.preservationTime);
        preservationTemperature = (EditText) findViewById(R.id.preservationTemperature);
        heating = (RadioButton) findViewById(R.id.heating);
        boiling = (RadioButton) findViewById(R.id.boiling);
        reservationOn = (RadioButton) findViewById(R.id.reservationOn);
        reservationOff = (RadioButton) findViewById(R.id.reservationOff);
        load();
        super.onCreate(savedInstanceState);
    }

    private void load() {
        reservationTime.setText(String.valueOf(kettle.getSetting().reservationTime()));
        boilingTemperature.setText(String.valueOf(kettle.getSetting().boilingTemperature()));
        preservationTime.setText(String.valueOf(kettle.getSetting().preservationTime()));
        preservationTemperature.setText(String.valueOf(kettle.getSetting().preservationTemperature()));

        if (kettle.getSetting().preservationMode() == PreservationMode.heating) {
            heating.setChecked(true);
        }
        if (kettle.getSetting().preservationMode() == PreservationMode.boiling) {
            boiling.setChecked(true);
        }
        if (kettle.status().isLoaded) {
            if (kettle.status().reservation) {
                reservationOn.setChecked(true);
            } else {
                reservationOff.setChecked(true);
            }
        }
    }
}

package ozner.xzy.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;

import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.device.OznerDeviceManager;

import ozner.xzy.test.R.id;

@SuppressLint("SimpleDateFormat")
public class AirPurifierSetupActivity extends Activity{
    AirPurifier_MXChip airPurifier = null;
    Button powerTimerOff;
    Button powerTimerOn;
    CheckBox powerTimerEnable;
    EditText name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_air_purifier_setup_acivity);

        String address = getIntent().getStringExtra("Address");
        airPurifier =(AirPurifier_MXChip) OznerDeviceManager.Instance().getDevice(address);
        name=(EditText)findViewById(id.name);
        name.setText(airPurifier.getName());

        powerTimerOn=(Button)findViewById(id.powerTimerOn);
        powerTimerOff=(Button)findViewById(id.powerTimerOff);
        powerTimerOn.setOnClickListener(onClickListener);
        powerTimerOff.setOnClickListener(onClickListener);

        powerTimerEnable = (CheckBox) findViewById(R.id.powerTimerEnable);
        powerTimerEnable.setOnClickListener(onClickListener);
        powerTimerEnable.setChecked(airPurifier.PowerTimer().Enable);

        powerTimerOff.setText(String.format("%02d:%02d", airPurifier.PowerTimer().PowerOffTime / 60,
                airPurifier.PowerTimer().PowerOffTime % 60));

        powerTimerOn.setText(String.format("%02d:%02d", airPurifier.PowerTimer().PowerOnTime / 60,
                airPurifier.PowerTimer().PowerOnTime % 60));

        findViewById(id.save).setOnClickListener(onClickListener);

    }
    private void showErrorMessage(String message) {
        new AlertDialog.Builder(this).setTitle("错误").setMessage(message)
                .setPositiveButton("确定", null).show();
    }
    View.OnClickListener onClickListener=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId())
            {
                case R.id.powerTimerOn: {
                    final TimePicker picker = new TimePicker(AirPurifierSetupActivity.this);
                    picker.setIs24HourView(true);
                    short time = airPurifier.PowerTimer().PowerOnTime;
                    int hour = time / 60;
                    int min = time % 60;

                    picker.setCurrentHour(hour);
                    picker.setCurrentMinute(min);

                    new android.support.v7.app.AlertDialog.Builder(AirPurifierSetupActivity.this).setTitle("").setView(picker)
                            .setPositiveButton("确定", new android.support.v7.app.AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    airPurifier.PowerTimer().PowerOnTime =
                                            (short) (picker.getCurrentHour() * 60 + picker.getCurrentMinute());
                                    String text = String.format("%02d:%02d",
                                            picker.getCurrentHour(), picker.getCurrentMinute());
                                    powerTimerOn.setText(text);
                                }
                            }).show();
                }
                break;

                case R.id.powerTimerOff: {
                    final TimePicker picker = new TimePicker(AirPurifierSetupActivity.this);
                    picker.setIs24HourView(true);
                    short time = airPurifier.PowerTimer().PowerOffTime;
                    int hour = time / 60;
                    int min = time % 60;

                    picker.setCurrentHour(hour);
                    picker.setCurrentMinute(min);

                    new android.support.v7.app.AlertDialog.Builder(AirPurifierSetupActivity.this).setTitle("").setView(picker)
                            .setPositiveButton("确定", new android.support.v7.app.AlertDialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    airPurifier.PowerTimer().PowerOffTime =
                                            (short) (picker.getCurrentHour() * 60 + picker.getCurrentMinute());

                                    String text = String.format("%02d:%02d",
                                            picker.getCurrentHour(), picker.getCurrentMinute());
                                    powerTimerOff.setText(text);
                                }
                            }).show();
                }
                break;
                case R.id.powerTimerEnable: {
                    airPurifier.PowerTimer().Enable = powerTimerEnable.isChecked();
                    break;
                }

                case id.save:
                {
                    String text = name.getText().toString();
                    if (text.isEmpty()) {
                        showErrorMessage("名称不能为空");
                        return;
                    }
                    airPurifier.Setting().name(text);
                    OznerDeviceManager.Instance().save(airPurifier);
                    finish();
                    break;
                }
            }
        }
    };
}

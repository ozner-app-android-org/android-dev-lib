package com.ozner.waterguardian;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, CheckBox.OnCheckedChangeListener {

    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;
    BluetoothDevice blueDevice;
    TextView statusView;
    BluetoothCallbackIMP bluetoothCallback = new BluetoothCallbackIMP();
    BluetoothGatt bluetoothGatt;
    boolean isConnected = false;
    byte[] firmware_bytes=null;
    int firmware_checksum=0;
    int firmware_size=0;
    int firmware_send=0;
    private void updateFirmware() {
        if (!isConnected) {
            Toast toast = Toast.makeText(MainActivity.this, "设备没有连接", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    1111);

        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1111) {
            if (data != null) {
                String path = GetPathFromUri4kitkat.getPath(this, data.getData());
                Toast.makeText(this, path, Toast.LENGTH_LONG).show();
                load_firmware(path);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void load_firmware(String path)
    {
        try {
            firmware_send=0;
            File file = new File(path);
            int Size = (int) file.length();
            if (Size > 127 * 1024){
                Toast toast=new Toast(this);
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setText("文件太大");
                toast.show();
            }

            if ((Size % 256) != 0) {
                Size = (Size / 256) * 256 + 256;
            }
            byte[] bytes = new byte[Size];
            Arrays.fill(bytes,(byte)0xff);
            int sum=0;
            FileInputStream fs = new FileInputStream(path);
            try {

                fs.read(bytes, 0, (int) file.length());
                long temp = 0;
                int len = Size / 4;
                for (int i = 0; i < len; i++) {
                    temp += ByteUtil.getUInt(bytes, i * 4);
                }
                long TempMask = 0x1FFFFFFFFL;
                TempMask -= 0x100000000L;
                sum = (int) (temp & TempMask);
            } finally {
                fs.close();
            }
            firmware_bytes=bytes;
            firmware_checksum=sum;
            firmware_size=Size;
            bluetoothCallback.eraseMCU();
        }catch (IOException e)
        {
            return;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((CheckBox) findViewById(R.id.fl1_check)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.fl2_check)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.fl3_check)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.fl4_check)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.fl5_check)).setOnCheckedChangeListener(this);
        findViewById(R.id.fl1_time).setOnClickListener(this);
        findViewById(R.id.fl2_time).setOnClickListener(this);
        findViewById(R.id.fl3_time).setOnClickListener(this);
        findViewById(R.id.fl4_time).setOnClickListener(this);
        findViewById(R.id.fl5_time).setOnClickListener(this);
        findViewById(R.id.fl1_max_time).setOnClickListener(this);
        findViewById(R.id.fl2_max_time).setOnClickListener(this);
        findViewById(R.id.fl3_max_time).setOnClickListener(this);
        findViewById(R.id.fl4_max_time).setOnClickListener(this);
        findViewById(R.id.fl5_max_time).setOnClickListener(this);

        findViewById(R.id.fl1_max_vol).setOnClickListener(this);
        findViewById(R.id.fl2_max_vol).setOnClickListener(this);
        findViewById(R.id.fl3_max_vol).setOnClickListener(this);
        findViewById(R.id.fl4_max_vol).setOnClickListener(this);
        findViewById(R.id.fl5_max_vol).setOnClickListener(this);


        findViewById(R.id.findDeviceButton).setOnClickListener(this);
        findViewById(R.id.writeButton).setOnClickListener(this);
        findViewById(R.id.readFilter).setOnClickListener(this);
        findViewById(R.id.readSensor).setOnClickListener(this);
        findViewById(R.id.otaButton).setOnClickListener(this);

        findViewById(R.id.disconnect).setOnClickListener(this);


        statusView = (TextView) findViewById(R.id.statusView);
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 0);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.fl1_check:
            case R.id.fl2_check:
            case R.id.fl3_check:
            case R.id.fl4_check:
            case R.id.fl5_check: {
                //获取点击的CheckView所在的panel索引位置，然后位置加1，获取到下面的panel
                LinearLayout layout = (LinearLayout) buttonView.getParent();
                int p = layout.indexOfChild(buttonView) + 1;
                LinearLayout panel = (LinearLayout) layout.getChildAt(p);
                for (int x = 0; x < panel.getChildCount(); x++) {
                    View view = panel.getChildAt(x);
                    if (view instanceof ViewGroup) {
                        ViewGroup viewGroup = (ViewGroup) view;
                        for (int y = 0; y < viewGroup.getChildCount(); y++) {
                            viewGroup.getChildAt(y).setEnabled(isChecked);
                        }
                    }
                }

            }
            break;
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.findDeviceButton:
                onFindDeviceClick();
                break;
            case R.id.writeButton:
                bluetoothCallback.writeInfo();
                break;
            case R.id.fl1_time:
            case R.id.fl2_time:
            case R.id.fl3_time:
            case R.id.fl4_time:
            case R.id.fl5_time: {
                selectTime((Button) v);
                break;
            }
            case R.id.fl1_max_time:
            case R.id.fl2_max_time:
            case R.id.fl3_max_time:
            case R.id.fl4_max_time:
            case R.id.fl5_max_time: {
                selectMaxTime((Button) v);
                break;
            }
            case R.id.fl1_max_vol:
            case R.id.fl2_max_vol:
            case R.id.fl3_max_vol:
            case R.id.fl4_max_vol:
            case R.id.fl5_max_vol: {
                selectMaxVol((Button) v);
                break;
            }
            case R.id.readFilter: {
                bluetoothCallback.lastReadFilterIndex = 0;
                bluetoothCallback.readFilter(0);
            }
            break;
            case R.id.readSensor: {
                bluetoothCallback.readSensor();
            }
            break;
            case R.id.disconnect: {
                if (bluetoothGatt != null) {
                    bluetoothGatt.disconnect();
                    bluetoothGatt.close();
                }
                blueDevice = null;
            }
            break;
            case  R.id.otaButton:
            {
                updateFirmware();
            }
            break;
        }
    }

    private int getInt(int id, String repStr) {
        String str = ((Button) findViewById(id)).getText().toString().replace(repStr, "");
        return Integer.parseInt(str);
    }


    void selectTime(final Button sender) {
        final DatePicker picker = new DatePicker(this);
        new android.support.v7.app.AlertDialog.Builder(this).setTitle("").setView(picker)
                .setPositiveButton("确定", new android.support.v7.app.AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        sender.setText(String.format("%d-%02d-%02d 00:00:00", picker.getYear(), picker.getMonth(), picker.getDayOfMonth()));
                    }
                }).show();

    }

    void selectMaxTime(final Button sender) {
        final TextInputEditText picker = new TextInputEditText(this);
        picker.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        picker.setText(sender.getText().toString().replace("分钟", ""));
        new android.support.v7.app.AlertDialog.Builder(this).setTitle("").setView(picker)
                .setPositiveButton("确定", new android.support.v7.app.AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int val = 0;
                        try {
                            val = Integer.parseInt(picker.getText().toString());
                        } catch (NumberFormatException e) {
                            val = 0;
                        }
                        sender.setText(String.format("%d分钟", val));
                    }
                }).show();

    }

    void selectMaxVol(final Button sender) {
        final TextInputEditText picker = new TextInputEditText(this);
        picker.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
        picker.setText(sender.getText().toString().replace("升", ""));
        new android.support.v7.app.AlertDialog.Builder(this).setTitle("").setView(picker)
                .setPositiveButton("确定", new android.support.v7.app.AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int val = 0;
                        try {
                            val = Integer.parseInt(picker.getText().toString());
                        } catch (NumberFormatException e) {
                            val = 0;
                        }
                        sender.setText(String.format("%d升", val));
                    }
                }).show();

    }

    void onFindDeviceClick() {

        if (!bluetoothAdapter.isEnabled()) {
            Toast toast = Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_SHORT);
            toast.show();
            return;
        } else {
            Dialog_Bluetooth dialog = new Dialog_Bluetooth(this);
            dialog.show();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    BluetoothDevice device = ((Dialog_Bluetooth) dialog).getSelectedDevice();
                    if (device != null) {
                        connectDevice(device);
                    }
                }
            });

        }
    }



    void connectDevice(BluetoothDevice device) {

        blueDevice = device;
        updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "等待连接"));
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        device.connectGatt(this, false, bluetoothCallback);

    }

    static UUID GetUUID(int id) {
        return UUID.fromString(String.format(
                "%1$08x-0000-1000-8000-00805f9b34fb", id));
    }

    Handler handler = new Handler();

    private void updateStatusText(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                statusView.setText(text);
            }
        });
    }

    class SensorInfo {
        public int tds_in;
        public int tds_in_raw;
        public int tds_out;
        public int tds_out_raw;
        public int vol;

        public void loadFromBytes(byte[] bytes, int index) {
            tds_in_raw = ByteUtil.getShort(bytes, index);
            tds_in = ByteUtil.getShort(bytes, index + 2);
            tds_out_raw = ByteUtil.getShort(bytes, index + 4);
            tds_out = ByteUtil.getShort(bytes, index + 6);
            vol = ByteUtil.getShort(bytes, index + 8);
        }

    }

    class FilterInfo {
        public byte index;
        public byte rev;

        public long time;
        public int workTime;
        public int maxTime;
        public int maxVol;

        public void loadFromBytes(byte[] bytes, int index) {
            this.index = bytes[index];
            rev = bytes[index + 1];
            time = ByteUtil.getInt(bytes, index + 2);
            workTime = ByteUtil.getInt(bytes, index + 6);
            maxTime = ByteUtil.getInt(bytes, index + 10);
            maxVol = ByteUtil.getInt(bytes, index + 14);

        }

        public void toBytes(byte[] bytes, int startIndex) {
            bytes[startIndex] = index;
            bytes[startIndex + 1] = 0;
            ByteUtil.putInt(bytes, (int) time, startIndex + 2);
            ByteUtil.putInt(bytes, 0, startIndex + 6);
            ByteUtil.putInt(bytes, maxTime, startIndex + 10);
            ByteUtil.putInt(bytes, maxVol, startIndex + 14);
        }

    }

    FilterInfo[] filterInfo = new FilterInfo[5];

    class BluetoothCallbackIMP extends BluetoothGattCallback {
        private static final int ServiceId = 0xFFF0;
        BluetoothGattCharacteristic mInput = null;
        BluetoothGattCharacteristic mOutput = null;
        BluetoothGattService mService = null;

        final UUID Characteristic_Input = GetUUID(0xFFF2);
        final UUID Characteristic_Output = GetUUID(0xFFF1);
        final UUID GATT_CLIENT_CHAR_CFG_UUID = GetUUID(0x2902);

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            bluetoothGatt = gatt;
            if (gatt.getDevice() != blueDevice) return;
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTING:
                    updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "连接中"));
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "查找服务"));
                    gatt.discoverServices();
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    isConnected = false;
                    updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "断开中"));
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    isConnected = false;
                    updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "已断开"));
                    break;
            }
        }

        private void writeFilter(int index,int fl_check, int fl_time, int fl_max_time, int fl_max_vol) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //判断是否要设置这个滤芯
            if (((CheckBox) findViewById(fl_check)).isChecked()) {
                FilterInfo filterInfo = new FilterInfo();
                filterInfo.index = (byte)index;
                try {
                    Date time = fmt.parse(((Button) findViewById(fl_time)).getText().toString());
                    filterInfo.time = (int) (time.getTime() / 1000);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                filterInfo.maxTime = getInt(fl_max_time, "分钟");
                filterInfo.maxVol = getInt(fl_max_vol, "升");

                updateStatusText(String.format("%s(写入滤芯%d)", blueDevice.getAddress(), filterInfo.index));
                byte[] data = new byte[19];
                data[0] = 0x12;
                filterInfo.toBytes(data, 1);
                mInput.setValue(data);
                if (!bluetoothGatt.writeCharacteristic(mInput)) {
                    updateStatusText(String.format("%s(写入滤芯%d错误)", blueDevice.getAddress(), filterInfo.index));
                    return;
                }
                //延时防止写入太快设备收不到
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        void writeInfo() {
            //判断蓝牙连接
            if (bluetoothGatt == null) return;
            if (!isConnected) {
                Toast toast = Toast.makeText(MainActivity.this, "设备没有连接", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            writeFilter(0,R.id.fl1_check, R.id.fl1_time, R.id.fl1_max_time, R.id.fl1_max_vol);
            writeFilter(1,R.id.fl2_check, R.id.fl2_time, R.id.fl2_max_time, R.id.fl2_max_vol);
            writeFilter(2,R.id.fl3_check, R.id.fl3_time, R.id.fl3_max_time, R.id.fl3_max_vol);
            writeFilter(3,R.id.fl4_check, R.id.fl4_time, R.id.fl4_max_time, R.id.fl4_max_vol);
            writeFilter(4,R.id.fl5_check, R.id.fl5_time, R.id.fl5_max_time, R.id.fl5_max_vol);
        }

        private void readSensor() {
            if (bluetoothGatt == null) return;
            if (!isConnected) {
                Toast toast = Toast.makeText(MainActivity.this, "设备没有连接", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            updateStatusText(String.format("%s(读取传感器)", blueDevice.getAddress()));
            mInput.setValue(new byte[]{0x10});

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bluetoothGatt.writeCharacteristic(mInput);
        }

        private void readFilter(int index) {
            if (bluetoothGatt == null) return;
            if (!isConnected) {
                Toast toast = Toast.makeText(MainActivity.this, "设备没有连接", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            updateStatusText(String.format("%s(读取滤芯:%d)", blueDevice.getAddress(), index));
            mInput.setValue(new byte[]{0x11, (byte) index});

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bluetoothGatt.writeCharacteristic(mInput);
        }
        private void eraseMCU()
        {
            if (bluetoothGatt == null) return;
            if (!isConnected) {
                Toast toast = Toast.makeText(MainActivity.this, "设备没有连接", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
            updateStatusText("擦除mcu");
            mInput.setValue(new byte[]{(byte)0xC2, 0});
            bluetoothGatt.writeCharacteristic(mInput);
        }

        private void loadFilter() {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (FilterInfo fi : filterInfo) {
                if (fi != null)
                    switch (fi.index) {
                        case 0: {
                            ((CheckBox) findViewById(R.id.fl1_check)).setChecked(fi.time != 0);
                            ((Button) findViewById(R.id.fl1_time)).setText(fmt.format(new Date(fi.time * 1000)));
                            ((TextView) findViewById(R.id.fl1_work_time)).setText(String.format("%d分钟", fi.workTime));
                            ((Button) findViewById(R.id.fl1_max_time)).setText(String.format("%d分钟", fi.maxTime));
                            ((Button) findViewById(R.id.fl1_max_vol)).setText(String.format("%d升", fi.maxVol));
                        }
                        break;
                        case 1: {
                            ((CheckBox) findViewById(R.id.fl2_check)).setChecked(fi.time != 0);
                            ((Button) findViewById(R.id.fl2_time)).setText(fmt.format(new Date(fi.time * 1000)));
                            ((TextView) findViewById(R.id.fl2_work_time)).setText(String.format("%d分钟", fi.workTime));
                            ((Button) findViewById(R.id.fl2_max_time)).setText(String.format("%d分钟", fi.maxTime));
                            ((Button) findViewById(R.id.fl2_max_vol)).setText(String.format("%d升", fi.maxVol));
                        }
                        break;
                        case 2: {
                            ((CheckBox) findViewById(R.id.fl3_check)).setChecked(fi.time != 0);
                            ((Button) findViewById(R.id.fl3_time)).setText(fmt.format(new Date(fi.time * 1000)));
                            ((TextView) findViewById(R.id.fl3_work_time)).setText(String.format("%d分钟", fi.workTime));
                            ((Button) findViewById(R.id.fl3_max_time)).setText(String.format("%d分钟", fi.maxTime));
                            ((Button) findViewById(R.id.fl3_max_vol)).setText(String.format("%d升", fi.maxVol));
                        }
                        break;
                        case 3: {
                            ((CheckBox) findViewById(R.id.fl4_check)).setChecked(fi.time != 0);
                            ((Button) findViewById(R.id.fl4_time)).setText(fmt.format(new Date(fi.time * 1000)));
                            ((TextView) findViewById(R.id.fl4_work_time)).setText(String.format("%d分钟", fi.workTime));
                            ((Button) findViewById(R.id.fl4_max_time)).setText(String.format("%d分钟", fi.maxTime));
                            ((Button) findViewById(R.id.fl4_max_vol)).setText(String.format("%d升", fi.maxVol));
                        }
                        break;
                        case 4: {
                            ((CheckBox) findViewById(R.id.fl5_check)).setChecked(fi.time != 0);
                            ((Button) findViewById(R.id.fl5_time)).setText(fmt.format(new Date(fi.time * 1000)));
                            ((TextView) findViewById(R.id.fl5_work_time)).setText(String.format("%d分钟", fi.workTime));
                            ((Button) findViewById(R.id.fl5_max_time)).setText(String.format("%d分钟", fi.maxTime));
                            ((Button) findViewById(R.id.fl5_max_vol)).setText(String.format("%d升", fi.maxVol));
                        }
                        break;

                    }
            }
        }

        private void sendFirmware()
        {
            byte[] bytes=new byte[20];
            int pg=firmware_send/16;
            int size=firmware_size-firmware_send;
            size=(size>=16?16:size);

            bytes[0]=(byte)0xC1;
            ByteUtil.putShort(bytes,(short)pg,1);
            bytes[3]=(byte)size;
            System.arraycopy(firmware_bytes,firmware_send,bytes,4,size);
            mInput.setValue(bytes);
            updateStatusText(String.format("发送固件:%d/%d",firmware_send,firmware_size));
            bluetoothGatt.writeCharacteristic(mInput);
        }
        private void sendUpdate()
        {
            byte[] bytes=new byte[9];
            bytes[0]=(byte)0xc3;
            ByteUtil.putInt(bytes,firmware_size,1);
            ByteUtil.putInt(bytes,firmware_checksum,5);
            mInput.setValue(bytes);
            updateStatusText(String.format("固件大小:%d,checksum:H%X",firmware_send,firmware_checksum));
            bluetoothGatt.writeCharacteristic(mInput);
        }
        Handler delayHandler=new Handler();
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status==0)
            {
                byte[] bytes=characteristic.getValue();
                if (bytes.length>0) {
                    switch (bytes[0]) {
                        case (byte)0xC2:
                        {

                            delayHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendFirmware();
                                }
                            },1000);//擦除mcu ，1秒以后开发发送固件
                        }
                        break;
                        case (byte)0xC1:
                        {
                            firmware_send += bytes[3];
                            if (firmware_send < firmware_size) {
                                sendFirmware();
                            }else
                            {
                                delayHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        sendUpdate();
                                    }
                                },1000);//固件发送完成 ，1秒以后开始升级
                            }
                        }
                        break;
                    }
                }
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        int lastReadFilterIndex = 0;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] bytes = characteristic.getValue();
            if (bytes.length > 0) {
                switch (bytes[0]) {
                    case (byte) 0xa0: {
                        final SensorInfo sensorInfo = new SensorInfo();
                        sensorInfo.loadFromBytes(bytes, 1);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) findViewById(R.id.sensorInfo)).setText(
                                        String.format("TDS 进水:%d 原始:%d 出水:%d 原始:%d\n水流量:%d升",
                                                sensorInfo.tds_in, sensorInfo.tds_in_raw,
                                                sensorInfo.tds_out, sensorInfo.tds_out_raw, sensorInfo.vol)
                                );
                            }
                        });
                        readFilter(0);
                    }
                    break;
                    //返回滤芯信息
                    case (byte) 0xa1:
                        FilterInfo fi = new FilterInfo();
                        fi.loadFromBytes(bytes, 1);
                        if (fi.index >= 0 && fi.index < 5) {
                            filterInfo[fi.index] = fi;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                loadFilter();
                            }
                        });
                        lastReadFilterIndex++;
                        if (lastReadFilterIndex < 5) {
                            readFilter(lastReadFilterIndex);
                        } else
                            lastReadFilterIndex = 0;
                        break;
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (gatt.getDevice() != blueDevice) return;
            updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "设置通知成功"));
            isConnected = true;
            readSensor();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (gatt.getDevice() != blueDevice) return;
            updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "设置设备"));
            mService = gatt.getService(GetUUID(ServiceId));
            if (mService != null) {
                mInput = mService.getCharacteristic(Characteristic_Input);
                mOutput = mService.getCharacteristic(Characteristic_Output);
                mInput.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                BluetoothGattDescriptor desc = mOutput.getDescriptor(GATT_CLIENT_CHAR_CFG_UUID);
                if (desc != null) {
                    updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "设置通知"));
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.setCharacteristicNotification(mOutput, true);
                    if (!bluetoothGatt.writeDescriptor(desc)) {
                        updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "设置通知失败"));
                    }
                }
            } else {
                updateStatusText(String.format("%s(%s)", blueDevice.getAddress(), "未发现服务"));

            }
        }

    }
}



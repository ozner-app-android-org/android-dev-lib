package ozner.xzy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.ozner.application.OznerBLEService;
import com.ozner.device.OperateCallback;
import com.ozner.device.OznerDeviceManager;
import com.ozner.ui.library.RoundDrawable;
import com.ozner.wifi.mxchip.WaterPurifier.WaterPurifier;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    private static final int WifiActivityRequestCode = 0x100;
    final Monitor mMonitor = new Monitor();
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
        findViewById(R.id.power).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final WaterPurifier waterPurifier=(WaterPurifier)OznerDeviceManager.Instance().getDevice("C8:93:46:45:33:19");
                if (waterPurifier!=null)
                {
                    waterPurifier.setPower(!waterPurifier.Power(), new OperateCallback<Void>() {
                        @Override
                        public void onSuccess(Void var1) {
                            Handler handler=new Handler(getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "电源状态:"+(waterPurifier.Power()?"开":"关"),Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable var1) {
                            Handler handler=new Handler(getMainLooper());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "设置失败",Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private OznerBLEService.OznerBLEBinder getService() {
        OznerBaseApplication app = (OznerBaseApplication) getApplication();
        return app.getService();
    }

    class Monitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(OznerApplication.ACTION_ServiceInit)) {

            }

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

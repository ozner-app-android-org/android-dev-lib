package ozner.xzy.test;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dd.CircularProgressButton;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.ozner.ui.library.Screen;
import com.ozner.wifi.mxchip.MXWifiChip;
import com.ozner.wifi.mxchip.WifiDevice;

public class WifiActivateActivity extends AppCompatActivity {
    WifiDevice wifiDevice;
    CircleProgressBar progressBar;
    CircularProgressButton finishButton;
    LinearLayout logLayout;
    String deivceID;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler=new Handler();
        setContentView(R.layout.activity_wifi_activate);
        progressBar=(CircleProgressBar)findViewById(R.id.progress);
        logLayout=(LinearLayout)findViewById(R.id.textLogPanel);
        progressBar.setColorSchemeResources(android.R.color.holo_red_light);
        finishButton =(CircularProgressButton)findViewById(R.id.finishButton);
        wifiDevice=WifiDevice.loadByJSON(getIntent().getStringExtra("json"));
        addLog("开始激活:"+wifiDevice.localIP);

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finishButton.getProgress() == -1) {
                    finishButton.setEnabled(false);
                    active();
                } else {
                    Intent intent=new Intent();
                    intent.putExtra("DeviceID", deivceID);
                    setResult(0x100,intent);
                    finish();
                }
            }
        });

        active();
    }

    public void active()
    {
        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {

                MXWifiChip mxChip=new MXWifiChip(WifiActivateActivity.this);
                try {
                    deivceID=mxChip.ActiveDevice(wifiDevice);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            addLog("设备ID:"+deivceID);
                            progressBar.setVisibility(View.INVISIBLE);
                            finishButton.setProgress(100);
                            finishButton.setEnabled(true);
                        }
                    });
                }catch (Exception e)
                {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            addLog("网络错误");
                            progressBar.setVisibility(View.INVISIBLE);
                            finishButton.setProgress(-1);
                            finishButton.setEnabled(true);
                        }
                    });
                }
            }
        });
        thread.start();

    }

    private void addLog(String log)
    {
        TextView textView=new TextView(this);
        textView.setText(log);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textView.setTextSize(Screen.sp2px(this, 4.3f));
        textView.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
        LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity= Gravity.CENTER;
        logLayout.addView(textView,layoutParams);
    }



}

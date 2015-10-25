package ozner.xzy.test;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.dd.CircularProgressButton;
import com.ozner.ui.library.RoundDrawable;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

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

        Intent intent = new Intent(this, WifiConfigurationActivity.class);
        startActivity(intent);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addWifiButton:
                Intent intent = new Intent(this, WifiConfigurationActivity.class);
                startActivity(intent);
                break;
        }
    }
}

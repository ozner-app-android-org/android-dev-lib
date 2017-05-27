package ozner.xzy.test.qcode;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.android.FinishListener;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;
import com.ozner.device.NotSupportDeviceException;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;
import com.ozner.tap.Tap;
import com.ozner.tap.TapManager;
import com.ozner.wifi.mxchip.MXChipIO;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import ozner.xzy.test.R;

public class QCodeActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private CameraManager cameraManager;
    private ViewfinderView viewfinderView;
    private boolean hasSurface = false;
    private QCodeActivityHandler handler;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;
    private IntentSource source;

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        final String address = rawResult.getText();
        Toast toast = Toast.makeText(QCodeActivity.this, rawResult.getText(), Toast.LENGTH_LONG);
        toast.show();
        //^([0-9a-fA-F]{2})(([/\s:-][0-9a-fA-F]{2}){5})$

        if (address.matches("^([0-9a-fA-F]{2})(([0-9a-fA-F]{2}){5})$") || address.matches("^([0-9a-fA-F]{2})(([/\\s:][0-9a-fA-F]{2}){5})$") ) {
            String mac=address;
            if (address.indexOf(":")<=0)
            {
                mac = address.substring(0, 2) + ":" +
                        address.substring(2, 4) + ":" +
                        address.substring(4, 6) + ":" +
                        address.substring(6, 8) + ":" +
                        address.substring(8, 10) + ":" +
                        address.substring(10, 12);
            }
            final String macAddress=mac;

            new AlertDialog.Builder(this)
                    .setTitle("请选择")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setSingleChoiceItems(new String[]{"空净", "庆科水机", "智能杯", "水探头","电热壶"}, 0,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    OznerDevice device = null;
                                    switch (which) {
                                        case 0: {
                                            MXChipIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager().createMXChipDevice(macAddress, "FOG_HAOZE_AIR");
                                            io.name = "Air";
                                            try {
                                                device=OznerDeviceManager.Instance().getDevice(io);
                                            } catch (NotSupportDeviceException e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        }
                                        case 1: {
                                            MXChipIO io = OznerDeviceManager.Instance().ioManagerList().mxChipIOManager().createMXChipDevice(macAddress, "MXCHIP_HAOZE_Water");
                                            io.name = "Water";
                                            try {
                                                device=OznerDeviceManager.Instance().getDevice(io);

                                            } catch (NotSupportDeviceException e) {
                                                e.printStackTrace();
                                            }
                                            break;
                                        }
                                        case 2:
                                        {
                                            device= OznerDeviceManager.Instance().deviceManagers().cupManager().loadDevice(macAddress,"CP001","");
                                            break;
                                        }

                                        case 3:
                                        {
                                            device= OznerDeviceManager.Instance().deviceManagers().tapManager().loadDevice(macAddress,"SC001","");
                                            break;
                                        }
                                        case 4:
                                        {
                                            device= OznerDeviceManager.Instance().deviceManagers().kettleMgr().loadDevice(macAddress,"DRH001","");
                                            break;
                                        }
                                    }
                                    OznerDeviceManager.Instance().save(device);
                                    dialog.dismiss();

                                    QCodeActivity.this.setResult(RESULT_OK,null);
                                    QCodeActivity.this.finish();

                                }
                            }
                    )
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("错误")
                    .setMessage("扫描的条码不是一个设备MAC地址")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handler = new QCodeActivityHandler(QCodeActivity.this, decodeFormats, decodeHints, characterSet, cameraManager);

                            //QCodeActivity.this.finishActivity(RESULT_CANCELED);
                            dialog.dismiss();
                        }
                    })
                    .show();
        }

    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qcode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraManager = new CameraManager(getApplication());
        viewfinderView = (ViewfinderView) findViewById(com.google.zxing.R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);
        decodeFormats = null;
        characterSet = null;
        SurfaceView surfaceView = (SurfaceView) findViewById(com.google.zxing.R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(com.google.zxing.R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(com.google.zxing.R.string.app_name));
        builder.setMessage(getString(com.google.zxing.R.string.msg_camera_framework_bug));
        builder.setPositiveButton(com.google.zxing.R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new QCodeActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);

            }
            //decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            displayFrameworkBugMessageAndExit();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}

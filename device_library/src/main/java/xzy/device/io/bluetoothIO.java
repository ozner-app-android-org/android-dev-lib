package xzy.device.io;

import android.content.Context;

/**
 * Created by zhiyongxu on 2017/3/16.
 */

public class bluetoothIO extends deviceIO {
    Context context;
    public bluetoothIO(Context context)
    {
        this.context=context;
    }

    @Override
    public void open(deviceIOCallback callback) {

    }

    @Override
    public void close() {

    }

    @Override
    public void write(byte[] bytes) {

    }
}

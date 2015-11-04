package ozner.xzy.test;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.ozner.device.OznerDevice;

/**
 * Created by zhiyongxu on 15/11/4.
 */
public class BaseItemView extends LinearLayout {
    public BaseItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void loadDevice(OznerDevice device) throws ClassCastException {

    }

}

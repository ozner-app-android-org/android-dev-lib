package ozner.xzy.test;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.ozner.AirPurifier.AirPurifier_MXChip;
import com.ozner.WaterPurifier.WaterPurifier;
import com.ozner.device.OznerDevice;
import com.ozner.device.OznerDeviceManager;

import java.util.ArrayList;

/**
 * Created by zhiyongxu on 15/11/4.
 */
public class DeviceListAdapter extends BaseAdapter {
    final ArrayList<OznerDevice> devices = new ArrayList<>();
    Context context;
    LayoutInflater layoutInflater;
    Class<?> filterClass = null;

    public DeviceListAdapter(Context context, Class<?> filterClass) {
        this.filterClass = filterClass;
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }


    public void reload() {
        synchronized (devices) {
            devices.clear();
            if (OznerDeviceManager.Instance() == null) return;
            OznerDevice[] list = OznerDeviceManager.Instance().getDevices();
            if (list == null) return;
            for (OznerDevice device : list) {
                if (filterClass != null) {
                    if (device.getClass().equals(filterClass)) {
                        devices.add(device);
                    }
                } else
                    devices.add(device);
            }
        }
        this.notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        synchronized (devices) {
            return devices.size();
        }
    }

    @Override
    public Object getItem(int i) {
        synchronized (devices) {
            return devices.get(i);
        }
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }



    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        OznerDevice device = null;
        synchronized (devices) {
            device = devices.get(i);
        }

        BaseItemView item = (BaseItemView) view;
        if (item != null) {
            try {
                item.loadDevice(device);
            } catch (ClassCastException e) {
                item = null;
            }
        }
        if (device instanceof WaterPurifier) {
            item = (BaseItemView) layoutInflater.inflate(R.layout.water_purifier_item_view, null);
        }
        if (device instanceof AirPurifier_MXChip) {
            item = (BaseItemView) layoutInflater.inflate(R.layout.air_purifier_item_view, null);
        }
        if (item != null)
            item.loadDevice(device);
        return item;
    }
}

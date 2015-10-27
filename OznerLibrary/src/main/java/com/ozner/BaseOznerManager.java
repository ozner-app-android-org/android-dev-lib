package com.ozner;

import android.content.Context;

import com.ozner.device.OznerContext;
import com.ozner.util.SQLiteDB;
import com.ozner.util.dbg;

/**
 * Created by zhiyongxu on 15/10/26.
 */
public class BaseOznerManager {
    /**
     * 新增一个配对设备广播
     */
    public final static String ACTION_OZNER_MANAGER_DEVICE_ADD = "com.ozner.manager.device.add";
    /**
     * 删除设备广播
     */
    public final static String ACTION_OZNER_MANAGER_DEVICE_REMOVE = "com.ozner.manager.device.remove";
    /**
     * 修改设备广播
     */
    public final static String ACTION_OZNER_MANAGER_DEVICE_CHANGE = "com.ozner.manager.device.change";

    OznerContext mContext;
    String mOwner = "";

    public BaseOznerManager(OznerContext context) {
        mContext = context;
    }


    protected SQLiteDB getDB() {
        return mContext.getDB();
    }

    protected Context getContext() {
        return mContext.getApplication();
    }

    protected String getOwner() {
        return mOwner;
    }

    /**
     * 设置绑定的用户
     *
     * @param Owner 用户ID
     */
    public void setOwner(String Owner) {
        if (Owner == null)
            return;
        if (Owner.isEmpty())
            return;
        if (mOwner.equals(Owner))
            return;
        mOwner = Owner;
        dbg.i("Set Owner:%s", Owner);

    }
}


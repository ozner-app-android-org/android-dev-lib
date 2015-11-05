package com.ozner.device;

/**
 * Created by xzyxd on 2015/11/3.
 */
public class OperateCallbackProxy<T> implements OperateCallback<T> {

    private OperateCallback<T> callback;

    public OperateCallbackProxy(OperateCallback<T> callback) {
        this.callback = callback;
    }

    @Override
    public void onSuccess(T var1) {
        if (callback != null)
            callback.onSuccess(var1);
    }

    @Override
    public void onFailure(Throwable var1) {

        if (callback != null)
            callback.onFailure(var1);
    }
}

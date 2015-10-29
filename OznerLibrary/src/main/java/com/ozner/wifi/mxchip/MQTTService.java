package com.ozner.wifi.mxchip;

import android.content.Context;

import com.ozner.util.Helper;
import com.ozner.util.dbg;
import com.ozner.wifi.OperationCallback;
import com.ozner.wifi.WifiControlCallback;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.net.URISyntaxException;

/**
 * Created by zhiyongxu on 15/10/26.
 */
public class MQTTService implements Listener {
    private final static String host = "tcp://api.easylink.io:1883";
    private final static String userName = "admin";
    private final static String passWord = "123456";

    WifiControlCallback callback;
    MQTT mqtt = new MQTT();
    CallbackConnection connection;
    Context context;
    /**
     * 收到消息的回调
     */
    onPublishCallback onPublishCallback = null;

    public MQTTService(Context context) {
        this.context = context;
        //MQTT设置说明
        try {
            mqtt.setHost(host);
            mqtt.setClientId("v1-app-" + Helper.rndString(12)); //用于设置客户端会话的ID。在setCleanSession(false);被调用时，MQTT服务器利用该ID获得相应的会话。此ID应少于23个字符，默认根据本机地址、端口和时间自动生成
            mqtt.setCleanSession(false); //若设为false，MQTT服务器将持久化客户端会话的主体订阅和ACK位置，默认为true
            mqtt.setKeepAlive((short) 60);//定义客户端传来消息的最大时间间隔秒数，服务器可以据此判断与客户端的连接是否已经断开，从而避免TCP/IP超时的长时间等待
            mqtt.setUserName("admin");//服务器认证用户名
            mqtt.setPassword("admin");//服务器认证密码
//        mqtt.setWillTopic("willTopic");//设置“遗嘱”消息的话题，若客户端与服务器之间的连接意外中断，服务器将发布客户端的“遗嘱”消息
//        mqtt.setWillMessage("willMessage");//设置“遗嘱”消息的内容，默认是长度为零的消息
//        mqtt.setWillQos(QoS.AT_LEAST_ONCE);//设置“遗嘱”消息的QoS，默认为QoS.ATMOSTONCE
//        mqtt.setWillRetain(true);//若想要在发布“遗嘱”消息时拥有retain选项，则为true
//        mqtt.setVersion("3.1.1");
//        mqtt.setConnectAttemptsMax(10L);//客户端首次连接到服务器时，连接的最大重试次数，超出该次数客户端将返回错误。-1意为无重试上限，默认为-1
            mqtt.setReconnectAttemptsMax(-1);//客户端已经连接到服务器，但因某种原因连接断开时的最大重试次数，超出该次数客户端将返回错误。-1意为无重试上限，默认为-1
            mqtt.setReconnectDelay(10L);//首次重连接间隔毫秒数，默认为10ms
            mqtt.setReconnectDelayMax(30000L);//重连接间隔毫秒数，默认为30000ms
            mqtt.setReconnectBackOffMultiplier(2);//设置重连接指数回归。设置为1则停用指数回归，默认为2

            //Socket设置说明
            mqtt.setReceiveBufferSize(65536);//设置socket接收缓冲区大小，默认为65536（64k）
            mqtt.setSendBufferSize(65536);//设置socket发送缓冲区大小，默认为65536（64k）
            mqtt.setTrafficClass(8);//设置发送数据包头的流量类型或服务类型字段，默认为8，意为吞吐量最大化传输

            //带宽限制设置说明
            mqtt.setMaxReadRate(0);//设置连接的最大接收速率，单位为bytes/s。默认为0，即无限制
            mqtt.setMaxWriteRate(0);//设置连接的最大发送速率，单位为bytes/s。默认为0，即无限制
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        connection = mqtt.callbackConnection();
        connection.listener(this);
        connection.connect(new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dbg.d("onSuccess");
                //setSubscribe();
            }

            @Override
            public void onFailure(Throwable throwable) {
                dbg.d("onFailure");
            }
        });
    }

    public MQTTService.onPublishCallback getOnPublishCallback() {
        return onPublishCallback;
    }

    public void setOnPublishCallback(onPublishCallback callback) {
        onPublishCallback = callback;
    }

    /*private void setSubscribe()
    {
        if (subscribes.size()>0) {
            connection.subscribe(subscribes.toArray(new Topic[0]), new Callback<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    dbg.d("subscribe onSuccess");
                }

                @Override
                public void onFailure(Throwable throwable) {
                    dbg.d("subscribe onFailure");
                }
            });
        }
    }

    public void subscribe(String topic)
    {
        for(Topic t : subscribes)
        {
            if (t.name().toString().equals(topic))
            {
                return;
            }
        }
        subscribes.add(new Topic(topic,QoS.AT_LEAST_ONCE));
        connection.subscribe(subscribes.toArray(new Topic[0]), null);
    }

    public void unSubscribe(String topic)
    {
        for (int i=0;i<subscribes.size();i++)
        {
            if (subscribes.get(i).name().equals(topic))
            {
                subscribes.remove(i);
                break;
            }
        }
        connection.subscribe(subscribes.toArray(new Topic[0]), null);
    }*/
    public void subscribe(String topic) {
        connection.subscribe(new Topic[]{new Topic(topic, QoS.AT_LEAST_ONCE)}, null);
    }

    public void unSubscribe(String topic) {
        connection.unsubscribe(new UTF8Buffer[]{new UTF8Buffer(topic)}, null);
    }

    public void publish(String topic, byte[] data, OperationCallback<Void> cb) {
        connection.publish(topic, data, QoS.AT_LEAST_ONCE, false, cb);
    }

    @Override
    public void onConnected() {
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onPublish(UTF8Buffer utf8Buffer, Buffer buffer, Runnable runnable) {
        String name = utf8Buffer.toString();
        byte[] data = buffer.getData();
        if (onPublishCallback != null) {
            onPublishCallback.onPublish(name, data);
        }
    }

    @Override
    public void onFailure(Throwable throwable) {

    }

    public interface onPublishCallback {
        void onPublish(String topic, byte[] data);
    }
}

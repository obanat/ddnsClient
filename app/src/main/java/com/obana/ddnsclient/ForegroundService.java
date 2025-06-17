package com.obana.ddnsclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;

public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String ZONE = "my.dynv5.net";
    private static final String TOAKEN = "i1tsBKsKb6yQ7CBWNJ_111111";
    private static final long INTERVAL = 10*60 * 1000; // 10分钟
    private Handler handler;
    private Thread mDdnsThread;
    private Network mNetwork;

    private String mZone = ZONE;
    private String mToken = TOAKEN;
    private long mPeriod = INTERVAL;
    private Notification mNotification;
    private int mUpdateCount = 0;

    @Override
    public void onCreate() {

        super.onCreate();
        createNotificationChannel();
        startForegroundWithPersistentNotification();
        Log.i(TAG, "---> onCreate");


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mZone = prefs.getString("pref_domain_name","");
        mToken = prefs.getString("pref_user_token","");
        String index = prefs.getString("pref_update_period","");
        if ("1".equals(index) ) {
            mPeriod = 5 *60 * 1000;
        } else if ("2".equals(index)) {
            mPeriod = 10 *60 * 1000;
        } else if ("3".equals(index) ) {
            mPeriod = 30 *60 * 1000;
        } else {
            mPeriod = 10 * 1000;
        }

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                // 处理接收到的消息
                Log.i(TAG, "new message :" + msg.what);
                if (msg.what == 1000) {
                    updateNotification();
                }
            }
        };


        mDdnsThread = new Thread() {
            @Override
            public void run() {
                while (true){

                    Log.i(TAG, "---> Thread run once");
                    executeDdnsTask();
                    try {
                        sleep(mPeriod);
                    } catch (Exception e) {
                        //throw new RuntimeException(e);
                    }
                }
            }
        };

        mDdnsThread.setName("ddnsTask");
        requestCellNetwork();
    }

    private void executeDdnsTask() {

        Dynv6Updater.updateAAAAIfChanged(this,mZone,TOAKEN);
        // 这里添加你的实际任务逻辑
    }

    private void startForegroundWithPersistentNotification() {
        mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("常驻服务运行中")
                .setContentText("正在保持后台运行")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();

        startForeground(NOTIFICATION_ID, mNotification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "常驻服务通道",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("保持服务运行的通道");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //handler.removeCallbacks(mDdnsThread);
        super.onDestroy();
    }

    private void requestCellNetwork() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr == null) return;

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

        NetworkRequest build = builder.build();
        Log.i(TAG, "---> start request cell network ");

        conMgr.requestNetwork(build, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.i(TAG, "---> request network OK! start connectRunnable...");
                mNetwork = network;
                Dynv6Updater.init(ForegroundService.this, mNetwork, conMgr, handler);
                mDdnsThread.start();
            }
        });


    }
    public void updateNotification() {
        mUpdateCount++;

        String newContent;
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        newContent= "上次更新:" + currentTime.format(formatter) + " [" + mUpdateCount + "]";
        Notification notification = createNotification("DDNS域名:"+mZone, newContent);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String title, String content) {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(Notification.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }

        return builder.build();
    }
}

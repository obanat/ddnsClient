package com.obana.ddnsclient;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //startForegroundService();
    }

    private void startForegroundService() {
        Intent intent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            Log.i("APP","startForegroundService");
        } else {
            startService(intent);
            Log.i("APP","startService");
        }
    }
}

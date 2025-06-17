package com.obana.ddnsclient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Dynv6Updater {
    private static final String TAG = "Dynv6Updater";
    private static final String DYNV6_API = "https://dynv6.com/api/update?zone={zone}&token={token}&ipv6={ipv6}";

    private String mLocalIpV6Address;
    private static Context mContext;
    private static Network mNetwork;
    private static ConnectivityManager mCm;

    private static Handler mHandler;
    public static void updateAAAAIfChanged(Context context, String zone, String token) {

        if (mCm == null || mNetwork == null) return;


        String currentIp = getLocalIpv6(mNetwork,mCm);
        String dnsIp = getDnsIPv6(zone);

        Log.i(TAG, "updateAAAAIfChanged cur：" + currentIp + " dns:" + dnsIp);

        if (currentIp != null && !currentIp.equals(dnsIp)) {
            updateDynv6Record(zone, token, currentIp);
            mHandler.sendMessage(Message.obtain(mHandler,1000));
        }
    }

    public static void init(Context context, Network network, ConnectivityManager cm, Handler handler) {
        mContext = context;
        mNetwork = network;
        mHandler = handler;
        mCm = cm;
    }


    private static String getDnsIPv6(String zone) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(zone);
            for (InetAddress address : addresses) {
                if (address instanceof Inet6Address) {
                    return address.getHostAddress();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getDnsIPv6 exception：", e);
        }


        return null;
    }

    private static void updateDynv6Record(String zone, String token, String ipv6) {
        try {
            String urlStr = DYNV6_API
                    .replace("{zone}", zone)
                    .replace("{token}", token)
                    .replace("{ipv6}", ipv6);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                Log.i(TAG, "AAAA记录更新成功: " + ipv6);
            } else {
                Log.e(TAG, "更新失败，HTTP状态码: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            Log.e(TAG, "API调用失败", e);
        }
    }

    private static String getLocalIpv6(Network network, ConnectivityManager cm) {
        String localIpV6Address;
        try {
            LinkProperties lp = cm.getLinkProperties(network);
            if (lp != null) {
                for (LinkAddress la : lp.getLinkAddresses()) {
                    if (la.getAddress() != null && la.getAddress().getAddress().length == 16) {
                        return la.getAddress().getHostAddress();
                    }
                }
            }
            Log.e(TAG, "getLocalIpv6, Error Return NULL");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "get Local IPv6, Exception:" + e.getMessage());
        }
        return null;
    }
}

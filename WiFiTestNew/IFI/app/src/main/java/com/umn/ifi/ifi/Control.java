package com.umn.ifi.ifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by gaurav on 11/21/15.
 */
public class Control {
    public static Context mContext;
    public static void actionTaker(String command, WifiManager wifiManager) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException, InvocationTargetException {
        if(command.equals("OFF"))
        {
            wifiManager.disconnect();
            wifiManager.setWifiEnabled(false);
            boolean enabled = true;
            /*final ConnectivityManager conman = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Class conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);*/
        }
        else
        {
            wifiManager.setWifiEnabled(true);
        }
    }
}

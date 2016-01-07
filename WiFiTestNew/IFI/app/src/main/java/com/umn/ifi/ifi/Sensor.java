package com.umn.ifi.ifi;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by gaurav on 11/21/15.
 */
public class Sensor {
    public static int numberofTicks=0;
    public static WifiManager wifiManager;
    static Context mContext;
    public static double gsmLevel = -1;
    private static TelephonyManager        Tel;
    private static MyPhoneStateListener    MyListener = new MyPhoneStateListener();;
    public static double[] getVelocity() {
        return new double[]{0,0,0};
    }

    public static double getWiFiStrength(Context mmContext)
    {
        mContext = mmContext;
        int wifiLevel =0;
        wifiManager= (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo != null)
        {
            wifiLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
        }
        return wifiLevel;
    }

    public static double getMobileDataStrength(Context mmContext) {

        mContext = mmContext;
        return gsmLevel;
       /* Tel       = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS );
        return gsmLevel;*/
        /*TelephonyManager telephonyManager =        (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        CellInfoGsm cellinfogsm = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            cellinfogsm = (CellInfoGsm)telephonyManager.getAllCellInfo().get(0);
            CellSignalStrengthGsm cellSignalStrengthGsm = cellinfogsm.getCellSignalStrength();
            return cellSignalStrengthGsm.getDbm();
        }
        else
            return -1;*/
        /*try {
            final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                for (final CellInfo info : tm.getAllCellInfo()) {
                    if (info instanceof CellInfoGsm) {
                        final CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        gsmLevel =  gsm.getDbm();
                        // do what you need
                    } else if (info instanceof CellInfoCdma) {
                        final CellSignalStrengthCdma cdma = ((CellInfoCdma) info).getCellSignalStrength();
                        gsmLevel = cdma.getDbm();
                        // do what you need
                    } else if (info instanceof CellInfoLte) {
                        final CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                        gsmLevel =  lte.getDbm();
                        // do what you need
                    } else {
                        throw new Exception("Unknown type of cell signal!");
                    }
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return gsmLevel;*/

    }

    /* start of private class */
    private static class MyPhoneStateListener extends PhoneStateListener
    {
        /* Get the Signal strength from the provider, each tiome there is an update */
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength)
        {
            super.onSignalStrengthsChanged(signalStrength);
            gsmLevel = signalStrength.getGsmSignalStrength();
        }
        @Override
        public void onCellLocationChanged(CellLocation location)
        {
        }

    };/* End of private Class */
}

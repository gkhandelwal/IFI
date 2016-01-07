package com.example.gaurav.wifitest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
;
import android.support.v7.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    int wifiLevel =0;

    WifiManager wifiManager1;
    List<ScanResult> wifiList;
    StringBuilder sb = new StringBuilder();
    TextView mainText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            checkNetworkStrength();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        EditText abc = (EditText) findViewById(R.id.edit_message);
        //abc.setBackgroundColor(Color.WHITE);
        //abc.setText(Integer.toString(wifiLevel));
        String helloText="wifi Available \n";
        if(wifiList!=null) {
            for (int i = 0; i < wifiList.size(); i++) {
                if ((wifiList.get(i).SSID).isEmpty() == false)
                    helloText = helloText + wifiList.get(i).SSID + ":" + WifiManager.calculateSignalLevel(wifiList.get(i).level, 5) + "\n";
            }

            WifiInfo wifiInfo = wifiManager1.getConnectionInfo();
            helloText = helloText + "\n WiFi Connected to =>" + wifiInfo.getSSID() + "\n";
        }
        else
            helloText= "Connected to Mobile Data";
        abc.setText(helloText);
    }


    private void checkNetworkStrength() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException {



        wifiManager1 = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager1.getConnectionInfo();

        if (wifiInfo != null)
        {
            wifiLevel = WifiManager.calculateSignalLevel(wifiInfo.getRssi(),5);
            if(wifiLevel<=3) {
            //
                /* for 3g */
                wifiManager1.setWifiEnabled(false);
                boolean enabled = true;
                final ConnectivityManager conman = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                final Class conmanClass = Class.forName(conman.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(conman);
                final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);

                setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);

                /*end of 3G*/

            }
            else
            {
                wifiManager1.setWifiEnabled(true);
                wifiManager1.startScan();
                wifiList = wifiManager1.getScanResults();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    /* Can be used when a new WiFi AP detected... as of now I am ignoring it */
    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {

            sb = new StringBuilder();
            wifiList = wifiManager1.getScanResults();
            sb.append("\n        Number Of Wifi connections :"+wifiList.size()+"\n\n");

            for(int i = 0; i < wifiList.size(); i++){

                sb.append(new Integer(i+1).toString() + ". ");
                sb.append((wifiList.get(i)).toString());
                sb.append("\n\n");
            }

            mainText.setText(sb);
        }

    }
}

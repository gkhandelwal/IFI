package com.umn.ifi.ifi;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.StrictMode;
public class MainActivity extends Activity {

    private ListView listView = null;
    private ArrayAdapter arrayAdapter = null;
    private SensingResultReceiver mReceiver;
    private ServiceConnection conn;
    private boolean connected = false;
    SensingService mServer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        mReceiver = new SensingResultReceiver(new Handler());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((Button)findViewById(R.id.mode_button)).setText(R.string.m_mode);
        setWiFiStrength(0, "none");
        setCellStrength(0,"unknown");

        conn = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the object we can use to
                // interact with the service.  We are communicating with the
                // service using a Messenger, so here we get a client-side
                // representation of that from the raw IBinder object.
                connected = true;
                SensingService.LocalBinder mLocalBinder = (SensingService.LocalBinder)service;
                mServer = mLocalBinder.getServerInstance();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                conn = null;
                mServer = null;
                connected = false;
                //also stop the service
                Intent i = new Intent(MainActivity.this, SensingService.class);
                stopService(i);
            }
        };
        if(isServiceRunning()){
            log("Service running, attempting to connect...");
            Intent i = new Intent(Intent.ACTION_SYNC, null, this, SensingService.class);
            i.putExtra("receiver", mReceiver);
            bindService(i, conn, Context.BIND_IMPORTANT);
            log("Connection established");
        }else {
            log("Service not running");
        }
    }

    public class SensingResultReceiver extends ResultReceiver {
        public SensingResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // do the stuff
            switch (resultCode) {
                case SensingService.STATUS_RUNNING:
                    setProgressBarIndeterminateVisibility(true);
                    break;
                case SensingService.STATUS_FINISHED:

                    setProgressBarIndeterminateVisibility(false);

                /* Update UI*/
                    setWiFiStrength(resultData.getDouble("wifi"), resultData.getString("ssid"));
                    setCellStrength(resultData.getInt("cell"), resultData.getString("cellType"));
                    setMode(resultData.getInt("mode"));
                    setDownloadRate(resultData.getDouble("download"));
                    String act = resultData.getString("action");
                    if(!act.toLowerCase().equals("none")) {
                        log("got update from service. action:" + resultData.getString("action"));
                    }
                    break;
                case SensingService.STATUS_ERROR:
                /* Handle the error */
                    String error = resultData.getString(Intent.EXTRA_TEXT);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    break;
                case SensingService.STATUS_WIFI:
                /* Handle the error */
                    String err = resultData.getString("result");
                    Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
    public void toggleCapture(View v){
        toggleCapture();
    }
    public void toggleCapture(){
        if(connected) {
            mServer.toggleCaptureData();
            if (mServer.captureData){
                log("Data Capture Started.");
            }else{
                log("Data Capture Stopped.");
            }
        }else{
            log("Can't capture data: not connected!");
        }
    }
    public void toggleService(View v){
        toggleService();
    }
    public void toggleService() {

        if(isServiceRunning()){
            unbindService(conn);
            log("Service Stopped");
        }else {
            Intent i = new Intent(Intent.ACTION_SYNC, null, this, SensingService.class);
            i.putExtra("receiver", mReceiver);
            log("Service not running, starting and attempting to connect...");
            startService(i);
            bindService(i, conn, Context.BIND_AUTO_CREATE);
            log("Connection established");
        }
    }

    public void markInterval(View v){ registerInterval();}
    public void registerInterval(){
        if(connected) {
            if(mServer.captureData) {
                mServer.markInterval();
                log("new interval registered");
            }else{
                log("Can't mark interval: not capturing!");
            }
        }else{
            log("Can't capture data: not connected!");
        }

    }
    public void enterSkepticalMode(View v){
        enterSkepticalMode();
    }
    public void enterSkepticalMode(){
        if(mServer != null && connected){
            mServer.toggleSkeptical();
            log("Entering Skeptical Mode...");
        }else{
            log("Service not running! Start it and try again.");
        }

    }
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SensingService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public void setMode(int m){
        String mode = "Unknown Mode";
        switch(m){
            case SensingService.MODE_MONITOR:
                mode = "Monitor";
                break;
            case SensingService.MODE_SKEPTICAL:
                mode = "Skeptical";
                break;
            case SensingService.MODE_COOLDOWN:
                mode = "Cooldown";
                break;
        }
        ((TextView) findViewById(R.id.mode)).setText("Mode: " + mode);
    }
    public void setWiFiStrength(double s, String id){
        ((TextView) findViewById(R.id.wifi_status)).setText("WiFi: "+Double.toString(s) + " ("+id+")");
    }
    public void setCellStrength(double s, String t){
        ((TextView) findViewById(R.id.cell_status)).setText("Cell: " + Double.toString(s) + "("+t+")");
    }
    public void toggleDownload(View v){
        if(connected && mServer != null){
            mServer.toggleDownload();
        }
    }
    public void setDownloadRate(double s){
       ((TextView) findViewById(R.id.down)).setText("Download: " + Double.toString(s));
        //((TextView) findViewById(R.id.down)).setText("Download: " + s);
    }
    public void log(String msg){
        TextView logtxt = ((TextView) findViewById(R.id.log));
        logtxt.setText(msg + '\n' + logtxt.getText());
    }
}


package com.ifi.ifi;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private ComponentName srv;
    private boolean connected = false;
    private ServiceConnection conn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ((TextView) findViewById(R.id.mode)).setText("Monitor");
        ((Button)findViewById(R.id.mode_button)).setText(R.string.m_mode);
        setWiFiStrength(0);
        setCellStrength(0);
        setDisplacement(0);

        conn = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the object we can use to
                // interact with the service.  We are communicating with the
                // service using a Messenger, so here we get a client-side
                // representation of that from the raw IBinder object.

                connected = true;
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                conn = null;
                connected = false;
            }
        };
        if(isServiceRunning()){
            log("Service running, attempting to connect...");
            Intent i = new Intent(getBaseContext(), com.ifi.ifi.Monitor.class);
            bindService(i, conn, Context.BIND_IMPORTANT);
            log("Connection established");
        }else {
            log("Service not running");
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void toggleService() {
        Intent i = new Intent(getBaseContext(), com.ifi.ifi.Monitor.class);
        if(isServiceRunning()){
            stopService(i);
            log("Service Stopped");
        }else {
            log("Service not running, starting and attempting to connect...");
            bindService(i, conn, Context.BIND_AUTO_CREATE);
            log("Connection established");
        }
    }
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (com.ifi.ifi.Monitor.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public void setWiFiStrength(double s){
        ((TextView) findViewById(R.id.wifi_status)).setText("WiFi: "+Double.toString(s));
    }
    public void setCellStrength(double s){
        ((TextView) findViewById(R.id.cell_status)).setText("Cell: " + Double.toString(s));
    }
    public void setDisplacement(double s){
        ((TextView) findViewById(R.id.disp)).setText("Displacement: " + Double.toString(s));
    }
    public void log(String msg){
        TextView logtxt = ((TextView) findViewById(R.id.log));
        logtxt.setText(msg + '\n' + logtxt.getText());
    }
}

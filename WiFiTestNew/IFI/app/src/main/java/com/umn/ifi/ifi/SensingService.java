package com.umn.ifi.ifi;


import android.app.IntentService;


import android.content.Context;
import android.content.Intent;

import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;

import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/**
 * Created by gaurav on 11/21/15.
 */
public class SensingService extends IntentService {

    private int bytesIn=0;
    long downloadTime = 1;
    long start = 0;
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_WIFI = 3;

    public static final int MODE_MONITOR = 0;
    public static final int MODE_SKEPTICAL = 1;
    public static final int MODE_COOLDOWN = 2;
    //private int cooldownTime = 10000;
    private int mode = MODE_MONITOR;
    public boolean captureData = false;
    private File dataFile;
    private FileOutputStream dataOut;
    private int dataInterval = 0;
    private String capID = "";
    private static final String TAG = "SensingService";
    private TelephonyManager mManager;
    private ConnectivityManager cManager;
    private WifiManager wManager;
    private Double wifiSignalStrength = -1d;
    private String wifiSSID = "none";
    private int mobileSignalStrength = -1;
    private SignalStrength mSignalStrength;
    private ConnectionQuality mQuality;
    private String mobileType = "unknown";
    private int activeNetworkType = -1;
    private int mobileState = -1;
    private String[] sSignal;
    private CellLocation mCellLocation;
    private final IBinder mBinder = new LocalBinder();
    private Handler mHandler;
    private Handler kHandler;
    private ResultReceiver receiver;
    private Timer mTimer;
    private int sampleIx = 0;
    private long totalBytesRxLastTime = -1;
    private long totalBytesTxLastTime = -1;
    private float dataDownRateTrafficStats = 0;
    private float dataUpRateTrafficStats = 0;
    private boolean canUseTrafficStats = false;
    private boolean waitingForCellSignal = false;
    private ConnectionQuality mConnectionClass = ConnectionQuality.UNKNOWN;
    private boolean waitingForDownload = false;
    private ConnectionClassManager mConnectionClassManager;
    private DeviceBandwidthSampler mDeviceBandwidthSampler;
    private ConnectionChangedListener mListenerfb;
    private static int isRetryFlag = 0;
    private static long myDownloadSpeed =0;
    private double movingAverage = Double.POSITIVE_INFINITY;
    private LinkedList<Float> movingAverageSamples = new LinkedList<Float>();
    private int mAvgN = 3;
    private double regressionSlope = Double.POSITIVE_INFINITY;
    private SimpleRegression regression;
    private LinkedList<Double> regressionSamples = new LinkedList<Double>();
    private int regN = 3;
    private LinkedList<Double>regressionSumSamples = new LinkedList<Double>();
    private int regSumN = 3;
    private double regressionSum=0;
    private static ConnectionQuality cq;
    private double pingDelay=0;
    private final static int UPDATE_THRESHOLD=300;
    private String action="none";
    private double timePassed = 0d;
    // File url to download
    //private static String file_url = "http://api.androidhive.info/progressdialog/hive.jpg";
    private static String file_url = "http://connectionclass.parseapp.com/m100_hubble_4060.jpg";
    // For turning on and off download of big file
    private boolean globalDownloadFlag = false;
    

    //Listen for connection class changes
    private class bandwidthListener implements ConnectionClassManager.ConnectionClassStateChangeListener{
        public void onBandwidthStateChange(ConnectionQuality bandwidthState){
            mQuality = bandwidthState;

        }
    }
    // Listener for signal strength.
    final PhoneStateListener mListener = new PhoneStateListener()
    {
        @Override
        public void onCellLocationChanged(CellLocation mLocation)
        {
            super.onCellLocationChanged(mLocation);
            Log.d(TAG, "Cell location obtained.");
            mCellLocation = mLocation;
        }
        @Override
        public void onDataConnectionStateChanged(int state, int net){
            switch(net){
                case 0: mobileType = "unknown"; break;
                case 1: mobileType = "GPRS"; break;
                case 2: mobileType = "EDGE"; break;
                case 3: mobileType = "UMTS"; break;
                case 4: mobileType = "CDMA"; break;
                case 5: mobileType = "EVDO_0"; break;
                case 6: mobileType = "EVDO_A"; break;
                case 7: mobileType = "1xRTT"; break;
                case 8: mobileType = "HSDPA"; break;
                case 9: mobileType = "HSUPA"; break;
                case 10: mobileType = "HSPA"; break;
                case 11: mobileType = "iDen"; break;
                case 12: mobileType = "EVDO_B"; break;
                case 13: mobileType = "LTE"; break;
                case 14: mobileType = "eHRPD"; break;
                case 15: mobileType = "HSPA+"; break;
            }
            switch(state) {

            }
        }
        @Override
        public void onSignalStrengthsChanged(SignalStrength sStrength) {
            super.onSignalStrengthsChanged(sStrength);
            Log.d(TAG, "Signal strength obtained.");
            sSignal = sStrength.toString().split(" ");
            /* part[0] = "Signalstrength:"  _ignore this, it's just the title_
                parts[1] = GsmSignalStrength
                parts[2] = GsmBitErrorRate
                parts[3] = CdmaDbm
                parts[4] = CdmaEcio
                parts[5] = EvdoDbm
                parts[6] = EvdoEcio
                parts[7] = EvdoSnr
                parts[8] = LteSignalStrength
                parts[9] = LteRsrp
                parts[10] = LteRsrq
                parts[11] = LteRssnr
                parts[12] = LteCqi
                parts[13] = gsm|lte
                parts[14] = _not reall sure what this number is_ */
            //different phones implement this differently
            if(sStrength.isGsm()){
                mobileSignalStrength = getGsmStrength(sSignal);
            }else if (mobileType.toLowerCase().equals("lte")) {
                mobileSignalStrength = getLteStrength(sSignal);
            }else if(mobileType.toLowerCase().equals("cdma")){
                //cdma = 2g (i think)
                mobileSignalStrength = getCdmaStrength(sSignal);
            }else if(mobileType.toLowerCase().substring(0,4).equals("evdo") ||  mobileType.toLowerCase().equals("ehrpd")){
                //3g
                mobileSignalStrength = getEvdoStrength(sSignal);
            }
        }
    };

    public SensingService() {
        super(SensingService.class.getName());
    }

    public void onCreate(){
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "Handling Intent");
        totalBytesRxLastTime = TrafficStats.getTotalRxBytes();
        totalBytesTxLastTime = TrafficStats.getTotalTxBytes();
        setMovingAverageN(mAvgN);
        regression = new SimpleRegression();
        setRegressionN(regN);
        regressionSumSamples = new LinkedList<Double>();
        if(totalBytesRxLastTime == TrafficStats.UNSUPPORTED){
            canUseTrafficStats = false;
            Log.d(TAG, "TrafficStats monitoring unsupported!");
        }else{
            canUseTrafficStats = true;
        }
        receiver = intent.getParcelableExtra("receiver");
        mManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        cManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mManager.listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_DATA_ACTIVITY);

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                //do stuff
                //Toast.makeText(SensingService.this, "Message says: "+message.arg1, Toast.LENGTH_LONG).show();
                //sensing loop

                try {
                    tick();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        };
/*
        if(ping())
            Toast.makeText(SensingService.this, "Ping ON", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(SensingService.this, "Ping OFF", Toast.LENGTH_LONG).show();
*/
        int delay = 1000; // delay for 1 sec.
        int period = 1000; // repeat every 5 sec.
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Message mess = mHandler.obtainMessage();
                mess.arg1 = 2;
                mess.sendToTarget();
            }
        }, delay, period);

        mConnectionClassManager = ConnectionClassManager.getInstance();
        mDeviceBandwidthSampler = DeviceBandwidthSampler.getInstance();
        mListenerfb = new ConnectionChangedListener();
        if(globalDownloadFlag)
            helperFunction();
        try {
            tick();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        //don't know whether requittred
        mConnectionClassManager.register(mListenerfb);
    }

    private boolean ping(){
        System.out.println("executeCommand");
        Runtime runtime = Runtime.getRuntime();
        try
        {
            long start = System.currentTimeMillis();
            //Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 -w 1 8.8.8.8");
            int mExitValue = mIpAddrProcess.waitFor();
            pingDelay = (double)(System.currentTimeMillis() - start);
            System.out.println(" mExitValue "+mExitValue);
            if(mExitValue==0){
                return true;
            }else{
                return false;
            }
        }
        catch (Exception e)
        {
            pingDelay = -2;
            Log.d(TAG," Exception:"+e.getMessage()+e.getStackTrace());
        }
        return false;

/*        boolean worked = false;
        try {
            long before = System.currentTimeMillis();
            worked = InetAddress.getByName("www.google.com").isReachable(500);
            pingDelay = (double)(System.currentTimeMillis() - before);
        }catch(Exception e){
            Log.d(TAG,"ping failed: "+e.getMessage()+": "+e.getStackTrace());
            pingDelay = -2;
        }
        return worked;*/

    }

    private class ConnectionChangedListener
            implements ConnectionClassManager.ConnectionClassStateChangeListener {

        @Override
        public void onBandwidthStateChange(ConnectionQuality bandwidthState) {
            mConnectionClass = bandwidthState;
           /* runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mTextView.setText(mConnectionClass.toString());
                }
            });*/
        }
    }
    public void helperFunction()
    {
            kHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                //do stuff
                //Toast.makeText(SensingService.this, "Message says: "+message.arg1, Toast.LENGTH_LONG).show();
                //sensing loop
                if(globalDownloadFlag) {
                    new DownloadFileFromURL().execute(file_url);
                }
                //tick();
            }
        };
        Message mess = kHandler.obtainMessage();
        mess.arg1 = 2;
        mess.sendToTarget();

    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {

        public SensingService getServerInstance()
        {
            return SensingService.this;
        }

        }

    public void onDestroy(){
        Log.d(TAG, "Service Received Signal to Stop");

        // Register the listener with the telephony manager
        mManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
        mTimer.cancel();
        mTimer.purge();
        super.onDestroy();
    }
    public void tick() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        //update samples
        pingDelay = -1;
        timePassed += System.currentTimeMillis() - timePassed;
        Log.d(TAG, String.valueOf(isRetryFlag));
        if(isRetryFlag==0) {
            if(globalDownloadFlag)
                helperFunction();
        }

        NetworkInfo netinfo = cManager.getActiveNetworkInfo();
        if(netinfo != null){
            activeNetworkType = netinfo.getType();
        }else {
            activeNetworkType = -1;
        }
        WifiInfo winfo = wManager.getConnectionInfo();
        if(winfo!=null)
        {
            wifiSignalStrength = (double)winfo.getRssi();
            wifiSSID = winfo.getSSID();
        }
        long totalBytesRxThisTime = TrafficStats.getTotalRxBytes();
        dataDownRateTrafficStats = ((float)( totalBytesRxThisTime - totalBytesRxLastTime))/ (1024f);
        totalBytesRxLastTime = totalBytesRxThisTime;

        long totalBytesTxThisTime = TrafficStats.getTotalTxBytes();
        dataUpRateTrafficStats = ((float)( totalBytesTxThisTime - totalBytesTxLastTime))/ (1024f);
        totalBytesTxLastTime = totalBytesTxThisTime;

        updateMovingAverage();
        updateRegression();
        updateRegressionSum();

        Bundle bundle = new Bundle();
        action = "none";

        //control logic
        switch(mode){
            case MODE_MONITOR:
                break;
            case MODE_SKEPTICAL:
                try
                {
                    if(activeNetworkType == ConnectivityManager.TYPE_WIFI)
                    {
                        //check to see if we should shut it off
                        if((movingAverage<= 5)&&(regressionSum<=-3)) {
                            //we just lost download rate. Is it because we lost connectivity or is it because the phone just finished downloading?
                            waitingForDownload = true;
                        }
                        if(waitingForDownload){
                            if(movingAverage < 5) {
                                if (!ping()) {
                                    Control.actionTaker("OFF", wManager);
                                    action = "wifi_off";
                                    Toast.makeText(getApplicationContext(), String.valueOf("WIFI OFF"), Toast.LENGTH_SHORT).show();
                                    Sensor.numberofTicks = 0;
                                }else{
                                    //no op
                                }
                            }
                        }
                    }
                    else if(activeNetworkType == ConnectivityManager.TYPE_MOBILE || activeNetworkType == -1)
                    {
                        Sensor.numberofTicks++;
                        if(Sensor.numberofTicks >= 90 || getJakeCellStrength(sSignal) <= -275 ){
                            Control.actionTaker("ON", wManager);
                            action = "wifi_on";
                            Toast.makeText(getApplicationContext(), String.valueOf("WIFI ON"), Toast.LENGTH_SHORT).show();
                        }

                    }
                }
                catch (Exception e) {
                    Log.e(TAG, "ERROR!!!", e);
                    bundle.putString(Intent.EXTRA_TEXT, e.toString());
                }

                break;
            case MODE_COOLDOWN:
                break;
        }
        bundle.putInt("mode",mode);
        bundle.putDouble("wifi", wifiSignalStrength);
        bundle.putString("ssid",wifiSSID);
        bundle.putInt("cell", mobileSignalStrength);
        bundle.putString("cellType",mobileType);
        bundle.putString("action", action);

        // for download stuff
        //ConnectionQuality cq = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
        mDeviceBandwidthSampler.stopSampling();
        myDownloadSpeed = (bytesIn / (System.currentTimeMillis()-start)) * 1000;
        if(System.currentTimeMillis()-start==0)
            bundle.putDouble("download", -1.0);
        else
            bundle.putDouble("download", myDownloadSpeed);
        cq = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
        //bundle.putString("downloadfacebook", mConnectionClass.toString());
        bundle.putString("downloadfacebook", cq.toString());

        bytesIn = 0;
        start = System.currentTimeMillis();
        Log.d(TAG, "Updating UI...");
        receiver.send(STATUS_FINISHED, bundle);
        mDeviceBandwidthSampler.startSampling();

        if(captureData){
            logSample();
        }
    }


    // AsyncTask to avoid an ANR.
    private class ReflectionTask extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... mVoid)
        {
            mobileSignalStrength=mSignalStrength.getGsmSignalStrength();

            return null;
        }

        protected void onProgressUpdate(Void... progress)
        {
            // Do nothing...
        }

        protected void onPostExecute(Void result)
        {
            //notify whoever needs to be notified
        }
    }

    private final void logSample(){
        int ix = sampleIx - 1; //last one we saved
        //write data to file
        try {
            StringBuilder data = new StringBuilder();
            data.append(Double.toString(timePassed));
            data.append(",").append(Integer.toString(dataInterval));
            data.append(",").append(wifiSSID);
            data.append(",").append(wifiSignalStrength.toString());
            for(String s: sSignal){
                data.append(",").append(s);
            }
            data.append(",").append(mobileType);
            data.append(",").append(netTypeToString(activeNetworkType));
            data.append(",").append(Long.toString(myDownloadSpeed));
            data.append(",").append(cq);
            String dataDownRateTS = canUseTrafficStats? Double.toString(dataDownRateTrafficStats) : "Unsupported";
            String dataUpRateTS = canUseTrafficStats? Double.toString(dataUpRateTrafficStats) : "Unsupported";
            data.append(",").append(dataDownRateTS);
            data.append(",").append(dataUpRateTS);
            data.append(",").append(Double.toString(movingAverage));
            data.append(",").append(Integer.toString(mAvgN));
            data.append(",").append(Double.toString(regressionSlope));
            data.append(",").append(Integer.toString(regN));
            data.append(",").append(Double.toString(regressionSum));
            data.append(",").append(Double.toString(pingDelay));
            data.append(",").append(action);
            String line = data+"\n";
            dataOut.write(line.getBytes());
            Log.d(TAG, "saved line to file.");
        }catch(Exception e){
            Log.d(TAG,"Could not save data to file:" + e.getMessage());
        }

    }

    private String netTypeToString(int t){
        String type = "unknown";
        switch(t){
            case ConnectivityManager.TYPE_WIFI:
                type = "wifi";
                break;
            case ConnectivityManager.TYPE_MOBILE:
                type = "mobile";
                break;
            default:
                break;
        }
        return type;
    }
    private int getLteStrength(String[] vals){
        return Integer.parseInt(vals[8]) * 2 - 113;
    }
    private int getGsmStrength(String[] vals){
        return Integer.parseInt(vals[1]) * 2 - 113;
    }
    private int getCdmaStrength(String[] vals){
        return Integer.parseInt(vals[3]);
    }
    private int getEvdoStrength(String[] vals){
        return Integer.parseInt(vals[5]);
    }
    private int getJakeCellStrength(String[] vals){return Integer.parseInt(vals[14]);}
    private final void update()
    {
        if (mSignalStrength == null || mCellLocation == null) return;

        ReflectionTask mTask = new ReflectionTask();
        mTask.execute();
    }
    public void toggleSkeptical(){
        //only do it if we're in monitor mode
        if(mode == MODE_MONITOR){
            mode = MODE_SKEPTICAL;
            /*mTimer.cancel();
            mTimer.purge();
            int delay = 500; // delay for .5 sec.
            int period = 500; // repeat every 0.5 sec.
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    Message mess = mHandler.obtainMessage();
                    mess.arg1 = 0;
                    mess.sendToTarget();
                }
            }, delay, period);*/
        }else{
            mode = MODE_MONITOR;
        }
    }
    public void toggleDownload() {
        if(globalDownloadFlag==true)
            globalDownloadFlag = false;
        else
            globalDownloadFlag = true;
    }
    public void toggleCaptureData(){
        captureData = !captureData;
        if(captureData){
            try {
                dataFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ifi_samples.csv");
                boolean newFile = false;
                if (!dataFile.exists()) {
                    dataFile.createNewFile();
                    newFile = true;
                }
                dataOut = new FileOutputStream(dataFile, true);
                if(newFile){
                    String header = "CaptureGroup,Interval,SSID,WIFI_RSSI,Ignore,GSM_Signal_Strength,GSM_BER,CDMA_DBM,DCMA_ECIO,EVDO_DBM,EVDO_ECIO,EVDO_SNR,LTE_Signal_Strength,LTE_RSRP,LTERSRQ,LTE_RSSNR,LTE_CQI,?,NETWORK_TYPE_SS,??,NETWORK_TYPE_TM,NET_INT,RAV_DL,FB_DL,TS_DL,TS_UL,M_AVG,AVG_N, M_SLOPE,SLOPE_N,REG_SUM,PING,ACTION\n";
                    dataOut.write(header.getBytes());
                }
                capID = UUID.randomUUID().toString();
            }catch(Exception e){
                Log.d(TAG, "Could not open file: "+e.getMessage());
            }
        }else{
            try {
                dataOut.close();
            }catch(Exception e){
                Log.d(TAG,"Could not close file stream:"+e.getMessage());
            }
            //make file visible to usb
            MediaScannerConnection.scanFile(this, new String[]{dataFile.toString()}, null, null);
        }
    }
    public void markInterval(){
        dataInterval++;
    }
    private void setMovingAverageN(int n){
        mAvgN = n;
        LinkedList<Float> temp = movingAverageSamples;
        movingAverageSamples = new LinkedList<Float>();
        int count = 0;
        Iterator<Float> itr = temp.iterator();
        while(count <= n) {
            //first is latest
            if(itr.hasNext()) {
                movingAverageSamples.addLast(itr.next());
            }else{
                movingAverageSamples.addLast(0f);
            }
            count++;
        }

    }
    private void updateMovingAverage(){
        movingAverageSamples.addFirst(dataDownRateTrafficStats);
        while(movingAverageSamples.size() > mAvgN) {
            movingAverageSamples.removeLast();
        }
        float sum = 0;
        for(int i = 0; i < mAvgN; i++ ){
            sum+=movingAverageSamples.get(i);
        }
        movingAverage = sum/((double)mAvgN);
    }
    private void setRegressionN(int n ){
        regN = n;
        LinkedList<Double> temp = regressionSamples;
        regressionSamples = new LinkedList<Double>();
        int count = 0;
        Iterator<Double> itr = temp.iterator();
        while(count <= n) {
            //first is latest
            if(itr.hasNext()) {
                regressionSamples.addLast(itr.next());
            }else{
                regressionSamples.addLast(0d);
            }
            count++;
        }
    }
    private void updateRegression(){
        regression.clear();
        regressionSamples.addFirst((double)movingAverage);
        while(regressionSamples.size() > regN) {
            regressionSamples.removeLast();
        }
        for(int i = 0; i < regN; i++){
            regression.addData(regN-i,regressionSamples.get(i));
        }
        regressionSlope = regression.getSlope();
    }
    private void updateRegressionSum(){
        regressionSumSamples.addFirst(regressionSlope);
        while(regressionSumSamples.size() > regSumN) {
            regressionSumSamples.removeLast();
        }
        regressionSum = 0;
        for(int i = 0; i < regressionSumSamples.size(); i++){
            regressionSum += regressionSumSamples.get(i);
        }

    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        InputStream stream=null;
        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            isRetryFlag = 1;
            mDeviceBandwidthSampler.startSampling();
            super.onPreExecute();

        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {

            try {

                String downloadFileUrl="http://download.thinkbroadband.com/5MB.zip";
                long startCon=System.currentTimeMillis();
                URL url=new URL(f_url[0]);
                URLConnection con=url.openConnection();
                con.setUseCaches(false);
                long connectionLatency=System.currentTimeMillis()- startCon;
                stream=con.getInputStream();

                start =System.currentTimeMillis();
                int currentByte=0;
                long updateStart=System.currentTimeMillis();
                long updateDelta=0;
                int  bytesInThreshold=0;

                while(((currentByte=stream.read())!=-1)&&globalDownloadFlag){
                    bytesIn++;
                    bytesInThreshold++;
                    if(updateDelta>=UPDATE_THRESHOLD){
                        //int progress=(int)((bytesIn/(double)EXPECTED_SIZE_IN_BYTES)*100);
                        updateStart=System.currentTimeMillis();
                        //SpeedInfo speedInfo = calculate(updateDelta, bytesInThreshold);
                        bytesInThreshold=0;
                        //wait(10000);
                        //System.out.println(speedInfo.downspeed);
                        //Toast.makeText(getApplicationContext(), String.valueOf(speedInfo.downspeed), Toast.LENGTH_SHORT).show();
                    }
                    updateDelta = System.currentTimeMillis()- updateStart;
                }

                downloadTime=(System.currentTimeMillis()-start);
                if(downloadTime==0){
                    downloadTime=1;
                }


            } catch (Exception e) {
                //Log.e("Error: ", e.getMessage());
                isRetryFlag = 0;
                return null;
            }


            return null;
        }


        /**
         * After completing background task
         * Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {


            if(stream!=null)
                try {
                    isRetryFlag = 0;
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            finally{

            bytesIn = 0;
            mDeviceBandwidthSampler.stopSampling();
            // Retry for up to 10 times until we find a ConnectionClass.
            //
            if(globalDownloadFlag)
                helperFunction();
                }
            // setting downloaded into image view
            //my_image.setImageDrawable(Drawable.createFromPath(imagePath));
        }

    }
    private SpeedInfo calculate(final long downloadTime, final long bytesIn){
        SpeedInfo info=new SpeedInfo();
        //from mil to sec
        long bytespersecond   =(bytesIn / downloadTime) * 1000;
        double kilobits=bytespersecond * BYTE_TO_KILOBIT;
        double megabits=kilobits  * KILOBIT_TO_MEGABIT;
        info.downspeed=bytespersecond;
        info.kilobits=kilobits;
        info.megabits=megabits;

        return info;
    }
    private static class SpeedInfo{
        public double kilobits=0;
        public double megabits=0;
        public double downspeed=0;
    }
    private static final double EDGE_THRESHOLD = 176.0;
    private static final double BYTE_TO_KILOBIT = 0.0078125;
    private static final double KILOBIT_TO_MEGABIT = 0.0009765625;
}


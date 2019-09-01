package com.example.anish.sensor_data;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.anish.sensor_data.MainActivity.studentId;

public class wifi_service extends Service {
    private WifiManager wifiManager;
    private Timer timer;
    private String name, ip_ad, level;
    private Calendar today = Calendar.getInstance(TimeZone.getDefault());
    private int yToday = today.get(Calendar.YEAR);
    private int mToday = today.get(Calendar.MONTH);
    private int dToday = today.get(Calendar.DAY_OF_MONTH);

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    IBinder binder = new wifi_service.LocalBinder();
    public class LocalBinder extends Binder{
        public wifi_service getInstance(){
            return wifi_service.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startid){
        startForeground(3, runAsForeground());
        return START_STICKY;
    }

    private Notification runAsForeground(){
        if(Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel1 = new NotificationChannel("notification1", "channel1", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "notification1")
                .setContentTitle("Service running")
                .setContentText("Running")
                .setContentIntent(pendingIntent)
                .build();
        return notification;
    }


    @Override
    public void onCreate(){
        super.onCreate();
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(true);
                }
                registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
                wifiManager.startScan();
            }
        }, 0, 5*60*1000);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        timer.cancel();
        unregisterReceiver(wifiReceiver);
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo netinfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (ConnectivityManager.TYPE_WIFI == netinfo.getType()) {
                    WifiInfo winfo = wifiManager.getConnectionInfo();
                    int noOfLevels = 5;
                    level = Integer.toString(wifiManager.calculateSignalLevel(winfo.getRssi(), noOfLevels));
                    int ip = winfo.getIpAddress();
                    name = winfo.getSSID();
                    ip_ad = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
                    if(!ip_ad.equals("0.0.0.0"))
                        wifiToCsv();
                }
            }
        }
    };
    public void wifiToCsv(){

        String basedir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String id="";
        if(studentId!=null){
            id = studentId.getString("studId","");
        }
        String day = dToday+"_"+(mToday+1);
        String file_name = day+"_"+id+"_wifi.csv";
        String file_path = basedir + File.separator + file_name;
        File f = new File(file_path);
        CSVWriter writer;
        FileWriter fileWriter;
        try {
            if (f.exists() && !f.isDirectory()) {
                fileWriter = new FileWriter(file_path, true);
                writer = new CSVWriter(fileWriter);
            } else {
                writer = new CSVWriter(new FileWriter(file_path));
                String data[] = {"Date", "HH:MM:SS", "Wifi Name", "Ip Address", "Signal Strength"};
                writer.writeNext(data);
            }
            Calendar calender = Calendar.getInstance();
            calender.add(Calendar.DATE, 0);
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            String yday = df.format(calender.getTime());
            DateFormat df2 = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
            String ytime = df2.format(calender.getTime());
            String data[] = new String[5];
            data[0] = yday; data[1] = ytime; data[2] = name; data[3] = ip_ad; data[4]=level;

            writer.writeNext(data);
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
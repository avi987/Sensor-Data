package com.example.anish.sensor_data;


//import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
//import android.widget.ListView;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
//import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.anish.sensor_data.MainActivity.studentId;

public class bluetooth_service extends Service {
    private BluetoothAdapter bluetoothAdapter;
    private  Timer timer;
    private BluetoothDevice device;
    private int counter=0;
    private int timesScanned=0;
    private CountDownTimer ctimer = null;
    private Calendar today = Calendar.getInstance(TimeZone.getDefault());
    private int yToday = today.get(Calendar.YEAR);
    private int mToday = today.get(Calendar.MONTH);
    private int dToday = today.get(Calendar.DAY_OF_MONTH);

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    IBinder binder = new bluetooth_service.LocalBinder();
    public class LocalBinder extends Binder {
        public bluetooth_service getInstance(){
            return bluetooth_service.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        startForeground(2, runAsForeground());
        return START_STICKY;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        timesScanned = 0;
        counter = 0;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.i("anish_1", "service started");

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    timesScanned = timesScanned + 1;
                    if(bluetoothAdapter.isDiscovering()){
                        bluetoothAdapter.cancelDiscovery();
                        //Log.i("anish_1", "cancel discover");

                        bluetoothAdapter.startDiscovery();
                        IntentFilter i_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(broadcastreceiver, i_filter);
                        //Log.i("anish_1", "receiver registered");
                    }
                    if(!bluetoothAdapter.isDiscovering()){
                        bluetoothAdapter.startDiscovery();
                        IntentFilter i_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(broadcastreceiver, i_filter);
                    }
                }
            }, 0, 5*60*1000);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        timer.cancel();
        ctimer.cancel();
        unregisterReceiver(broadcastreceiver);
    }

    private Notification runAsForeground(){
        if(Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel1 = new NotificationChannel("notification1", "channel1", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
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

    private BroadcastReceiver broadcastreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            assert action != null;
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                Log.i("anish", "a_found");
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("anish_1", device.getName()+" "+device.getAddress());
                writeToCsv();
            }
        }
    };
    public void writeToCsv(){
        String basedir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String id="";
        if(studentId!=null){
            id = studentId.getString("studId","");
        }
        String day = dToday+"_"+(mToday+1);
        String file_name = day+"_"+id+"_Bluetooth_devices.csv";
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
                String data[] = {"Date", "HH:MM:SS", "Device ID","times scanned","scanning time"};
                writer.writeNext(data);
            }
            Calendar calender = Calendar.getInstance();
            calender.add(Calendar.DATE, 0);
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            String yday = df.format(calender.getTime());
            DateFormat df2 = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
            String ytime = df2.format(calender.getTime());
            String data[] = new String[5];
                data[0] = yday; data[1] = ytime; data[2] = device.getName();data[3]=String.valueOf(timesScanned);data[4]=String.valueOf(counter);
                writer.writeNext(data);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
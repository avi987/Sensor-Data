package com.example.anish.sensor_data;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.os.IBinder;
import android.os.Binder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/*import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;*/
import java.util.*;

import android.content.SharedPreferences.*;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;

public class background_service extends Service implements SensorEventListener {
    public Sensor accelerometer, gyroscope, barometer, microphone, magnetometer, light, proximity;
    public SensorManager sensorManager;
    private float vals[];
    private int x;
    private int i;
    private boolean start;
    private static SharedPreferences unlocks, locks;
    private static SharedPreferences time, timeStat;
    private long unlocks_val, locks_val;
    private long startTime;
    private String maxproxi;
    private static double mEMA = 0.0;
    static final private double e_filter = 0.6;
    private MediaRecorder mrecorder;
    private Calendar today = Calendar.getInstance(TimeZone.getDefault());
    private int yToday = today.get(Calendar.YEAR);
    private int mToday = today.get(Calendar.MONTH);
    private int dToday = today.get(Calendar.DAY_OF_MONTH);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    IBinder binder = new background_service.LocalBinder();

    public class LocalBinder extends Binder {
        public background_service getInstance() {
            return background_service.this;
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, runAsForeground());
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        start = false;
        startTime = MainActivity.startmilli;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");

        intentFilter.setPriority(100);

        registerReceiver(screenReceiver, intentFilter);
        locks = getSharedPreferences("locks", Context.MODE_PRIVATE);
        unlocks = getSharedPreferences("unlocks", Context.MODE_PRIVATE);
        Editor editorLock = locks.edit();
        Editor editor = unlocks.edit();
        editorLock.putString("locks", String.valueOf(0));
        editor.putString("unlocks", String.valueOf(0));
        editorLock.commit();
        editor.commit();
        time = getSharedPreferences("time", Context.MODE_PRIVATE);
        timeStat = getSharedPreferences("sTime", Context.MODE_PRIVATE);
        Editor editor1 = time.edit();
        Editor editor2 = timeStat.edit();
        editor1.putString("time", "");
        editor2.putString("sTime", "");
        editor1.commit();
        editor2.commit();
        locks_val = 0;
        unlocks_val = 0;
        registerSensor();
    }

    public void registerSensor() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, barometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);

        maxproxi = String.valueOf(proximity.getMaximumRange());
        vals = new float[12];
        Arrays.fill(vals, 0);
        x = 0;
        i = 0;
        Log.i("anish_123", "registered");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, barometer);
        sensorManager.unregisterListener(this, light);
        sensorManager.unregisterListener(this, proximity);
        unregisterReceiver(screenReceiver);
        //try{
        //  workbook.close();
        //}
        //catch (Exception e){
        //  e.printStackTrace();
        //}
        Log.i("anish_123", "destroyed");
        try {
            mrecorder.stop();
        } catch (RuntimeException e) {

        } finally {
            mrecorder.release();
            mrecorder = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i("anish_123", "changed");
        if (sensorEvent.sensor == accelerometer) {
            Log.i("anish_123", "a");
            vals[0] = sensorEvent.values[0];
            vals[1] = sensorEvent.values[1];
            vals[2] = sensorEvent.values[2];
        } else if (sensorEvent.sensor == gyroscope) {
            Log.i("anish_123", "g");
            vals[3] = sensorEvent.values[0];
            vals[4] = sensorEvent.values[1];
            vals[5] = sensorEvent.values[2];

        } else if (sensorEvent.sensor == magnetometer) {
            Log.i("anish_123", "m");
            vals[6] = sensorEvent.values[0];
            vals[7] = sensorEvent.values[1];
            vals[8] = sensorEvent.values[2];

        } else if (sensorEvent.sensor == barometer) {
            Log.i("anish_123", "b");
            vals[9] = sensorEvent.values[0];
        } else if (sensorEvent.sensor == light) {
            Log.i("anish_123", "l");
            vals[10] = sensorEvent.values[0];
        } else if (sensorEvent.sensor == proximity) {
            Log.i("anish_123", "p");
            vals[11] = sensorEvent.values[0];
        }
        if (time.getString("time", "").equals("")) {
            try {
                excel_import(vals);
            } catch (Exception e) {
                e.printStackTrace();
            }
            SharedPreferences.Editor editor1 = time.edit();
            editor1.putString("time", String.valueOf(System.currentTimeMillis()));
            editor1.commit();
            Log.i("anish_123", time.getString("time", ""));
        } else {
            long msec = Long.valueOf(time.getString("time", ""));
            long csec = Long.valueOf(System.currentTimeMillis());
            Log.i("anish_123", String.valueOf(msec));
            if (csec - msec >= 10 * 1000) {
                Log.i("anish_123", String.valueOf(msec));
                Log.i("anish_123", String.valueOf(csec));
                try {
                    excel_import(vals);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                SharedPreferences.Editor editor1 = time.edit();
                editor1.putString("time", String.valueOf(System.currentTimeMillis()));
                editor1.commit();
            }
        }

        if (timeStat.getString("sTime", "").equals("")) {
            SharedPreferences.Editor editor2 = timeStat.edit();
            editor2.putString("sTime", String.valueOf(System.currentTimeMillis()));
            editor2.commit();
            Log.i("anish_123", timeStat.getString("sTime", ""));
        } else {
            long msecStat = Long.valueOf(timeStat.getString("sTime", ""));
            long csecStat = Long.valueOf(System.currentTimeMillis());
            Log.i("anish_123", String.valueOf(msecStat));
            if (csecStat - msecStat >= 60*60*1000) {
                Log.i("anish_123", String.valueOf(msecStat));
                Log.i("anish_123", String.valueOf(csecStat));
                Toast.makeText(this, "going to main activity", Toast.LENGTH_LONG).show();
                Intent sendIntent = new Intent(background_service.this, MainActivity.class);
                sendIntent.putExtra("verbose", "123");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(sendIntent);
                SharedPreferences.Editor editor2 = timeStat.edit();
                editor2.putString("sTime", String.valueOf(System.currentTimeMillis()));
                editor2.commit();
            }
        }
    }

    private Notification runAsForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel1 = new NotificationChannel("notification1", "channel1", NotificationManager.IMPORTANCE_HIGH);
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

    BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                locks_val++;
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                unlocks_val++;
            }
            SharedPreferences.Editor editorLock = locks.edit();
            SharedPreferences.Editor editor = unlocks.edit();
            editorLock.putString("locks", String.valueOf(locks_val));
            editor.putString("unlocks", String.valueOf(unlocks_val));
            editorLock.commit();
            editor.commit();
        }
    };

    public static void setUnlocks() {
        SharedPreferences.Editor editorLock = locks.edit();
        SharedPreferences.Editor editor = unlocks.edit();
        editorLock.putString("locks", "0");
        editor.putString("unlocks", "0");
        editorLock.commit();
        editor.commit();
    }

    public void excel_import(float v[]) throws IOException {
        String audio = getAudioDecibels();
        Calendar calender = Calendar.getInstance();
        calender.add(Calendar.DATE, 0);
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String yday = df.format(calender.getTime());
        DateFormat df2 = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
        String ytime = df2.format(calender.getTime());
        Log.i("anish", ytime);
        Log.i("anish_d", yday);
        String basedir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String id = MainActivity.studentId.getString("studId","");
        String day = dToday + "_" + (mToday+1);
        String file_name = day+"_"+id+"_Sensor Data.csv";
        String file_path = basedir + File.separator + file_name;
        File f = new File(file_path);
        CSVWriter writer;
        FileWriter fileWriter;
        if (f.exists() && !f.isDirectory()) {
            fileWriter = new FileWriter(file_path, true);
            writer = new CSVWriter(fileWriter);
        } else {
            writer = new CSVWriter(new FileWriter(file_path));
            String data[] = {"Date", "HH:MM:SS", "Accelerometer_x(m/s^2)", "Accelerometer_y(m/s^2)", "Accelerometer_z(m/s^2)", "Gyroscope_x(rad/s)", "Gyroscope_y(rad/s)", "Gyroscope_z(rad/s)", "Magnetometer_x(uT)", "Magnetometer_y(uT)", "Magnetometer_z(uT)", "Pressure(hPa)", "Light(lux)", "Proximity(0 - " + maxproxi + ")", "locks", "unlocks", "Audio"};
            writer.writeNext(data);
        }
        if (start == false) {
            start = true;
        }
        String data[] = {yday, ytime, String.valueOf(vals[0]), String.valueOf(vals[1]), String.valueOf(vals[2]), String.valueOf(vals[3]), String.valueOf(vals[4]), String.valueOf(vals[5]), String.valueOf(vals[6]), String.valueOf(vals[7]), String.valueOf(vals[8]), String.valueOf(vals[9]), String.valueOf(vals[10]), String.valueOf(vals[11]), locks.getString("locks", "0"), unlocks.getString("unlocks", "0"), audio};
        writer.writeNext(data);
        writer.close();
    }

    private String getAudioDecibels(){
        if(mrecorder == null){
            mrecorder = new MediaRecorder();
            mrecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mrecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mrecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mrecorder.setOutputFile("/dev/null");
            try {
                mrecorder.prepare();
            }
            catch(java.io.IOException ioe){
                ioe.printStackTrace();
            }
            mrecorder.start();
        }

        double aud = mrecorder.getMaxAmplitude();
        //int db = 0;
        //if(aud > 1){
          //  db = (int)(20*Math.log10(aud));
        //}
        //mEMA = e_filter * aud + (1.0 - e_filter) * mEMA;
        if(aud == 0){
            return "0";
        }
        return String.valueOf(aud);
    }
}

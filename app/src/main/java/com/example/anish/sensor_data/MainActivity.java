package com.example.anish.sensor_data;


import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity{
    public static SharedPreferences studentId;
    private Intent st, bl, wi;
    private BluetoothAdapter bluetoothAdapter;
    public static long startmilli;
    public static String stats;
    public String msg,example;
    public int mood = -1;
    private CountDownTimer ctimer = null;
    private AlertDialog alertDialog = null;
    private int nMissed=0, nIncoming=0, nOutgoing =0;
    private boolean stopped = false;
    private static boolean setId = false;
    private String calldet[];
    private Calendar today = Calendar.getInstance(TimeZone.getDefault());
    private int yToday = today.get(Calendar.YEAR);
    private int mToday = today.get(Calendar.MONTH);
    private int dToday = today.get(Calendar.DAY_OF_MONTH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(studentId==null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter Your student ID");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    studentId = getSharedPreferences("studId",MODE_PRIVATE);
                    SharedPreferences.Editor idEditor = studentId.edit();
                    idEditor.putString("studId",input.getText().toString());
                    idEditor.apply();
                }
            });
            builder.show();
        }

        stopped = false;
        startmilli = 0;
        SharedPreferences timeStat = getSharedPreferences("sTime", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor2 = timeStat.edit();
        editor2.putString("sTime","");
        editor2.apply();
        example = "123";
        Intent receiveIntent = getIntent();
        msg = receiveIntent.getStringExtra("verbose");
        if(msg==null){
            msg="000";
        }
        Toast.makeText(this, "the verbose is "+msg, Toast.LENGTH_LONG).show();
        if(msg.equals(example)){
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.activity_prompt, null);
            mood = -1;

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setView(promptsView);
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Submit",new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog,int id) {
                                    cancelTimer();
                                    callStatLog();
                                    msg = null;
                                    mood=-1;
                                    Toast.makeText(MainActivity.this, "called stats log", Toast.LENGTH_SHORT).show();
                                }
                            });

            alertDialog = alertDialogBuilder.create();
            alertDialog.show();

            final ImageButton vsad = alertDialog.findViewById(R.id.vsad);
            final ImageButton sad = alertDialog.findViewById(R.id.sad);
            final ImageButton neutral = alertDialog.findViewById(R.id.neutral);
            final ImageButton happy = alertDialog.findViewById(R.id.happy);
            final ImageButton vhappy = alertDialog.findViewById(R.id.vhappy);

            if(mood==-1){
                (alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                Toast.makeText(MainActivity.this, "Please select your mood", Toast.LENGTH_LONG).show();
            }

            vsad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mood = 1;
                    final Animation myAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce);
                    vsad.startAnimation(myAnim);
                    ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    cancelTimer();
                }
            });
            sad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mood = 2;
                    final Animation myAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce);
                    sad.startAnimation(myAnim);
                    ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    cancelTimer();
                }
            });
            neutral.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mood = 3;
                    final Animation myAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce);
                    neutral.startAnimation(myAnim);
                    ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    cancelTimer();
                }
            });
            happy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mood = 4;
                    final Animation myAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce);
                    happy.startAnimation(myAnim);
                    ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    cancelTimer();
                }
            });
            vhappy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mood = 5;
                    final Animation myAnim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.bounce);
                    vhappy.startAnimation(myAnim);
                    ((AlertDialog) alertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    cancelTimer();
                }
            });
            startTimer();
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        requestAppPermissions();
        //checkSpecialPhones();
    }

    public void start_track(View view){
        Toast.makeText(this, "Started the background processes", Toast.LENGTH_LONG).show();
        startmilli = System.currentTimeMillis();
        st = new Intent(MainActivity.this, background_service.class);
        startService(st);
        if(!bluetoothAdapter.isEnabled()){
            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enable);
        }
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1)
            checkPermissions();
        bl = new Intent(MainActivity.this, bluetooth_service.class);
        startService(bl);
        wi = new Intent(MainActivity.this, wifi_service.class);
        startService(wi);
    }

    public void callStatLog(){
        stats = getForegroundProcesses();
        writeToCsv();
    }

    public void stop_track(View view){
        background_service.setUnlocks();
        calldet = getCallDuration();
        stopped = true;
        callStatLog();
        msg = null;
        mood=-1;
        Toast.makeText(MainActivity.this, "called stats log", Toast.LENGTH_SHORT).show();
        try {
            stopService(st);
        }
        catch(Exception e){

        }
        stopService(bl);
        stopService(wi);
    }


    private String getForegroundProcesses(){
        Calendar calender = Calendar.getInstance();
        calender.add(Calendar.DATE, 0);
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String foreprocess = df.format(calender.getTime()) + "\n" + "\n";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) MainActivity.this.getSystemService(Context.USAGE_STATS_SERVICE);

            Calendar calendar = Calendar.getInstance();

            calendar.add(Calendar.DATE, -1);

            long start = calendar.getTimeInMillis();

            long end = System.currentTimeMillis();
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end);
            HashMap<String, Long> map = new HashMap<>();
            for(int i=0;i<stats.size();i++){
                if(stats.get(i).getTotalTimeInForeground() != 0) {
                    if(map.containsKey(stats.get(i).getPackageName())){
                        long fore = map.get(stats.get(i).getPackageName());
                        fore += stats.get(i).getTotalTimeInForeground();
                        map.remove(stats.get(i).getPackageName());
                        map.put(stats.get(i).getPackageName(), fore);
                    }
                    else{
                        map.put(stats.get(i).getPackageName(), stats.get(i).getTotalTimeInForeground());
                    }
                }
            }
            Set<Map.Entry<String, Long>> set = map.entrySet();
            List<Map.Entry<String, Long>> li = new ArrayList<>(set);
            Collections.sort(li, new Comparator<Map.Entry<String, Long>>() {
                @Override
                public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                    long c1 = o1.getValue();
                    long c2 = o2.getValue();
                    if(c1 <= c2)
                        return -1;
                    else
                        return 1;
                }
            });
            LinkedHashMap<String, Long> lmap = new LinkedHashMap<>();
            for(int i=0;i<li.size();i++){
                lmap.put(li.get(i).getKey(), li.get(i).getValue());
            }
            Iterator<Map.Entry<String, Long>> it = lmap.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String, Long> pair = (Map.Entry<String, Long>) it.next();
                long milli = pair.getValue();
                long secs = (milli/1000);
                long mins = (secs/60);
                long hours = (mins/60);
                String t = String.valueOf(hours)+":"+String.valueOf(mins%60)+":"+String.valueOf(secs%60);
                foreprocess += pair.getKey() + "        " + t+"\n";
            }
        }
        return foreprocess;
    }

    public void writeToCsv(){
        String basedir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String id = studentId.getString("studId","");
        String day = dToday+"_"+(mToday+1);
        String file_name = day+"_"+id+"_Stats.csv";
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
                String data[] = {"DATE", "HH:MM:SS","MOOD","STATS","Call Duration","Call Details"};
                writer.writeNext(data);
            }
            Calendar calender = Calendar.getInstance();
            calender.add(Calendar.DATE, 0);
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            String yday = df.format(calender.getTime());
            DateFormat df2 = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
            String ytime = df2.format(calender.getTime());
            String feeling = "";
            switch(mood){
                case 1:
                    feeling = "Very Sad";
                    break;
                case 2:
                    feeling = "Sad";
                    break;
                case 3:
                    feeling = "Neutral";
                    break;
                case 4:
                    feeling = "Happy";
                    break;
                case 5:
                    feeling = "Very Happy";
                    break;
                case 6:
                    feeling = "--";
            }
            String duration;
            String deets;
            if(stopped){
                duration = String.valueOf(calldet[0]);
                deets = String.valueOf(calldet[1]);
            }
            else{
                duration="-";
                deets="-";
            }
            String data[] = new String[6];
            data[0] = yday; data[1] = ytime;data[2]= feeling;data[3] = stats;data[4]=duration;data[5]=deets;
            writer.writeNext(data);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long convertToMillis(String t){
        long c=0;
        try {
            String time[] = t.split("\\:");
            c = 0;
            c += Integer.valueOf(time[0]) * 60 * 60 * 1000;
            Log.i("anishhh", time[1]);
            c += Integer.valueOf(time[1]) * 60 * 1000;
            c += Integer.valueOf(time[2]) * 1000;
        }
        catch(Exception e){

        }
        return c;
    }

    private String convertToStandard(long m){
        long sec = m/1000;
        long min = sec/60;
        long hour = min/60;

        return String.valueOf(hour%24)+":"+String.valueOf(min%60)+":"+String.valueOf(sec%60);
    }

    private void requestAppPermissions(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            return;
        }
        AppOpsManager appOpps = (AppOpsManager)MainActivity.this.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), MainActivity.this.getPackageName());
        boolean granted;
        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (this.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        if(!granted){
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        if(hasReadPermission() && haswritePermission() && hasCallLogPermission() && hasRecordPermission()){
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.RECORD_AUDIO
                }, 1);
        }

    private boolean hasCallLogPermission(){
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_CALL_LOG)) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasRecordPermission(){
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.RECORD_AUDIO)) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasReadPermission(){
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean haswritePermission(){
        return (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d("anish", "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    private void checkSpecialPhones(){
        if(Build.BRAND.equalsIgnoreCase("xiaomi")){
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(intent);
        }
        else if(Build.BRAND.equalsIgnoreCase("oppo")){
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            startActivity(intent);
        }
    }
    public void startTimer(){
        ctimer = new CountDownTimer(10*60*1000,1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                if(mood==-1){
                    mood = 6;
                }
                callStatLog();
                msg = null;
                Toast.makeText(MainActivity.this, "called stats log", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        };
        ctimer.start();
    }
    public void cancelTimer(){
        if(ctimer!=null){
            ctimer.cancel();
        }
    }

    private String[] getCallDuration() {
        String details[] = new String[2];
        details[0] = "0:0:0";
        details[1] = "-";
        String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
        String timeLimit[] = {String.valueOf(startmilli), String.valueOf(System.currentTimeMillis())};
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        Cursor managedCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, android.provider.CallLog.Calls.DATE + " BETWEEN ? AND ?", timeLimit, strOrder);
//        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        assert managedCursor != null;
        int type;
        type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        long totalDuration=0;
        long hours=0,mins=0,secs=0;
        while (managedCursor.moveToNext()) {
//            String phNumber = managedCursor.getString(number);
            String callType = managedCursor.getString(type);
            String callDate = managedCursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));
            Calendar callDay = Calendar.getInstance();
            callDay.setTime(callDayTime);
            int cYear = callDay.get(Calendar.YEAR);
            int cMonth = callDay.get(Calendar.MONTH);
            int cDay = callDay.get(Calendar.DAY_OF_MONTH);
            if(yToday == cYear && mToday == cMonth && dToday == cDay){
                totalDuration = totalDuration + Long.valueOf(managedCursor.getString(duration));
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE: nOutgoing = nOutgoing + 1;
                        break;
                    case CallLog.Calls.INCOMING_TYPE: nIncoming = nIncoming +1;
                        break;
                    case CallLog.Calls.MISSED_TYPE: nMissed = nMissed +1;
                        break;
                }
                secs = totalDuration;
                mins = secs/60;
                hours = mins/60;
            }
        }
        details[0] = String.valueOf(hours%24) + ":" + String.valueOf(mins%60) + ":" + String.valueOf(secs%60);
        details[1] = "Incoming Calls : "+String.valueOf(nIncoming)+"\nOutgoing Calls : "+String.valueOf(nOutgoing)+"\nMissed Calls : "+String.valueOf(nMissed);
        managedCursor.close();
        return details;
    }
}


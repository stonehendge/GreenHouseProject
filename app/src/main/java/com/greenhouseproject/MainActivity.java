package com.greenhouseproject;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsMessage;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.lang.System;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_UPD = "com.greenhouseproject.ACTION_UPD";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    private static final int DISPLAY_DATA = 1;
    final String SAVED_TEL = "TNumber";

    Button buttonStart;
    Button buttonStop;
    Button buttonStatus;
    Button buttonSettings;
    Button buttonPumpOff;
    TextView textViewTemp;
    TextView textViewHumAir;
    TextView textViewHumSoil;
    TextView textViewPumpStat;
    TextView textViewBarrelStat;
    TextView textViewTimer;
    String phoneNo;//=loadTelNumber();
    String message;

    SharedPreferences sPref;

    private Timer mTimer;
    TimerTaskUpd mTimerTaskUpd;
    public static long starttime;

    BroadcastReceiver broadcastReceiver;
    public BroadcastReceiver broadcastReceiver_UPD;
   // this.registerReceiver(broadcastReceiver_UPD, new IntentFilter(ACTION_UPD));

    Handler mHandler  = new Handler();/*{
        @Override
        public void handleMessage(Message msg) {
            // Do task here
            if (msg.what == DISPLAY_DATA)
            {
                setContentView(R.layout.activity_main);
                final String strTime = String.format ("%d", System.currentTimeMillis()-starttime);
                textViewTimer.setText(textViewTimer+"\t\t\t"+strTime);
            }//displayData();
        }
    };*/
    TimerTask TimerTaskUpdateUI;

    /* = new TimerTask() {
        @Override
        public void run() {
            // post a runnable to the handler
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //setContentView(R.layout.activity_main);
                    final String strTime = String.format ("%d", (System.currentTimeMillis()-starttime)/1000);
                    textViewTimer.setText("Полив "+"\t\t\t\t"+strTime+" cек");
                }
            });
        }
    };*/

    String loadTelNumber() {
        sPref = getSharedPreferences(SAVED_TEL,MODE_PRIVATE);
        String TN = sPref.getString(SAVED_TEL, "");
        String out="";
        if (TN.equals(""))
        {
            buttonStart.setEnabled(false);
            buttonStop.setEnabled(false);
            buttonStatus.setEnabled(false);
        }
        else
        {
            out="+7"+TN;
            buttonStart.setEnabled(true);
            buttonStop.setEnabled(true);
            buttonStatus.setEnabled(true);
        }
        return out;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });


        textViewTemp = (TextView) findViewById(R.id.textViewTemp);
        textViewHumAir = (TextView) findViewById(R.id.textViewHumAir);
        textViewHumSoil = (TextView) findViewById(R.id.textViewHumSoil);
        textViewPumpStat = (TextView) findViewById(R.id.textViewPumpStat);
        textViewBarrelStat = (TextView) findViewById(R.id.textViewBarrelStat);
        textViewTimer = (TextView) findViewById(R.id.textViewTimer);

        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonStatus = (Button) findViewById(R.id.buttonStatus);
        buttonSettings = (Button) findViewById(R.id.buttonSettings);
        buttonPumpOff = (Button) findViewById(R.id.buttonPumpOff);

        phoneNo=loadTelNumber();

        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                phoneNo=loadTelNumber();
                if(phoneNo.equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Не задан номер телефона в настройках", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendSmsByManager(phoneNo,"PumpGreenHouseOn");
                if (mTimer != null) {
                    mTimer.cancel();
                }
                mTimer = new Timer();
                TimerTaskUpdUI mTimerTaskUpd = new TimerTaskUpdUI();
                starttime = System.currentTimeMillis();
                mTimer.schedule(mTimerTaskUpd, 1000,1000);

                //mHandler.sendEmptyMessageDelayed(DISPLAY_DATA, 1000);
            }
        });

        buttonPumpOff.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                phoneNo=loadTelNumber();
                if(phoneNo.equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Не задан номер телефона в настройках", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendSmsByManager(phoneNo,"GreenHousePumpOff");

            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                phoneNo=loadTelNumber();
                if(phoneNo.equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Не задан номер телефона в настройках", Toast.LENGTH_SHORT).show();
                    return;
                }
                // sendSMSMessage("+79655456399","GreenHousePumpOn");
                // sendSmsByManager("+79655456399","GreenHousePumpOn");
                sendSmsByManager("+79655456399","PumpGreenHouseOff");
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                textViewTimer.setText("Время полива");
            }
        });

        buttonStatus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                phoneNo=loadTelNumber();
                if(phoneNo.equals(""))
                {
                    Toast.makeText(getApplicationContext(), "Не задан номер телефона в настройках", Toast.LENGTH_SHORT).show();
                    return;
                }
                // sendSMSMessage("+79655456399","GreenHousePumpOn");
                // sendSmsByManager("+79655456399","GreenHousePumpOn");
                sendSmsByManager("+79655456399","PumpGreenHouseStatus");

                /*mTimer = new Timer();
                TimerTaskUpd mTimerTaskUpd = new TimerTaskUpd();
                starttime = System.currentTimeMillis();
                mTimer.schedule(mTimerTaskUpd, 1000);*/
            }
        });

        buttonSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent modifySettings=new Intent(MainActivity.this,SetActivity.class);
                startActivity(modifySettings);

            }
        });
        //BroadcastReceiver
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {


                // Toast.makeText(context,"Broadcast Received in Activity called ",Toast.LENGTH_SHORT).show();
                if (intent.getAction().equals(SMS_RECEIVED)) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        // get sms objects
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        if (pdus.length == 0) {
                            return;
                        }
                        // large message might be broken into many
                        SmsMessage[] messages = new SmsMessage[pdus.length];
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < pdus.length; i++) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            sb.append(messages[i].getMessageBody());
                        }
                        String sender = messages[0].getOriginatingAddress();
                        String message = sb.toString();

                        String[] array = message.split(",", -1);

                        for(int j = 0; j < array.length; j++) {
                            String i_str = array[j];

                            if (i_str.startsWith("T:"))
                            {
                                textViewTemp.setText( "Температура"+"\t\t\t\t"+i_str.substring(2));
                                //(TextView) TVTemp = (TextView) findViewById(R.id.textViewTemp);
                                //Toast.makeText(context, "Temperature="+i_str.substring(2), Toast.LENGTH_SHORT).show();
                            }
                            if (i_str.startsWith("H:"))
                            {
                                textViewHumAir.setText( "Влажность воздуха"+"\t\t\t\t"+i_str.substring(2));
                                //Toast.makeText(context, "Humidity Air="+i_str.substring(2), Toast.LENGTH_SHORT).show();
                            }
                            if (i_str.startsWith("SM:"))
                            {
                                String State="";
                                if(Integer.parseInt(i_str.substring(3))>=500)
                                {
                                    State="Сухо";
                                }
                                if(Integer.parseInt(i_str.substring(3))<=300)
                                {
                                    State="Влажно";
                                }
                                if(Integer.parseInt(i_str.substring(3))>300 && Integer.parseInt(i_str.substring(3))<500)
                                {
                                    State="Норма";
                                }
                                textViewHumSoil.setText( "Влажность почвы"+"\t\t\t\t"+i_str.substring(3)+" "+State);
                                //Toast.makeText(context, "Soil Moisture="+i_str.substring(3), Toast.LENGTH_SHORT).show();
                            }
                            if (i_str.startsWith("PS:"))
                            {
                                String PumpStatus = "";
                                if(i_str.substring(3).equals( "0"))
                                {
                                    PumpStatus="Выключен";
                                }
                                if(i_str.substring(3).equals( "1"))
                                {
                                    PumpStatus="Включен";
                                }
                                textViewPumpStat.setText( "Состояние насоса"+"\t\t\t\t"+PumpStatus);
                                //Toast.makeText(context, "Soil Moisture="+i_str.substring(3), Toast.LENGTH_SHORT).show();
                            }
                            if (i_str.startsWith("WL:"))
                            {
                                String WaterLevelStatus = "";
                                if(i_str.substring(3).equals( "1"))
                                {
                                    WaterLevelStatus="Полная";
                                }
                                if(i_str.substring(3).equals( "2") || i_str.substring(3).equals( "3"))
                                {
                                    WaterLevelStatus="Не полная";
                                }
                                textViewBarrelStat.setText( "Состояние бочки"+"\t\t\t\t"+WaterLevelStatus);
                                //Toast.makeText(context, "Soil Moisture="+i_str.substring(3), Toast.LENGTH_SHORT).show();
                            }
                        }

                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        // prevent any other broadcast receivers from receiving broadcast
                        // abortBroadcast();
                    }
                }
            }


        };

        //=====================================

        broadcastReceiver_UPD = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {


                // Toast.makeText(context,"Broadcast Received in Activity called ",Toast.LENGTH_SHORT).show();
                if (intent.getAction().equals(ACTION_UPD)) {
                    //Bundle bundle = intent.getExtras();
                    buttonStart.setEnabled(true);
                    buttonStop.setEnabled(true);
                    buttonStatus.setEnabled(true);
                }
            }


        };

        registerReceiver(broadcastReceiver_UPD, new IntentFilter(ACTION_UPD));
        //=================================
//        IntentFilter filter = new IntentFilter();
//        // specify the action to which receiver will listen
//        filter.addAction("SMS_RECEIVED");
//        registerReceiver(broadcastReceiver,filter);
        registerReceiver(broadcastReceiver, new IntentFilter(SMS_RECEIVED));


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(broadcastReceiver_UPD);
    }
  /*  private BroadcastReceiver reciever = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
          *//*  if (intent.getAction().equals(SMS_RECEIVED)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    // get sms objects
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    if (pdus.length == 0) {
                        return;
                    }
                    // large message might be broken into many
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < pdus.length; i++) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        sb.append(messages[i].getMessageBody());
                    }
                    String sender = messages[0].getOriginatingAddress();
                    String message = sb.toString();

                    String[] array = message.split(",", -1);

                    for(int j = 0; j < array.length; j++) {
                        String i_str = array[j];

                        if (i_str.startsWith("T:"))
                        {
                            textViewTemp.setText( textViewTemp.getText()+"      "+i_str.substring(2));
                            //(TextView) TVTemp = (TextView) findViewById(R.id.textViewTemp);
                            Toast.makeText(context, "Temperature="+i_str.substring(2), Toast.LENGTH_SHORT).show();
                        }
                        if (i_str.startsWith("H:"))
                        {
                            Toast.makeText(context, "Humidity Air="+i_str.substring(2), Toast.LENGTH_SHORT).show();
                        }
                        if (i_str.startsWith("SM:"))
                        {
                            Toast.makeText(context, "Soil Moisture="+i_str.substring(3), Toast.LENGTH_SHORT).show();
                        }
                    }

                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    // prevent any other broadcast receivers from receiving broadcast
                    // abortBroadcast();
                }
            }*//*
        }
    };*/

    protected void sendSMSMessage(String phone, String msg) {
        phoneNo = phone;//txtphoneNo.getText().toString();
        message = msg;//txtMessage.getText().toString();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }

    public void sendSmsByManager(String phoneNumber,String smsBody ) {
        try {
            // Get the default instance of the SmsManager
            SmsManager smsManager = SmsManager.getDefault();
            PendingIntent sentPI;
            String SENT = "SMS_SENT";

            sentPI = PendingIntent.getBroadcast(this, 0,new Intent(SENT), 0);

            //sms.sendTextMessage(phoneNumber, null, message, sentPI, null);

            smsManager.sendTextMessage(phoneNumber,
                    null,
                    smsBody,
                    sentPI,
                    null);
            Toast.makeText(getApplicationContext(), "Your sms has successfully sent!",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),"Your sms has failed..." + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS failed, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
      /*  if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }


    class TimerTaskUpdUI extends TimerTask {

        @Override
        public void run() {
            // post a runnable to the handler
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //setContentView(R.layout.activity_main);
                    final String strTime = String.format ("%d", (System.currentTimeMillis()-starttime)/1000);
                    textViewTimer.setText("Полив "+"\t\t\t\t"+strTime+" cек");
                }
            });
        }
    }

    class TimerTaskUpd extends TimerTask {

        @Override
        public void run() {


            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setContentView(R.layout.activity_main);
                    final String strTime = String.format ("%d", System.currentTimeMillis()-starttime);
                    textViewTimer.setText(textViewTimer+"\t\t\t"+strTime);
                }
            });

//            setContentView(R.layout.activity_main);
//            final String strTime = String.format ("%d", System.currentTimeMillis()-starttime);
//            textViewTimer.setText(textViewTimer+"\t\t\t"+strTime);
           /* Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "dd:MMMM:yyyy HH:mm:ss a", Locale.getDefault());
            final String strDate = simpleDateFormat.format(calendar.getTime());

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mCounterTextView.setText(strDate);
                }
            });*/
        }
    }
}

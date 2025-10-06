package com.example.myapplication2.Register;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.DataCaptureService;
import com.example.myapplication2.R;
import com.example.myapplication2.Settings.SettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.Calendar;

import pl.droidsonroids.gif.GifImageView;

public class Register7Activity extends AppCompatActivity {
    private static final String TAG = "Register7";
    private DatabaseReference mDatabase;
    private ConectInsole conectar;
    private ConectInsole2 conectar2;
    RegisterActivity register;
    private SharedPreferences sharedPreferences;
    private Calendar calendar;
    private Intent serviceIntent;
    private String followInRight, followInLeft,re;
    private short S1, S2, S3, S4, S5, S6, S7, S8, S9;
    private byte freq = 1;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Boolean verificar;
    private TextView instruct;
    private GifImageView gifimagewait;
    Boolean foot = false;
    private String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: start Register7Activity");
        setContentView(R.layout.activity_register7_1);

        serviceIntent = new Intent(this, DataCaptureService.class);
        sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        followInRight = sharedPreferences.getString("Sright", "default");
        followInLeft  = sharedPreferences.getString("Sleft",  "default");
        Log.d(TAG, "onCreate: followInRight=" + followInRight + ", followInLeft=" + followInLeft);

        conectar  = new ConectInsole(this);
        conectar2 = new ConectInsole2(this);
        register = new RegisterActivity();

        Log.d(TAG, "onCreate: ConectInsole initialized");
        Log.d(TAG, "onCreate: ConectInsole2 initialized");

        ImageView positioninsole = findViewById(R.id.imageinstructionsinsole);
        Button mTest1 = findViewById(R.id.buttontestinsole1);
        S1 = S2 = S3 = S4 = S5 = S6 = S7 = S8 = S9 = 0x1FFF;

        positioninsole.setImageResource(R.drawable.positioninsole);

        mTest1.setOnClickListener(v -> {
            Log.d(TAG, "mTest1 clicked");
            setContentView(R.layout.activity_register7_2);
            instruct    = findViewById(R.id.inst_connection);
            Button mNext7Btn = findViewById(R.id.btnNext7);
            mNext7Btn.setVisibility(View.GONE);
            instruct.setText("Permaneça de pé. Aguarde enquanto coletamos alguns dados importantes.");

            sendCommand((byte) 0x3A, freq);
            Log.d(TAG, "Initial sendCommand cmd=0x3A");

            if ("true".equals(followInRight)) {
                handler.postDelayed(() -> {
                    Log.d(TAG, "handleStopCommand scheduled for right");
                    handleStopCommand((byte) 0x3B, freq);
                }, 10000);
            }
            if ("true".equals(followInLeft)) {
                handler.postDelayed(() -> {

                    Log.d(TAG, "handleStopCommand2 scheduled for left");
                    handleStopCommand2((byte) 0x3B, freq);
                }, 10000);
            }

            handler.postDelayed(this::execute_nextlayout, 20000);
        });
        Log.d(TAG, "onCreate: UI initialized");
    }

    private void execute_nextlayout() {
        Log.d(TAG, "execute_nextlayout: start");
        Button mNext7Btn   = findViewById(R.id.btnNext7);
        gifimagewait = findViewById(R.id.gifimage);
        instruct.setText("Pronto! Podemos prosseguir.");
        mNext7Btn.setVisibility(View.VISIBLE);
        gifimagewait.setVisibility(View.GONE);

        mNext7Btn.setOnClickListener(v -> {
            sharedPreferences = getSharedPreferences("reconfigurar", MODE_PRIVATE);
            re= String.valueOf(sharedPreferences.getBoolean("reconfigurar",false));

            if (re.equals("true")){
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    userId = user.getUid();  // Pega o UID do usuário logado
                    mDatabase = FirebaseDatabase.getInstance("https://bioapp-496ae-default-rtdb.firebaseio.com/")
                            .getReference()
                            .child("Users")
                            .child(userId);  // Salvar dados no nó "Users/{UID}"
                register.saveUserData2(userId);
            }
            else {
                Log.d(TAG, "Next7 button clicked");
                verificar = false;
                SharedPreferences.Editor editor = getSharedPreferences("My_Appcalibrar", MODE_PRIVATE).edit();
                editor.putBoolean("verificar", verificar);
                editor.apply();
                startActivity(new Intent(getApplicationContext(), Register6Activity.class));
            }

        });
    }

    private void sendCommand(byte cmd, byte freq) {
        Log.d(TAG, "sendCommand: cmd=" + String.format("0x%02X", cmd) + ", freq=" + freq);
        S1 = S2 = S3 = S4 = S5 = S6 = S7 = S8 = S9 = 0x1FFF;
        if ("true".equals(followInRight)) {
            Log.d(TAG, "sendCommand: sending to ConectInsole");
            conectar.createAndSendConfigData(cmd, freq, S1,S2,S3,S4,S5,S6,S7,S8,S9);
        }
        if ("true".equals(followInLeft)) {
            Log.d(TAG, "sendCommand: sending to ConectInsole2");
            conectar2.createAndSendConfigData(cmd, freq, S1,S2,S3,S4,S5,S6,S7,S8,S9);
        }
    }

    public void handleStopCommand(byte cmd, byte freq) {
        Log.d(TAG, "handleStopCommand: cmd=" + String.format("0x%02X", cmd));
        sendCommand(cmd, freq);
        handler.postDelayed(() -> {
            Log.d(TAG, "handleStopCommand: invoking conectar.receiveData");
            conectar.receiveData(this);
        }, 500);
        handler.postDelayed(() -> {
            Log.d(TAG, "handleStopCommand: stopping service and processing data");
            processReceivedData(conectar);
        }, 1050);
    }

    public void handleStopCommand2(byte cmd, byte freq) {
        Log.d(TAG, "handleStopCommand2: cmd=" + String.format("0x%02X", cmd));
        sendCommand(cmd, freq);
        handler.postDelayed(() -> {
            Log.d(TAG, "handleStopCommand2: invoking conectar2.receiveData");
            conectar2.receiveData(this);
        }, 500);
        handler.postDelayed(() -> {
            Log.d(TAG, "handleStopCommand2: processing data2");
            processReceivedData2(conectar2);
        }, 1050);
    }

    private void processReceivedData(@NonNull ConectInsole insole) {
        Log.d(TAG, "processReceivedData: start");
        String Sright = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE).getString("Sright","default");
        Log.d(TAG, "processReceivedData: Sright=" + Sright);
        if ("true".equals(Sright)) {
            Log.d(TAG, "processReceivedData: processing right insole readings");
            foot = false;
            sharedPreferences = getSharedPreferences("My_Appinsolereadings", MODE_PRIVATE);
            String[] sensorKeys = {"S1_1","S2_1","S3_1","S4_1","S5_1","S6_1","S7_1","S8_1","S9_1"};
            short[][] sensorReadings = new short[9][];
            for (int i=0; i<9; i++) {
                String data = sharedPreferences.getString(sensorKeys[i], "[0,0]");
                sensorReadings[i] = stringToShortArray(data);
                Log.d(TAG, "processReceivedData: " + sensorKeys[i] + "=" + Arrays.toString(sensorReadings[i]));
            }
            short[] limS = thresholdSensors_steady(sensorReadings, foot);
            Log.d(TAG, "processReceivedData: thresholds=" + Arrays.toString(limS));
            byte cmd1 = 0x2A;
            insole.createAndSendConfigData(cmd1, freq,
                    limS[0],limS[1],limS[2],limS[3],limS[4],limS[5],limS[6],limS[7],limS[8]);
            saveConfigData1ToPrefs(limS[0],limS[1],limS[2],limS[3],limS[4],limS[5],limS[6],limS[7],limS[8]);
            SharedPreferences.Editor editor = getSharedPreferences("Treshold_insole1",MODE_PRIVATE).edit();
            for (int i=0; i<9; i++) editor.putInt("Lim"+(i+1)+"I1", limS[i]);
            editor.apply();
            Log.d(TAG, "processReceivedData: saved thresholds prefs");
        }
    }

    private void processReceivedData2(@NonNull ConectInsole2 insole) {
        Log.d(TAG, "processReceivedData2: start");
        String Sleft = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE).getString("Sleft","default");
        Log.d(TAG, "processReceivedData2: Sleft=" + Sleft);
        if ("true".equals(Sleft)) {
            Log.d(TAG, "processReceivedData2: processing left insole readings");
            foot = true;
            sharedPreferences = getSharedPreferences("My_Appinsolereadings2", MODE_PRIVATE);
            String[] sensorKeys = {"S1_2","S2_2","S3_2","S4_2","S5_2","S6_2","S7_2","S8_2","S9_2"};
            short[][] sensorReadings = new short[9][];
            for (int i=0; i<9; i++) {
                sensorReadings[i] = stringToShortArray(sharedPreferences.getString(sensorKeys[i],"[0,0]"));
                Log.d(TAG, "processReceivedData2: " + sensorKeys[i] + "=" + Arrays.toString(sensorReadings[i]));
            }
            short[] limS = thresholdSensors_steady(sensorReadings, foot);
            Log.d(TAG, "processReceivedData2: thresholds=" + Arrays.toString(limS));
            byte cmd1 = 0x2A;
            insole.createAndSendConfigData(cmd1, freq, limS[0],limS[1],limS[2],limS[3],limS[4],limS[5],limS[6],limS[7],limS[8]);
            saveConfigData2ToPrefs(limS[0],limS[1],limS[2],limS[3],limS[4],limS[5],limS[6],limS[7],limS[8]);
            SharedPreferences.Editor editor = getSharedPreferences("Treshold_insole2",MODE_PRIVATE).edit();
            for (int i=0; i<9; i++) editor.putInt("Lim"+(i+1)+"I2", limS[i]);
            editor.apply();
            Log.d(TAG, "processReceivedData2: saved thresholds prefs");
        }
    }

    @NonNull
    public short[] thresholdSensors_steady(short[][] sensorReadings, boolean whichfoot) {
        Log.d(TAG, "thresholdSensors_steady: start for foot=" + whichfoot);
        short[] limS = new short[9];
        String[] hex = new String[9];
        for (int i=0; i<9; i++) {
            limS[i] = (short) getMean(sensorReadings[i]);
            hex[i] = Integer.toHexString(limS[i]);
        }
        Log.d(TAG, "thresholdSensors_steady: means hex=" + Arrays.toString(hex));
        SharedPreferences regionsPref = getSharedPreferences("My_Appregions", MODE_PRIVATE);
        for (int i=0; i<9; i++) {
            boolean region = regionsPref.getBoolean((whichfoot?"S"+(i+1):"S"+(i+1)+"r"), false);
            short hexS = (short) Integer.parseInt(hex[i], 16);
            limS[i] = (short) (((region?0x3:0x1) << 12) | (hexS & 0xFFF));
            Log.d(TAG, "thresholdSensors_steady: sensor"+(i+1)+" region="+region+" limS="+limS[i]);
        }
        return limS;
    }

    private int getMean(@NonNull short[] values) {
        int sum=0;
        for (short v: values) sum += v;
        int mean = values.length>0? sum/values.length : 0;
        Log.d(TAG, "getMean: mean="+mean+" from values length="+values.length);
        return mean;
    }

    public short[] stringToShortArray(String str) {
        Log.d(TAG, "stringToShortArray: raw="+str);
        str = str.replaceAll("[\\[\\]\\s]", "");
        String[] parts = str.split(",");
        short[] arr = new short[parts.length];
        for (int i=0; i<parts.length; i++) arr[i] = Short.parseShort(parts[i]);
        Log.d(TAG, "stringToShortArray: parsed="+Arrays.toString(arr));
        return arr;
    }

    private void saveConfigData1ToPrefs(int S1,int S2,int S3,int S4,int S5,int S6,int S7,int S8,int S9) {
        Log.d(TAG, "saveConfigData1ToPrefs: values="+Arrays.toString(new int[]{S1,S2,S3,S4,S5,S6,S7,S8,S9}));
        SharedPreferences.Editor editor = getSharedPreferences("ConfigPrefs1", Context.MODE_PRIVATE).edit();
        editor.putInt("S1", S1);
        editor.putInt("S2", S2);
        editor.putInt("S3", S3);
        editor.putInt("S4", S4);
        editor.putInt("S5", S5);
        editor.putInt("S6", S6);
        editor.putInt("S7", S7);
        editor.putInt("S8", S8);
        editor.putInt("S9", S9);
        editor.apply();
    }

    private void saveConfigData2ToPrefs(int S1,int S2,int S3,int S4,int S5,int S6,int S7,int S8,int S9) {
        Log.d(TAG, "saveConfigData2ToPrefs: values="+Arrays.toString(new int[]{S1,S2,S3,S4,S5,S6,S7,S8,S9}));
        SharedPreferences.Editor editor = getSharedPreferences("ConfigPrefs2", Context.MODE_PRIVATE).edit();
        editor.putInt("S1", S1);
        editor.putInt("S2", S2);
        editor.putInt("S3", S3);
        editor.putInt("S4", S4);
        editor.putInt("S5", S5);
        editor.putInt("S6", S6);
        editor.putInt("S7", S7);
        editor.putInt("S8", S8);
        editor.putInt("S9", S9);
        editor.apply();
    }
}

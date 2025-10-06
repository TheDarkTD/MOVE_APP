package com.example.myapplication2.Home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.AppForegroundService;
import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.Connection.ConnectionActivity;
import com.example.myapplication2.Data.DataActivity;
import com.example.myapplication2.DataCaptureService;
import com.example.myapplication2.HeatMapViewL;
import com.example.myapplication2.HeatMapViewR;
import com.example.myapplication2.R;
import com.example.myapplication2.Settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    private SharedPreferences sharedPreferences;
    private FloatingActionButton mPopBtn;
    private FrameLayout frameL, frameR;
    private HeatMapViewL heatmapViewL;
    private HeatMapViewR heatmapViewR;
    private ImageView maskL, maskR;
    private Button mBtnRead;
    private float[] lastLeituraR = null;
    private float[] lastLeituraL = null;
    Switch att;
    private String followInRight, followInLeft;
    private short S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1;
    private short S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2;
    private List<HeatMapViewL.SensorRegionL> sensoresL = new ArrayList<>();
    private List<HeatMapViewR.SensorRegionR> sensoresR = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Log.d(TAG, "onCreate: entered");

        sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        followInRight = sharedPreferences.getString("Sright", "default");
        followInLeft = sharedPreferences.getString("Sleft", "default");
        Log.d(TAG, "onCreate: followInRight=" + followInRight + ", followInLeft=" + followInLeft);


        heatmapViewL = findViewById(R.id.heatmapViewL);
        heatmapViewR = findViewById(R.id.heatmapViewR);
        maskL = findViewById(R.id.imageView5);
        maskR = findViewById(R.id.imageView8);
        frameL = findViewById(R.id.frameL);
        frameR = findViewById(R.id.frameR);

        Log.d(TAG, "onCreate: views initialized");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: entered");

        // Start services
        startService(new Intent(this, AppForegroundService.class));
        startService(new Intent(this, DataCaptureService.class));
        Log.d(TAG, "onStart: Services started");

        byte freq = 1;
        byte cmd = 0x3A;
        ConectInsole conectar = new ConectInsole(this);
        ConectInsole2 conectar2 = new ConectInsole2(this);

        if ("false".equals(followInLeft)) {
            heatmapViewL.setVisibility(View.GONE);
            maskL.setVisibility(View.GONE);
            frameL.setVisibility(View.GONE);
            Log.d(TAG, "onStart: Left insole hidden");
        }
        if ("false".equals(followInRight)) {
            heatmapViewR.setVisibility(View.GONE);
            maskR.setVisibility(View.GONE);
            frameR.setVisibility(View.GONE);
            Log.d(TAG, "onStart: Right insole hidden");
        }

        // Load thresholds
        sharedPreferences = getSharedPreferences("ConfigPrefs1", MODE_PRIVATE);
        short[] rightThresh = new short[]{
                S1_1 = (short) sharedPreferences.getInt("S1", 0xffff),
                S2_1 = (short) sharedPreferences.getInt("S2", 0xffff),
                S3_1 = (short) sharedPreferences.getInt("S3", 0xffff),
                S4_1 = (short) sharedPreferences.getInt("S4", 0xffff),
                S5_1 = (short) sharedPreferences.getInt("S5", 0xffff),
                S6_1 = (short) sharedPreferences.getInt("S6", 0xffff),
                S7_1 = (short) sharedPreferences.getInt("S7", 0xffff),
                S8_1 = (short) sharedPreferences.getInt("S8", 0xffff),
                S9_1 = (short) sharedPreferences.getInt("S9", 0xffff)
        };
        Log.d(TAG, "onStart: Right thresholds=" + Arrays.toString(rightThresh));

        sharedPreferences = getSharedPreferences("ConfigPrefs2", MODE_PRIVATE);
        short[] leftThresh = new short[]{
                S1_2 = (short) sharedPreferences.getInt("S1", 0xffff),
                S2_2 = (short) sharedPreferences.getInt("S2", 0xffff),
                S3_2 = (short) sharedPreferences.getInt("S3", 0xffff),
                S4_2 = (short) sharedPreferences.getInt("S4", 0xffff),
                S5_2 = (short) sharedPreferences.getInt("S5", 0xffff),
                S6_2 = (short) sharedPreferences.getInt("S6", 0xffff),
                S7_2 = (short) sharedPreferences.getInt("S7", 0xffff),
                S8_2 = (short) sharedPreferences.getInt("S8", 0xffff),
                S9_2 = (short) sharedPreferences.getInt("S9", 0xffff)
        };
        Log.d(TAG, "onStart: Left thresholds=" + Arrays.toString(leftThresh));

        if ("true".equals(followInRight)) {
            conectar.createAndSendConfigData(cmd, freq, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);
            Log.d(TAG, "onStart: Config sent to right insole");
        }
        if ("true".equals(followInLeft)) {
            conectar2.createAndSendConfigData(cmd, freq, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);
            Log.d(TAG, "onStart: Config sent to left insole");
        }


        Log.d(TAG, "onStart: Heatmaps initialized");

        BottomNavigationView nav = findViewById(R.id.bottomnavview1);
        nav.setSelectedItemId(R.id.home);
        nav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    Log.d(TAG, "Nav: Home"); return true;
                case R.id.settings:
                    Log.d(TAG, "Nav: Settings"); startActivity(new Intent(this, SettingsActivity.class)); finish(); return true;
                case R.id.connection:
                    Log.d(TAG, "Nav: Connection"); startActivity(new Intent(this, ConnectionActivity.class)); finish(); return true;
                case R.id.data:
                    Log.d(TAG, "Nav: Data"); startActivity(new Intent(this, DataActivity.class)); finish(); return true;
            }
            return false;
        });
        att = findViewById(R.id.switchatt);

// Handlers para repetir a execução
        Handler handlerRight = new Handler(Looper.getMainLooper());
        Handler handlerLeft = new Handler(Looper.getMainLooper());

        Runnable runnableRight = new Runnable() {
            @Override
            public void run() {
                if ("true".equals(followInRight) && att.isChecked()) {
                    byte cmd3c = 0x3C;

                    Log.d(TAG, "onStart: Right thresholds=" + Arrays.toString(rightThresh));
                    Log.d(TAG, "ReadBtn: send read cmd to right");
                    conectar.createAndSendConfigData(cmd3c, freq, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);

                    handlerRight.postDelayed(() -> {
                        Log.d(TAG, "ReadBtn: received data from right");
                        conectar.receiveData(HomeActivity.this);
                        loadColorsR();
                        conectar.createAndSendConfigData(cmd, freq, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);
                    }, 250); // atraso interno entre enviar e receber

                    handlerRight.postDelayed(this, 1500);
                }
            }
        };

        Runnable runnableLeft = new Runnable() {
            @Override
            public void run() {
                if ("true".equals(followInLeft) && att.isChecked()) {
                    byte cmd3c = 0x3C;

                    Log.d(TAG, "onStart: Left thresholds=" + Arrays.toString(leftThresh));
                    Log.d(TAG, "ReadBtn: send read cmd to left");
                    conectar2.createAndSendConfigData(cmd3c, freq, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);

                    handlerLeft.postDelayed(() -> {
                        Log.d(TAG, "ReadBtn: received data from left");
                        conectar2.createAndSendConfigData(cmd, freq, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);
                        loadColorsL();
                    }, 250);

                    handlerLeft.postDelayed(this, 1500);
                }
            }
        };

        att.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.d(TAG, "Switch ON");
                Toast.makeText(HomeActivity.this, "iniciando atualização", Toast.LENGTH_SHORT).show();

                // inicia os loops se necessário
                if ("true".equals(followInRight)) {
                    handlerRight.post(runnableRight);
                }
                if ("true".equals(followInLeft)) {
                    handlerLeft.post(runnableLeft);
                }
            } else {
                Log.d(TAG, "Switch OFF");
                Toast.makeText(HomeActivity.this, "atualizaçao desligada ", Toast.LENGTH_SHORT).show();

                // interrompe os loops
                handlerRight.removeCallbacks(runnableRight);
                handlerLeft.removeCallbacks(runnableLeft);
            }
        });

        mPopBtn = findViewById(R.id.floatingActionButton2);
        mPopBtn.setOnClickListener(v -> {
            Log.d(TAG, "PopBtn: Open Pop");
            startActivity(new Intent(this, Pop.class));
        });

        mBtnRead = findViewById(R.id.buttonread);
        mBtnRead.setOnClickListener(v -> {
            Log.d(TAG, "ReadBtn: request readings");
            byte cmd3c = 0x3C;
            if ("true".equals(followInRight)) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "onStart: Right thresholds=" + Arrays.toString(rightThresh));
                    Log.d(TAG, "ReadBtn: send read cmd to right");
                    conectar.createAndSendConfigData(cmd3c, freq, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);
                }, 100);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "ReadBtn: received data from right");
                    conectar.receiveData(this);
                    loadColorsR();
                    conectar.createAndSendConfigData(cmd, freq, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);
                }, 250);
            }
            if ("true".equals(followInLeft)) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "ReadBtn: send read cmd to left");
                    Log.d(TAG, "onStart: Left thresholds=" + Arrays.toString(leftThresh));
                    conectar2.createAndSendConfigData(cmd3c, freq, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);
                }, 100);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "ReadBtn: received data from left");

                    conectar2.createAndSendConfigData(cmd, freq, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);
                    loadColorsL();
                }, 250);
            }
        });
        if ("true".equals(followInLeft)) {
            loadColorsL();

        }
        if ("true".equals(followInRight)) {
            loadColorsR();
        }
        Insole_RightIP();
        Insole_leftIP();
    }

    private void loadColorsR() {
        Log.d(TAG, "loadColorsR: called");
        SharedPreferences prefs = getSharedPreferences("My_Appinsolereadings", MODE_PRIVATE);
        short[][] sensorReadings = loadSensorReadings(prefs);
        Log.d(TAG, "loadColorsR: sensorReadings=" + Arrays.deepToString(sensorReadings));

        float[] leituraAtual = null;

        if (sensorReadings != null && sensorReadings.length >= 9 && sensorReadings[0].length > 0) {
            int ultimo = sensorReadings[0].length - 1;
            leituraAtual = new float[9];
            for (int i = 0; i < 9; i++) {
                leituraAtual[i] = sensorReadings[i][ultimo];
            }
            lastLeituraR = leituraAtual; // atualiza ultimo dado
        } else if (lastLeituraR != null) {
            Log.w(TAG, "loadColorsR: usando última leitura salva por dados nulos");
            leituraAtual = lastLeituraR;
        } else {
            Log.e(TAG, "loadColorsR: dados nulos e sem último valor");
            return;
        }

        Log.d(TAG, "loadColorsR: leituraAtual=" + Arrays.toString(leituraAtual));
        sensoresR.clear();
        float r = 0.3f;
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.28f, 0.12f, leituraAtual[0], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.55f, 0.15f, leituraAtual[1], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.62f, 0.45f, leituraAtual[2], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.49f, 0.30f, leituraAtual[3], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.30f, 0.40f, leituraAtual[4], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.53f, 0.59f, leituraAtual[5], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.51f, 0.72f, leituraAtual[6], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.49f, 0.85f, leituraAtual[7], r));
        sensoresR.add(new HeatMapViewR.SensorRegionR(0.34f, 0.85f, leituraAtual[8], r));
        heatmapViewR.setRegions(sensoresR);
        Log.d(TAG, "loadColorsR: regions set");
    }


    private void loadColorsL() {
        Log.d(TAG, "loadColorsL: called");
        SharedPreferences prefs = getSharedPreferences("My_Appinsolereadings2", MODE_PRIVATE);
        short[][] sensorReadings = loadSensorReadings2(prefs);
        Log.d(TAG, "loadColorsL: sensorReadings=" + Arrays.deepToString(sensorReadings));

        float[] leituraAtual = null;

        if (sensorReadings != null && sensorReadings.length >= 9 && sensorReadings[0].length > 0) {
            int ultimo = sensorReadings[0].length - 1;
            leituraAtual = new float[9];
            for (int i = 0; i < 9; i++) {
                leituraAtual[i] = sensorReadings[i][ultimo];
            }
            lastLeituraL = leituraAtual; // atualiza ultimo dado
        } else if (lastLeituraL != null) {
            Log.w(TAG, "loadColorsL: usando última leitura salva por dados nulos");
            leituraAtual = lastLeituraL;
        } else {
            Log.e(TAG, "loadColorsL: dados nulos e sem último valor");
            return;
        }

        Log.d(TAG, "loadColorsL: leituraAtual=" + Arrays.toString(leituraAtual));
        sensoresL.clear();
        float r = 0.3f;
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.28f, 0.12f, leituraAtual[0], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.74f, 0.12f, leituraAtual[0], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.51f, 0.18f, leituraAtual[1], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.51f, 0.32f, leituraAtual[3], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.69f, 0.38f, leituraAtual[2], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.42f, 0.45f, leituraAtual[4], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.44f, 0.61f, leituraAtual[5], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.48f, 0.75f, leituraAtual[6], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.51f, 0.87f, leituraAtual[7], r));
        sensoresL.add(new HeatMapViewL.SensorRegionL(0.65f, 0.87f, leituraAtual[8], r));
        heatmapViewL.setRegions(sensoresL);
        Log.d(TAG, "loadColorsL: regions set");
    }


    private short[][] loadSensorReadings(SharedPreferences prefs) {
        Log.d(TAG, "loadSensorReadings: called");
        String[] keys = {"S1_1","S2_1","S3_1","S4_1","S5_1","S6_1","S7_1","S8_1","S9_1"};
        short[][] readings = new short[9][];
        for (int i = 0; i < 9; i++) {
            String data = prefs.getString(keys[i], "[]");
            Log.d(TAG, "loadSensorReadings: " + keys[i] + "=" + data);
            readings[i] = stringToShortArray(data);
        }
        return readings;
    }

    private short[][] loadSensorReadings2(SharedPreferences prefs) {
        Log.d(TAG, "loadSensorReadings2: called");
        String[] keys = {"S1_2","S2_2","S3_2","S4_2","S5_2","S6_2","S7_2","S8_2","S9_2"};
        short[][] readings = new short[9][];
        for (int i = 0; i < 9; i++) {
            String data = prefs.getString(keys[i], "[]");
            Log.d(TAG, "loadSensorReadings2: " + keys[i] + "=" + data);
            readings[i] = stringToShortArray(data);
        }
        return readings;
    }

    private short[] stringToShortArray(String input) {
        input = input.replace("[", "").replace("]", "").trim();
        if (input.isEmpty()) return new short[0];
        String[] parts = input.split(",");
        short[] result = new short[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = (short) Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "stringToShortArray: invalid number=" + parts[i]);
                result[i] = 0;
            }
        }
        return result;
    }

    public void Insole_RightIP() {
        Log.d(TAG, "Insole_RightIP: listener start");
        final int port = 20000;
        new Thread(() -> {
            try {
                DatagramSocket s = new DatagramSocket(port);
                DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
                while (true) {
                    s.receive(p);
                    String ip = new String(p.getData(), 0, p.getLength());
                    Log.d(TAG, "Insole_RightIP: got IP=" + ip);
                    SharedPreferences ipPrefs = getSharedPreferences("My_Appips", MODE_PRIVATE);
                    ipPrefs.edit().putString("IP", ip).apply();
                    Log.d(TAG, "Insole_RightIP: saved IP");
                }
            } catch (Exception e) {
                Log.e(TAG, "Insole_RightIP: error", e);
            }
        }).start();
    }

    public void Insole_leftIP() {
        Log.d(TAG, "Insole_leftIP: listener start");
        final int port = 20001;
        new Thread(() -> {
            try {
                DatagramSocket s = new DatagramSocket(port);
                DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
                while (true) {
                    s.receive(p);
                    String ip = new String(p.getData(), 0, p.getLength());
                    Log.d(TAG, "Insole_leftIP: got IP=" + ip);
                    SharedPreferences ipPrefs = getSharedPreferences("My_Appips", MODE_PRIVATE);
                    ipPrefs.edit().putString("IP2", ip).apply();
                    Log.d(TAG, "Insole_leftIP: saved IP");
                }
            } catch (Exception e) {
                Log.e(TAG, "Insole_leftIP: error", e);
            }
        }).start();
    }
}

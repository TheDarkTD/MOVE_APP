package com.example.myapplication2.Home;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.AppForegroundService;
import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.DataCaptureService;
import com.example.myapplication2.HeatMapViewL;
import com.example.myapplication2.HeatMapViewR;
import com.example.myapplication2.R;
import com.example.myapplication2.exam.ExamModeActivity;
import com.example.myapplication2.patient.PatientHubActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    // ====== Extras do exame ======
    public static final String EXTRA_CPF  = "extra_cpf";
    public static final String EXTRA_MODE = "extra_mode"; // "movimento" | "estatico"

    private String cpf;
    private String mode;
    private String sessionId;
    private boolean running = false;
    private final Handler autoStopHandler = new Handler();

    // ====== Preferências / seleção de pés ======
    private SharedPreferences sharedPreferences;
    private String followInRight, followInLeft;

    // ====== UI: mostradores e máscaras ======
    private FrameLayout frameL, frameR;
    private HeatMapViewL heatmapViewL;
    private HeatMapViewR heatmapViewR;
    private ImageView maskL, maskR;
    private Button PARAR;
    private Button INICIAR;

    // ====== Últimas leituras mantidas (fallback visual) ======
    private float[] lastLeituraR = null;
    private float[] lastLeituraL = null;

    // ====== Thresholds ======
    private short S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1;
    private short S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2;

    // ====== Regiões dos heatmaps ======
    private final List<HeatMapViewL.SensorRegionL> sensoresL = new ArrayList<>();
    private final List<HeatMapViewR.SensorRegionR> sensoresR = new ArrayList<>();

    // ====== Conectores BLE ======
    private ConectInsole conectar;   // direito
    private ConectInsole2 conectar2; // esquerdo


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Log.d(TAG, "onCreate: entered");

        // Toolbar com botão voltar
        MaterialToolbar topBar = findViewById(R.id.topAppBar);
        topBar.setNavigationIcon(R.drawable.ic_arrow_back_24);
        topBar.setNavigationOnClickListener(v -> {
            // Volta para a escolha do modo preservando o CPF
            Intent it = new Intent(this, ExamModeActivity.class);
            it.putExtra(ExamModeActivity.EXTRA_CPF, cpf);
            startActivity(it);
            finish();
        });

        // Recebe CPF e MODO do exame
        cpf  = getIntent().getStringExtra(EXTRA_CPF);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (cpf == null || mode == null) {
            // fallback seguro
            startActivity(new Intent(this, PatientHubActivity.class));
            finish();
            return;
        }
        topBar.setTitle("Exame (" + mode + ")");

        // Preferência de quais pés acompanhar
        sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        followInRight = sharedPreferences.getString("Sright", "default");
        followInLeft  = sharedPreferences.getString("Sleft", "default");

        // Views
        heatmapViewL = findViewById(R.id.heatmapViewL);
        heatmapViewR = findViewById(R.id.heatmapViewR);
        maskL = findViewById(R.id.imageView5);
        maskR = findViewById(R.id.imageView8);
        frameL = findViewById(R.id.frameL);
        frameR = findViewById(R.id.frameR);

        PARAR   = findViewById(R.id.buttonparar);
        INICIAR = findViewById(R.id.buttonIniciar);
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: entered");

        // Inicia serviços existentes
        startService(new Intent(this, AppForegroundService.class));
        startService(new Intent(this, DataCaptureService.class));

        // Instancia conectores (não envia 3A aqui — só ao clicar em Iniciar)
        conectar  = new ConectInsole(this);
        conectar2 = new ConectInsole2(this);

        // Mostrar/ocultar mostradores conforme preferências
        if ("false".equals(followInLeft)) {
            heatmapViewL.setVisibility(View.GONE);
            maskL.setVisibility(View.GONE);
            frameL.setVisibility(View.GONE);
        }
        if ("false".equals(followInRight)) {
            heatmapViewR.setVisibility(View.GONE);
            maskR.setVisibility(View.GONE);
            frameR.setVisibility(View.GONE);
        }

        // Carrega thresholds pé direito
        sharedPreferences = getSharedPreferences("ConfigPrefs1", MODE_PRIVATE);
        S1_1 = (short) sharedPreferences.getInt("S1", 0xffff);
        S2_1 = (short) sharedPreferences.getInt("S2", 0xffff);
        S3_1 = (short) sharedPreferences.getInt("S3", 0xffff);
        S4_1 = (short) sharedPreferences.getInt("S4", 0xffff);
        S5_1 = (short) sharedPreferences.getInt("S5", 0xffff);
        S6_1 = (short) sharedPreferences.getInt("S6", 0xffff);
        S7_1 = (short) sharedPreferences.getInt("S7", 0xffff);
        S8_1 = (short) sharedPreferences.getInt("S8", 0xffff);
        S9_1 = (short) sharedPreferences.getInt("S9", 0xffff);

        // Carrega thresholds pé esquerdo
        sharedPreferences = getSharedPreferences("ConfigPrefs2", MODE_PRIVATE);
        S1_2 = (short) sharedPreferences.getInt("S1", 0xffff);
        S2_2 = (short) sharedPreferences.getInt("S2", 0xffff);
        S3_2 = (short) sharedPreferences.getInt("S3", 0xffff);
        S4_2 = (short) sharedPreferences.getInt("S4", 0xffff);
        S5_2 = (short) sharedPreferences.getInt("S5", 0xffff);
        S6_2 = (short) sharedPreferences.getInt("S6", 0xffff);
        S7_2 = (short) sharedPreferences.getInt("S7", 0xffff);
        S8_2 = (short) sharedPreferences.getInt("S8", 0xffff);
        S9_2 = (short) sharedPreferences.getInt("S9", 0xffff);

        // Atualiza mostradores (com últimas leituras)
        loadColorsL();
        loadColorsR();
        INICIAR.setOnClickListener(v -> {
            Log.d(TAG, "ReadBtn: request readings");
            stopService(new Intent(this, DataCaptureService.class));
            conectar.setSessionMeta(cpf, mode, sessionId);   // pé direito
            conectar.enableBuffering(true);
            conectar.createAndSendConfigData((byte) 0x3A, (byte) 1, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);
            //conectar2.setSessionMeta(cpf, mode, sessionId);  // pé esquerdo (se usar)
            //conectar2.enableBuffering(true);
            //conectar2.createAndSendConfigData((byte) 0x3A, freq, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);
            if ("estatico".equalsIgnoreCase(mode)) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    conectar.flushToCloudNow();
                    conectar.enableBuffering(false);
                    conectar.createAndSendConfigData((byte) 0x3B, (byte) 1, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);
                    //conectar2.flushToCloudNow();
                    //conectar2.enableBuffering(false);
                    //conectar2.createAndSendConfigData((byte) 0x3B, (byte) 0, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);


                }, 10000);
            }

        });
        PARAR.setOnClickListener(v -> {
            conectar.flushToCloudNow();
            conectar.enableBuffering(false);
            conectar.createAndSendConfigData((byte) 0x3B, (byte) 0, S1_1, S2_1, S3_1, S4_1, S5_1, S6_1, S7_1, S8_1, S9_1);
            //conectar2.flushToCloudNow();
            //conectar2.enableBuffering(false);
            //conectar2.createAndSendConfigData((byte) 0x3B, (byte) 0, S1_2, S2_2, S3_2, S4_2, S5_2, S6_2, S7_2, S8_2, S9_2);
            //conectar2.clearBuffer();
        });

    }

    // ====== Controle do ciclo start/stop ======
    private String newSessionId() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault())
                .format(new Date());
    }

    // ====== Heatmap do pé direito ======
    // ====== Heatmap do pé direito ======
    public void loadColorsR() {
        // Se não estiver no thread da UI, re-agende e saia:
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::loadColorsR);
            return;
        }

        Log.d(TAG, "loadColorsR: called");
        SharedPreferences prefs = getSharedPreferences("My_Appinsolereadings", MODE_PRIVATE);
        short[][] sensorReadings = loadSensorReadings(prefs);
        Log.d(TAG, "loadColorsR: sensorReadings=" + Arrays.deepToString(sensorReadings));

        float[] leituraAtual = null;
        if (sensorReadings != null && sensorReadings.length >= 9 && sensorReadings[0].length > 0) {
            int ultimo = sensorReadings[0].length - 1;
            leituraAtual = new float[9];
            for (int i = 0; i < 9; i++) leituraAtual[i] = sensorReadings[i][ultimo];
            lastLeituraR = leituraAtual;
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
        // Força repintar (caso o custom view não chame sozinho):
        heatmapViewR.postInvalidateOnAnimation(); // ou heatmapViewR.invalidate()
    }

    // ====== Heatmap do pé esquerdo ======
    private void loadColorsL() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::loadColorsL);
            return;
        }

        Log.d(TAG, "loadColorsL: called");
        SharedPreferences prefs = getSharedPreferences("My_Appinsolereadings2", MODE_PRIVATE);
        short[][] sensorReadings = loadSensorReadings2(prefs);
        Log.d(TAG, "loadColorsL: sensorReadings=" + Arrays.deepToString(sensorReadings));

        float[] leituraAtual = null;
        if (sensorReadings != null && sensorReadings.length >= 9 && sensorReadings[0].length > 0) {
            int ultimo = sensorReadings[0].length - 1;
            leituraAtual = new float[9];
            for (int i = 0; i < 9; i++) leituraAtual[i] = sensorReadings[i][ultimo];
            lastLeituraL = leituraAtual;
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
        heatmapViewL.postInvalidateOnAnimation(); // ou heatmapViewL.invalidate()
    }


    // ====== Leitura das prefs (direito) ======
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

    // ====== Leitura das prefs (esquerdo) ======
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

    // ====== Conversor de string "[1,2,3]" -> short[] ======
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
    public interface Listener { void onNewReading(); } // simples


}
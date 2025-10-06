package com.example.myapplication2.Register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.ConectVibra;
import com.example.myapplication2.R;
import com.google.firebase.auth.FirebaseAuth;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

public class Register8Activity extends AppCompatActivity {
    Button mNext8Btn;
    FirebaseAuth fAuth;
    Button mStartS;
    NumberPicker mintensity, mpulse, mtime;
    Socket socket;
    PrintWriter output;
    String selectedPulse, selectedTime, selectedIntensity;
    BufferedReader input;
    byte PEST, INT, cmd;
    short TMEST, INEST;
    private static final int UDP_PORT = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        //startService(new Intent(this, DataReceiverService.class));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register8);


        mNext8Btn = findViewById(R.id.btnNext8);
        fAuth = FirebaseAuth.getInstance();

        mintensity = findViewById(R.id.intensidade1);
        mtime = findViewById(R.id.tempo1);
        mpulse = findViewById(R.id.pulso1);
        mStartS = findViewById(R.id.buttontestvibra);

        String[] arrayInt = new String[]{"Alta", "Média", "Baixa"};
        mintensity.setMinValue(0);
        mintensity.setMaxValue(arrayInt.length - 1);
        selectedIntensity = arrayInt[mintensity.getValue()];  // Initialize selectedIntensity

        mintensity.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return arrayInt[value];
            }
        });

        mintensity.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                selectedIntensity = arrayInt[newVal];
            }
        });

        String[] arrayTime = new String[]{"2 segundos", "5 segundos", "10 segundos", "15 segundos"};
        mtime.setMinValue(0);
        mtime.setMaxValue(arrayTime.length - 1);
        selectedTime = arrayTime[mtime.getValue()];  // Initialize selectedTime

        mtime.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return arrayTime[value];
            }
        });

        mtime.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                selectedTime = arrayTime[newVal];
            }
        });

        String[] arrayPul = new String[]{"Rápido", "Médio", "Devagar"};
        mpulse.setMinValue(0);
        mpulse.setMaxValue(arrayPul.length - 1);
        selectedPulse = arrayPul[mpulse.getValue()];  // Initialize selectedPulse

        mpulse.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return arrayPul[value];
            }
        });

        mpulse.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                selectedPulse = arrayPul[newVal];
            }
        });






        //Botão para iniciar estímulo teste
        mStartS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (selectedIntensity){
                    case "Alta":
                        INT = (byte) 100;
                        break;
                    case "Média":
                        INT = (byte) 70;
                        break;
                    case "Baixa":
                        INT = (byte) 30;
                        break;
                    default:
                        INT = (byte) 10;
                        break;
                }
                switch (selectedTime){
                    case "2 segundos":
                        TMEST = 2000;
                        break;
                    case "5 segundos":
                        TMEST = 5000;
                        break;
                    case "10 segundos":
                        TMEST = 10000;
                        break;
                    case "15 segundos":
                        TMEST = 15000;
                        break;
                }
                switch (selectedPulse){
                    case "Rápido":
                        PEST = (byte) 50;
                        INEST = 1000;
                        break;
                    case "Médio":
                        PEST = (byte) 50;
                        INEST = 4000;
                        break;
                    case "Devagar":
                        PEST = (byte) 70;
                        INEST = 5000;
                        break;

                }


                cmd = 0x1B;
                ConectVibra conectar = new ConectVibra(Register8Activity.this);
                conectar.SendConfigData(cmd, PEST, INT, TMEST, INEST);
                System.out.println(cmd + ","+ PEST+ ","+INT+ ","+TMEST+ ","+INEST);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    conectar.receiveData();
                }, 1000);

            }
        });

        mNext8Btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), RegisterActivity.class));

                SharedPreferences sharedPreferences = getSharedPreferences("My_Appvibra", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("int", String.valueOf(INT));
                editor.putString("time", String.valueOf(TMEST));
                editor.putString("pulse", String.valueOf(PEST));
                editor.putString("interval", String.valueOf(INEST));
                editor.apply();


            }
        });

    }}
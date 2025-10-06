package com.example.myapplication2.Register;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;


import com.example.myapplication2.R;
import com.google.firebase.auth.FirebaseAuth;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Register4Activity extends AppCompatActivity {

        Button mNext4Btn;
        FirebaseAuth fAuth;

        EditText ipAddressp1;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register4);

            mNext4Btn = findViewById(R.id.btnNext5);
            fAuth = FirebaseAuth.getInstance();;

            mNext4Btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Insole_RightIP();
                    startActivity(new Intent(getApplicationContext(), Register4_1Activity.class));
                }
            });
        }

    public void Insole_RightIP() {
        final int udpPortr = 20000; // Porta do ESP

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket(udpPortr);
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    while (true) {
                        socket.receive(packet);
                        String IPR = new String(packet.getData(), 0, packet.getLength());
                        Log.e("UDP", "Received IP: " + IPR + " on port: " + udpPortr);
                        // Armazene o IP conforme necessário
                        SharedPreferences sharedPreferences = getSharedPreferences("My_Appips", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("IP", IPR);
                        editor.apply();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    }

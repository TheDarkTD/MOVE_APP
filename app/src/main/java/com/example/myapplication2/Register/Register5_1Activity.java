package com.example.myapplication2.Register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import com.google.firebase.auth.FirebaseAuth;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Register5_1Activity  extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    Button mNext5Btn, mWebVBtn;
    WebView wregister5;
    String ipAddressp2s;
    FirebaseAuth fAuth;
    EditText ipAddressp2;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register5_1);
    }

    @Override
    public void onStart() {
        super.onStart();

        mNext5Btn = findViewById(R.id.btnNext5_1);
        wregister5 = findViewById(R.id.web5);
        fAuth = FirebaseAuth.getInstance();



        mNext5Btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Insole_leftIP();
                startActivity(new Intent(getApplicationContext(), Register3Activity.class));

            }
        });

        wregister5.setWebViewClient(new WebViewClient());

        // Habilita JavaScript se necessário
        WebSettings webSettings = wregister5.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Carrega uma URL
        wregister5.loadUrl("http://192.168.4.1");

    }

    public void Insole_leftIP() {
        final int udpPortl = 20001; // Porta do ESP

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket(udpPortl);
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    while (true) {
                        socket.receive(packet);
                        String IPL = new String(packet.getData(), 0, packet.getLength());
                        Log.e("UDP", "Received IP left: " + IPL + " on port: " + udpPortl);
                        // Armazene o IP conforme necessário
                        SharedPreferences sharedPreferences = getSharedPreferences("My_Appips", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("IP2", IPL);
                        editor.apply();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}

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

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;

public class Register6_1Activity extends AppCompatActivity {
    Button mNext6Btn, mWebVBtn;
    WebView wregister6;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register6_1);
    }

    @Override
    public void onStart() {
        super.onStart();

        mNext6Btn = findViewById(R.id.btnNext4_1);
        wregister6 = findViewById(R.id.web6);


        mNext6Btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                StimulatorIP();
                startActivity(new Intent(getApplicationContext(), Register8Activity.class));

            }
        });

        wregister6.setWebViewClient(new WebViewClient());

        // Habilita JavaScript se necessário
        WebSettings webSettings = wregister6.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Carrega uma URL
        wregister6.loadUrl("http://192.168.4.1");

    }

    public void StimulatorIP() {
        final int udpPortv = 10000; // Porta do ESP

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket(udpPortv);
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    while (true) {
                        socket.receive(packet);
                        String IPV = new String(packet.getData(), 0, packet.getLength());
                        Log.e("UDP", "Received IP: " + IPV + " on port: " + udpPortv);
                        // Armazene o IP conforme necessário
                        SharedPreferences sharedPreferences = getSharedPreferences("My_Appips", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("IPv", IPV);
                        editor.apply();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}

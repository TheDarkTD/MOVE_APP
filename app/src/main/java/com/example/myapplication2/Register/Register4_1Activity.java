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

public class Register4_1Activity extends AppCompatActivity {

    Button mNext4Btn;
    WebView wregister4;
    FirebaseAuth fAuth;
    EditText ipAddressp1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register4_1);

        mNext4Btn = findViewById(R.id.btnNext4_1);
        fAuth = FirebaseAuth.getInstance();
        wregister4 = findViewById(R.id.web4);

        mNext4Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Insole_RightIP();
                startActivity(new Intent(getApplicationContext(), Register2Activity.class));
            }
        });

        wregister4.setWebViewClient(new WebViewClient());

        // Habilita JavaScript se necessário
        WebSettings webSettings = wregister4.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Carrega uma URL
        wregister4.loadUrl("http://192.168.4.1");
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

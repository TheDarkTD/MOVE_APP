package com.example.myapplication2.Connection;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.Data.DataActivity;
import com.example.myapplication2.Home.HomeActivity;
import com.example.myapplication2.R;
import com.example.myapplication2.Settings.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;


public class ConnectionActivity extends AppCompatActivity {
    FirebaseAuth fAuth;
    private ConectInsole conectar;
    private ConectInsole2 conectar2;
    Switch conect1R, conect1L, conect2;
    String batInsoleright, batInsoleleft, batVibra;
    TextView batp, batv;
    private String InRight, InLeft;
    private TextView batlevel;
    Boolean connectedR, connectedL, connectedV;
    private Calendar calendar;
    private short S1, S2, S3, S4, S5, S6, S7, S8, S9;


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        conect1R = findViewById(R.id.switch1);
        conect1L = findViewById(R.id.switch3);
        conect2 = findViewById(R.id.switch2);
        batp = findViewById(R.id.batinsole);
        batv = findViewById(R.id.batvibra);

        conectar = new ConectInsole(this);
        conectar2 = new ConectInsole2(this);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onStart() {
        super.onStart();

        //checar quantidade de palmilhas
        SharedPreferences sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        InRight = sharedPreferences.getString("Sright", "default");
        InLeft = sharedPreferences.getString("Sleft", "default");

        //enviar solicitação de status de conexão
        calendar = Calendar.getInstance();
        byte cmd = 0X3E;
        byte freq = 1;
        S1 = S2 = S3 = S4 = S5 = S6 = S7 = S8 = S9 = 0x1FFF;

        if (InRight.equals("true")) {
            conectar.createAndSendConfigData(cmd, freq, S1, S2, S3, S4, S5, S6, S7, S8, S9);
        }
        if (InLeft.equals("true")) {
            conectar2.createAndSendConfigData(cmd, freq, S1, S2, S3, S4, S5, S6, S7, S8, S9);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                conectar2.receiveData(ConnectionActivity.this);}, 1500);
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomnavview3);
        bottomNavigationView.setSelectedItemId(R.id.connection);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    finish();
                case R.id.settings:
                    startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                    finish();
                    return true;
                case R.id.connection:
                    return true;
                case R.id.data:
                    startActivity(new Intent(getApplicationContext(), DataActivity.class));
                    finish();
                    return true;
            }
            return false;
        });


        //função para retornar valores de bateria vibra e palmilha e alterar mensagem

        sharedPreferences = getSharedPreferences("Battery_info", MODE_PRIVATE);
        batVibra = String.valueOf(sharedPreferences.getInt("batVibra", 0));
        batInsoleright = String.valueOf(sharedPreferences.getInt("Insole_right", 0));
        batInsoleleft = String.valueOf(sharedPreferences.getInt("Insole_left", 0));


        //checar status de conexão
        SharedPreferences sharedPreferences1 = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        connectedR  = sharedPreferences1.getBoolean("connectedinsole1", false);
        connectedL  = sharedPreferences1.getBoolean("connectedinsole2", false);

        /*if (InRight.equals("false") && InLeft.equals("true")) {
            conect1L.setChecked(connectedL);

            conect1R.setVisibility(View.GONE);
            batlevel.setText("Nível de bateria palmilha esquerda:");
            batp.setText(batInsoleleft);
        }
        if (InLeft.equals("false") && InRight.equals("true")) {
            conect1R.setChecked(connectedR);

            conect1L.setVisibility(View.GONE);
            batlevel.setText("Nível de bateria palmilha direita:");
            batp.setText(batInsoleright);

        }
        else{
            conect1L.setChecked(connectedL);
            conect1R.setChecked(connectedR);

            batlevel.setText("Nível de bateria palmilhas direita e esquerda:");
            batp.setText(batInsoleleft + batInsoleright);

        }*/


        sharedPreferences1 = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        connectedV = sharedPreferences1.getBoolean("connectedVibra", false);
        conect2.setChecked(connectedR);

        batv.setText(batVibra);


        }
}
package com.example.myapplication2.Register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;

public class Register6Activity extends AppCompatActivity {
    Button mNext6Btn, mWebVBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register6);
    }

    @Override
    public void onStart() {
        super.onStart();

        mNext6Btn = findViewById(R.id.btnNext5);



        mNext6Btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), Register6_1Activity.class));

            }
        });

    }


}

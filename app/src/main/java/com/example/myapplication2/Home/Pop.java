package com.example.myapplication2.Home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class Pop extends AppCompatActivity {

    FloatingActionButton mCloseBtn;
    private String followInRight = "", followInLeft = "";
    TextView atualiza;
    private SharedPreferences sharedPreferences;
    DatabaseReference databaseReference;
    private List<String> Listevents;
    private FirebaseAuth fAuth;
    String senddatainsole1 = " ";
    String senddatainsole2 = " ";
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popupwindow);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int) (width * .8), (int) (height * .8));

        atualiza = findViewById(R.id.textView5);

        ConectInsole conectar = new ConectInsole(this);
        ConectInsole2 conectar2 = new ConectInsole2(this);

        sharedPreferences = getSharedPreferences("eventos", MODE_PRIVATE);
        followInRight = sharedPreferences.getString("followInRight", "false");
        followInLeft = sharedPreferences.getString("followInLeft", "false");

        Listevents = new java.util.ArrayList<>();
        SharedPreferences event = getSharedPreferences("eventos", MODE_PRIVATE);
        if (followInRight.equals("true")) {
           senddatainsole1 = event.getString("eventlist", "");
        }
        if (followInLeft.equals("true")) {
            senddatainsole2 = event.getString("eventlist2", "");
        }

        Listevents.add(senddatainsole1);
        Listevents.add(senddatainsole2);

        StringBuilder builder = new StringBuilder();
        for (String item : Listevents) {
            builder.append(item).append("\n");
        }
        atualiza.setText(builder.toString());

        mCloseBtn = findViewById(R.id.buttonclose);
        mCloseBtn.setOnClickListener(v -> finish());
    }
}

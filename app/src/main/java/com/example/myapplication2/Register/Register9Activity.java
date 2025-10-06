package com.example.myapplication2.Register;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.LoginActivity;
import com.example.myapplication2.R;
import com.google.firebase.auth.FirebaseAuth;

public class Register9Activity extends AppCompatActivity {
    Button mEndBtn;
    FirebaseAuth fAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register9);
    }

    @Override
    public void onStart() {
        super.onStart();

        mEndBtn = findViewById(R.id.btnEnd);
        fAuth = FirebaseAuth.getInstance();


        mEndBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), LoginActivity.class));


            }
        });

    }
}
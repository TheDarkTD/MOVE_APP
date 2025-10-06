package com.example.myapplication2.Register;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register2Activity extends AppCompatActivity {
    Button mNext2Btn;
    FirebaseAuth fAuth;

    Boolean r1right = false, r2right = false, r3right = false, r4right = false, r5right = false, r6right = false, r7right = false, r8right = false, r9right = false, rnoner = false;
    CheckBox mRegion1r,mRegion2r,mRegion3r,mRegion4r,mRegion5r,mRegion6r,mRegion7r,mRegion8r,mRegion9r,mRegionNoner;
    String userID;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;



    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public void onStart() {
        super.onStart();


        mRegion1r = findViewById(R.id.checkBox10);
        mRegion2r = findViewById(R.id.checkBox11);
        mRegion3r = findViewById(R.id.checkBox12);
        mRegion4r = findViewById(R.id.checkBox13);
        mRegion5r = findViewById(R.id.checkBox14);
        mRegion6r = findViewById(R.id.checkBox15);
        mRegion7r = findViewById(R.id.checkBox16);
        mRegion8r = findViewById(R.id.checkBox17);
        mRegion9r = findViewById(R.id.checkBox18);
        mRegionNoner = findViewById(R.id.checkBoxNone2);
        mNext2Btn = findViewById(R.id.btnNext2);

        fAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();


        mNext2Btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                // checagem palmilha direita
                if(mRegion1r.isChecked()){
                    r1right=true;

                }
                if(mRegion2r.isChecked()){
                    r2right=true;

                }
                if(mRegion3r.isChecked()){
                    r3right=true;

                }
                if(mRegion4r.isChecked()){
                    r4right=true;

                }
                if(mRegion5r.isChecked()){
                    r5right=true;

                }
                if(mRegion6r.isChecked()){
                    r6right=true;

                }
                if(mRegion7r.isChecked()){
                    r7right=true;

                }
                if(mRegion8r.isChecked()){
                    r8right=true;

                }
                if(mRegion9r.isChecked()){
                    r9right=true;

                }
                if(mRegionNoner.isChecked()){
                    rnoner=true;

                }

                if (mRegionNoner.isChecked() &&
                        (       mRegion9r.isChecked() ||
                                mRegion8r.isChecked() ||
                                mRegion7r.isChecked() ||
                                mRegion6r.isChecked() ||
                                mRegion5r.isChecked() ||
                                mRegion4r.isChecked() ||
                                mRegion3r.isChecked() ||
                                mRegion2r.isChecked() ||
                                mRegion1r.isChecked())) {
                    mRegionNoner.setError("Campo inválido. Selecione alguma região ou nenhuma.");
                    return;
                }

                // Salva o resultado da checkbox nas SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("My_Appregions", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("S1r", r1right);
                editor.putBoolean("S2r", r2right);
                editor.putBoolean("S3r", r3right);
                editor.putBoolean("S4r", r4right);
                editor.putBoolean("S5r", r5right);
                editor.putBoolean("S6r", r6right);
                editor.putBoolean("S7r", r7right);
                editor.putBoolean("S8r", r8right);
                editor.putBoolean("S9r", r9right);
                editor.putBoolean("Snr", rnoner);
                editor.apply();
                System.out.println("valoresR"+r1right+""+r2right+r3right+r4right+r5right+r6right+r7right+r8right+r9right+rnoner);

                startActivity(new Intent(getApplicationContext(), Register7Activity.class));


            }
        });


    }
}
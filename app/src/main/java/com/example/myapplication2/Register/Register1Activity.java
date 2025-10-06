package com.example.myapplication2.Register;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.DataCaptureService;
import com.example.myapplication2.LoginActivity;
import com.example.myapplication2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Register1Activity extends AppCompatActivity {

    CheckBox mboth, mright, mleft;
    Button mNext1Btn,BackBtn ;
    String jleft, jright;
    Intent serviceIntent;
    boolean re;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register1);
        serviceIntent = new Intent(this, DataCaptureService.class);

    }

    @SuppressLint("MissingInflatedId")
    @Override
    public void onStart() {
        super.onStart();

        //remover stopservice quando autenticação estiver funcionando ok

        stopService(serviceIntent);
        mboth = findViewById(R.id.both);
        mright = findViewById(R.id.justright);
        mleft = findViewById(R.id.justleft);
        mNext1Btn = findViewById(R.id.btnNext1);
        BackBtn = findViewById(R.id.btnback);

        BackBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            }
        });
        mNext1Btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                if(!mboth.isChecked() && !mright.isChecked()  && !mleft.isChecked()){
                    mNext1Btn.setError("Selecione alguma das opções acima.");
                    return;
                }

                if(mboth.isChecked() && mleft.isChecked() || mboth.isChecked() && mright.isChecked()){
                    mboth.setError("Campo inválido. Selecione apenas uma palmilha ou ambas.");
                    return;
                }
                if(mright.isChecked() && mleft.isChecked()){
                    jleft="true";
                    jright="true";
                }

                if(mleft.isChecked() && !mright.isChecked()){
                    jleft="true";
                    jright="false";

                }
                if (mright.isChecked() && !mleft.isChecked()){
                    jright="true";
                    jleft="false";
                }
                if (mboth.isChecked()){
                    jleft="true";
                    jright="true";
                }


                SharedPreferences sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("Sleft", jleft);
                editor.putString("Sright", jright);
                editor.apply();

                System.out.println(jleft);
                System.out.println(jright);

                if (jleft.equals("true")){
                startActivity(new Intent(getApplicationContext(), Register5Activity.class));
                }
                if (jright.equals("true") && jleft.equals("false")){

                    startActivity(new Intent(getApplicationContext(), Register4Activity.class));
                }
            }
        });


    }
}

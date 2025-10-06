package com.example.myapplication2.Register;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication2.R;
public class Register3Activity extends AppCompatActivity {
    Button mNext3Btn;
    CheckBox mRegion1, mRegion2, mRegion3, mRegion4, mRegion5, mRegion6, mRegion7, mRegion8, mRegion9, mRegion10, mRegionNone;
    Boolean r1 = false, r2 = false, r3 = false, r4 = false, r5 = false, r6 = false, r7 = false, r8 = false, r9 = false, rNone = false;
    String userID;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register3);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public void onStart() {
        super.onStart();

        mRegion1 = findViewById(R.id.checkBox);
        mRegion2 = findViewById(R.id.checkBox2);
        mRegion3 = findViewById(R.id.checkBox3);
        mRegion4 = findViewById(R.id.checkBox4);
        mRegion5 = findViewById(R.id.checkBox5);
        mRegion6 = findViewById(R.id.checkBox6);
        mRegion7 = findViewById(R.id.checkBox7);
        mRegion8 = findViewById(R.id.checkBox8);
        mRegion9 = findViewById(R.id.checkBox9);
        mRegionNone = findViewById(R.id.checkBoxNone);

        mNext3Btn = findViewById(R.id.btnNext3);




        mNext3Btn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                ///Checar regiões de feridas


                if(mRegion1.isChecked()){
                    r1=true;

                }
                if(mRegion2.isChecked()){
                    r2=true;

                }
                if(mRegion3.isChecked()){
                    r3=true;

                }
                if(mRegion4.isChecked()){
                    r4=true;

                }
                if(mRegion5.isChecked()){
                    r5=true;

                }
                if(mRegion6.isChecked()){
                    r6=true;
                }
                if(mRegion7.isChecked()){
                    r7=true;

                }
                if(mRegion8.isChecked()) {
                    r8 = true;

                }
                if(mRegion9.isChecked()) {
                    r9 = true;
                }


                if(mRegionNone.isChecked()){
                    rNone=true;
                }

                if (mRegionNone.isChecked() &&
                        (
                                mRegion9.isChecked() ||
                                mRegion8.isChecked() ||
                                mRegion7.isChecked() ||
                                mRegion6.isChecked() ||
                                mRegion5.isChecked() ||
                                mRegion4.isChecked() ||
                                mRegion3.isChecked() ||
                                mRegion2.isChecked() ||
                                mRegion1.isChecked())) {
                    mRegionNone.setError("Campo inválido. Selecione alguma região ou nenhuma.");
                    return;
                }


                // Salva o resultado da checkbox nas SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("My_Appregions", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("S1", r1);
                editor.putBoolean("S2", r2);
                editor.putBoolean("S3", r3);
                editor.putBoolean("S4", r4);
                editor.putBoolean("S5", r5);
                editor.putBoolean("S6", r6);
                editor.putBoolean("S7", r7);
                editor.putBoolean("S8", r8);
                editor.putBoolean("S9", r9);
                editor.putBoolean("Sn", rNone);
                editor.apply();
                System.out.println("valoresL"+r1+""+r2+r3+r4+r5+r6+r7+r8+r9+rNone);


                sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
                String followIn = sharedPreferences.getString("Sright", "default");
                System.out.println(followIn);


                if (followIn.equals("true")){
                    startActivity(new Intent(getApplicationContext(), Register4Activity.class));}
                else{
                    startActivity(new Intent(getApplicationContext(), Register7Activity.class));}


            }
        });

    }
}
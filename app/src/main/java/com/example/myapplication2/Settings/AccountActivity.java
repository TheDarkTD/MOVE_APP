package com.example.myapplication2.Settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication2.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AccountActivity extends AppCompatActivity {

    FloatingActionButton mBackAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
    }

    @Override
    public void onStart() {
        super.onStart();
        TextView userdados = findViewById(R.id.user_data);

        String insoletext1 = "";
        String insoletext2 = "";

        SharedPreferences sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        String pdireita = sharedPreferences.getString("Sright", "false");
        String pesquerda = sharedPreferences.getString("Sleft", "false");

        if(pdireita.equals("true")){
            insoletext1 = "direita";
        }
        if(pesquerda.equals("true")){
            insoletext2 = "esquerda";
        }

        SharedPreferences userdata = getSharedPreferences("userdata", MODE_PRIVATE);
        String nome = userdata.getString("name", "nome");
        String email = userdata.getString("email", "email");
        String insoles = "Palmilhas: " + insoletext1 +" "+ insoletext2;

        userdados.setText("Nome: " + nome + "\n" + "Email: " + email + "\n" + insoles);

        mBackAccount = findViewById(R.id.buttonbackinstr);

        mBackAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

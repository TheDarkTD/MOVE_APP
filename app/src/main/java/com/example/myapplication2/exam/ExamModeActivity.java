package com.example.myapplication2.exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.Home.HomeActivity;
import com.example.myapplication2.R;

public class ExamModeActivity extends AppCompatActivity {

    public static final String EXTRA_CPF = "extra_cpf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_mode);

        String cpf = getIntent().getStringExtra(EXTRA_CPF);
        TextView tv = findViewById(R.id.tvCpf);
        tv.setText("CPF: " + cpf);

        Button btnEst = findViewById(R.id.btnEstatico);
        Button btnMov = findViewById(R.id.btnMovimento);
        Button btnBack= findViewById(R.id.btnBackToHub);

        btnEst.setOnClickListener(v -> goHome(cpf, "estatico"));
        btnMov.setOnClickListener(v -> goHome(cpf, "movimento"));
        btnBack.setOnClickListener(v -> finish()); // volta para o hub
    }

    private void goHome(String cpf, String mode) {
        Intent it = new Intent(this, HomeActivity.class);
        it.putExtra(HomeActivity.EXTRA_CPF, cpf);
        it.putExtra(HomeActivity.EXTRA_MODE, mode);
        startActivity(it);
        finish();
    }
}

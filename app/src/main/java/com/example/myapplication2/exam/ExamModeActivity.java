// app/src/main/java/com/example/myapplication2/exam/ExamModeActivity.java
package com.example.myapplication2.exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import com.example.myapplication2.patient.PatientHubActivity;
import com.example.myapplication2.Home.HomeActivity;

public class ExamModeActivity extends AppCompatActivity {

    public static final String EXTRA_CPF  = "extra_cpf";
    public static final String EXTRA_MODE = "extra_mode"; // "movimento" | "estatico"

    private String cpf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_mode);

        cpf = getIntent().getStringExtra(EXTRA_CPF);
        if (cpf == null) { finish(); return; }

        Button btnStatic    = findViewById(R.id.btnStatic);
        Button btnMovement  = findViewById(R.id.btnMovement);
        Button btnBack      = findViewById(R.id.btnBackToHub);

        btnStatic.setOnClickListener(v -> openHome("estatico"));
        btnMovement.setOnClickListener(v -> openHome("movimento"));
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, PatientHubActivity.class));
            finish();
        });
    }

    private void openHome(String mode) {
        Intent it = new Intent(this, HomeActivity.class);
        it.putExtra(EXTRA_CPF, cpf);
        it.putExtra(EXTRA_MODE, mode);
        startActivity(it);
        finish();
    }
}

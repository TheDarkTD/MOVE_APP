package com.example.myapplication2.patient;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import com.example.myapplication2.exam.ExamModeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import android.content.Intent;

public class PatientSearchActivity extends AppCompatActivity {

    private EditText etCPF;
    private TextView tvResult;
    private Button btnBuscar, btnUsar;
    private DatabaseReference base;

    private String foundCpf; // mantém o último CPF encontrado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_search);

        etCPF    = findViewById(R.id.etCPFSearch);
        tvResult = findViewById(R.id.tvResult);
        btnBuscar= findViewById(R.id.btnBuscar);
        btnUsar  = findViewById(R.id.btnUsarPaciente);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        base = FirebaseDatabase.getInstance("https://bioapp-496ae-default-rtdb.firebaseio.com/")
                .getReference().child("Users").child(uid).child("patients");

        btnUsar.setEnabled(false);

        btnBuscar.setOnClickListener(v -> doSearch());
        btnUsar.setOnClickListener(v -> {
            if (foundCpf != null) {
                Intent it = new Intent(this, ExamModeActivity.class);
                it.putExtra(ExamModeActivity.EXTRA_CPF, foundCpf);
                startActivity(it);
                finish();
            }
        });
    }

    private void doSearch() {
        String cpf = etCPF.getText().toString().trim();
        if (TextUtils.isEmpty(cpf)) {
            etCPF.setError("Informe o CPF");
            return;
        }

        base.child(cpf).child("_profile").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                tvResult.setText("Erro: " + task.getException().getMessage());
                btnUsar.setEnabled(false);
                return;
            }
            DataSnapshot ds = task.getResult();
            if (ds.exists()) {
                String nome   = String.valueOf(ds.child("nome").getValue());
                String peso   = String.valueOf(ds.child("peso").getValue());
                String altura = String.valueOf(ds.child("altura").getValue());
                String calc   = String.valueOf(ds.child("calcado").getValue());
                tvResult.setText("Encontrado:\nNome: " + nome + "\nPeso: " + peso +
                        "\nAltura: " + altura + "\nCalçado: " + calc);
                foundCpf = cpf;
                btnUsar.setEnabled(true);
            } else {
                tvResult.setText("Paciente não encontrado.");
                foundCpf = null;
                btnUsar.setEnabled(false);
            }
        });
    }
}

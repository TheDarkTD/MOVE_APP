package com.example.myapplication2.patient;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import com.example.myapplication2.exam.ExamModeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PatientFormActivity extends AppCompatActivity {

    private EditText etNome, etCPF, etPeso, etAltura, etCalcado;
    private DatabaseReference base;

    public static class Patient {
        public String cpf, nome, calcado;
        public float peso, altura;
        public Patient() {}
        public Patient(String cpf, String nome, float peso, float altura, String calcado) {
            this.cpf = cpf; this.nome = nome; this.peso = peso; this.altura = altura; this.calcado = calcado;
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_form);

        etNome   = findViewById(R.id.etNome);
        etCPF    = findViewById(R.id.etCPF);
        etPeso   = findViewById(R.id.etPeso);
        etAltura = findViewById(R.id.etAltura);
        etCalcado= findViewById(R.id.etCalcado);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        base = FirebaseDatabase.getInstance("https://bioapp-496ae-default-rtdb.firebaseio.com/")
                .getReference().child("Users").child(uid).child("patients");

        Button btnSalvar = findViewById(R.id.btnSalvarPaciente);
        btnSalvar.setOnClickListener(v -> saveAndGoToExam());
    }

    private void saveAndGoToExam() {
        String cpf = etCPF.getText().toString().trim();
        String nome = etNome.getText().toString().trim();
        String calcado = etCalcado.getText().toString().trim();
        float peso = parseFloatOrZero(etPeso.getText().toString().trim());
        float altura = parseFloatOrZero(etAltura.getText().toString().trim());

        if (TextUtils.isEmpty(cpf)) { etCPF.setError("CPF é obrigatório"); return; }
        if (TextUtils.isEmpty(nome)) { etNome.setError("Nome é obrigatório"); return; }

        Patient p = new Patient(cpf, nome, peso, altura, calcado);

        base.child(cpf).child("_profile").setValue(p)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Paciente salvo!", Toast.LENGTH_SHORT).show();
                    Intent it = new Intent(this, ExamModeActivity.class);
                    it.putExtra(ExamModeActivity.EXTRA_CPF, cpf);
                    startActivity(it);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private float parseFloatOrZero(String s) {
        try { return Float.parseFloat(s); } catch (Exception e) { return 0f; }
    }
}

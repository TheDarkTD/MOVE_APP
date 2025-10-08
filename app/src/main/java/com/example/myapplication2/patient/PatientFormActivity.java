// app/src/main/java/com/example/myapplication2/patient/PatientFormActivity.java
package com.example.myapplication2.patient;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication2.R;
import com.google.firebase.auth.*;

public class PatientFormActivity extends AppCompatActivity {

    private EditText etName, etCpf, etWeight, etHeight, etShoe;
    private Button btnSave;
    private PatientDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_form);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        dao = new PatientDao(user.getUid());

        etName   = findViewById(R.id.etName);
        etCpf    = findViewById(R.id.etCpf);
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etShoe   = findViewById(R.id.etShoe);
        btnSave  = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> savePatient());
    }

    private void savePatient() {
        String name   = etName.getText().toString().trim();
        String cpfRaw = etCpf.getText().toString().trim();
        String cpf    = PatientDao.normalizeCpf(cpfRaw);
        String wStr   = etWeight.getText().toString().trim();
        String hStr   = etHeight.getText().toString().trim();
        String shoeStr= etShoe.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Nome obrigatório"); return; }
        if (TextUtils.isEmpty(cpf) || cpf.length()!=11) { etCpf.setError("CPF obrigatório (11 dígitos)"); return; }
        if (TextUtils.isEmpty(wStr)) { etWeight.setError("Peso obrigatório"); return; }
        if (TextUtils.isEmpty(hStr)) { etHeight.setError("Altura obrigatória"); return; }

        Double weight, height;
        try {
            weight = Double.parseDouble(wStr.replace(",", "."));
            height = Double.parseDouble(hStr.replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Peso/Altura inválidos", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer shoe = null;
        if (!TextUtils.isEmpty(shoeStr)) {
            try { shoe = Integer.parseInt(shoeStr); }
            catch (NumberFormatException e) { etShoe.setError("Número de calçado inválido"); return; }
        }

        Patient p = new Patient(cpf, name, weight, height, shoe);

        dao.save(p).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Paciente salvo", Toast.LENGTH_SHORT).show();
                finish(); // volta ao Hub
            } else {
                Toast.makeText(this, "Falha ao salvar: " +
                        (task.getException()!=null? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
            }
        });
    }
}

// app/src/main/java/com/example/myapplication2/patient/PatientSearchActivity.java
package com.example.myapplication2.patient;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication2.R;
import com.google.firebase.auth.*;

public class PatientSearchActivity extends AppCompatActivity {

    private EditText etCpf;
    private Button btnSearch;
    private TextView tvResult;
    private PatientDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_search);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        dao = new PatientDao(user.getUid());

        etCpf = findViewById(R.id.etCpfSearch);
        btnSearch = findViewById(R.id.btnSearchCpf);
        tvResult = findViewById(R.id.tvResult);

        btnSearch.setOnClickListener(v -> search());
    }

    private void search() {
        String cpfRaw = etCpf.getText().toString().trim();
        String cpf = PatientDao.normalizeCpf(cpfRaw);
        if (TextUtils.isEmpty(cpf) || cpf.length()!=11) {
            etCpf.setError("CPF (11 dígitos)");
            return;
        }

        dao.refByCpf(cpf).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Patient p = task.getResult().getValue(Patient.class);
                String shoeStr = (p.shoe != null) ? String.valueOf(p.shoe) : "-";
                tvResult.setText(
                        "Nome: "   + p.name     + "\n" +
                                "CPF: "    + p.cpf      + "\n" +
                                "Peso: "   + p.weightKg + " kg\n" +
                                "Altura: " + p.heightM  + " m\n" +
                                "Calçado: "+ shoeStr
                );
            } else {
                Toast.makeText(this, "Paciente não encontrado", Toast.LENGTH_SHORT).show();
                tvResult.setText("");
            }
        });
    }
}

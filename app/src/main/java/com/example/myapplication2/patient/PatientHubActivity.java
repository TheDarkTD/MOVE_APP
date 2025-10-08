// app/src/main/java/com/example/myapplication2/patient/PatientHubActivity.java
package com.example.myapplication2.patient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication2.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PatientHubActivity extends AppCompatActivity {

    Button btnCreate, btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_hub);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        btnCreate = findViewById(R.id.btnCreatePatient);
        btnSearch = findViewById(R.id.btnSearchPatient);

        btnCreate.setOnClickListener(v ->
                startActivity(new Intent(this, PatientFormActivity.class))
        );

        btnSearch.setOnClickListener(v ->
                startActivity(new Intent(this, PatientSearchActivity.class))
        );
    }
}

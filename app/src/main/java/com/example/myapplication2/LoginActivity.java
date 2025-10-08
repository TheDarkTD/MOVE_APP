// app/src/main/java/com/example/myapplication2/LoginActivity.java
package com.example.myapplication2;

import static android.content.ContentValues.TAG;
import static com.example.myapplication2.Settings.SettingsActivity.SHARED_PREFS;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.Register.RegisterActivity;
import com.example.myapplication2.patient.PatientHubActivity;
import com.google.firebase.auth.*;

public class LoginActivity extends AppCompatActivity {

    EditText mEmail, mPassword;
    Button mLoginBtn;
    TextView mCreateBtn;
    FirebaseAuth fAuth;
    ConectInsole conectInsole;
    ConectInsole2 conectInsole2;
    String uid = null;

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // flag "reconfigurar" (mantive como estava)
        SharedPreferences.Editor edit = getSharedPreferences("reconfigurar", MODE_PRIVATE).edit();
        edit.putBoolean("reconfigurar", false);
        edit.apply();

        fAuth = FirebaseAuth.getInstance();
        conectInsole  = new ConectInsole(this);
        conectInsole2 = new ConectInsole2(this);

        mEmail      = findViewById(R.id.email);
        mPassword   = findViewById(R.id.password);
        mLoginBtn   = findViewById(R.id.btnLogin);
        mCreateBtn  = findViewById(R.id.textRegister);

        fAuth.setLanguageCode("pt-BR");

        // Registrar
        mCreateBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        // Login
        mLoginBtn.setOnClickListener(v -> {
            String email    = mEmail.getText().toString().trim();
            String password = mPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) { mEmail.setError("Email obrigatório."); return; }
            if (TextUtils.isEmpty(password)) { mPassword.setError("Senha obrigatória."); return; }

            fAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login bem-sucedido", Toast.LENGTH_LONG).show();
                            FirebaseUser user = fAuth.getCurrentUser();
                            if (user != null) {
                                uid = user.getUid();
                                // Navega direto para o HUB de pacientes
                                startActivity(new Intent(this, PatientHubActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(this,
                                    "Erro no login: " + (task.getException() != null ? task.getException().getMessage() : ""),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Já logado → vai ao HUB de pacientes
            startActivity(new Intent(this, PatientHubActivity.class));
            finish();
        }
    }
}

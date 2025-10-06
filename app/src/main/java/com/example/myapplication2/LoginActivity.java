package com.example.myapplication2;

import static android.content.ContentValues.TAG;

import static com.example.myapplication2.Settings.SettingsActivity.SHARED_PREFS;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.Home.HomeActivity;
import com.example.myapplication2.Register.Register1Activity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
    EditText mEmail, mPassword;
    Button mLoginBtn;
    TextView mCreateBtn;
    FirebaseAuth fAuth;
    ConectInsole conectInsole;
    ConectInsole2 conectInsole2;
    String uid = null;
    boolean re;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        re=false;
        SharedPreferences.Editor edit = getSharedPreferences("reconfigurar", MODE_PRIVATE).edit();
        edit.putBoolean("reconfigurar", re);
        edit.apply();
        // Inicializa FirebaseAuth e ConectInsoles
        fAuth = FirebaseAuth.getInstance();
        conectInsole  = new ConectInsole(this);
        conectInsole2 = new ConectInsole2(this);

        // Views
        mEmail      = findViewById(R.id.email);
        mPassword   = findViewById(R.id.password);
        mLoginBtn   = findViewById(R.id.btnLogin);
        mCreateBtn  = findViewById(R.id.textRegister);

        // Define idioma para mensagens do Firebase (opcional)
        fAuth.setLanguageCode("pt-BR");

        // Botão registrar
        mCreateBtn.setOnClickListener(v ->
                startActivity(new Intent(this, Register1Activity.class))
        );

        // Botão login
        mLoginBtn.setOnClickListener(v -> {
            String email    = mEmail.getText().toString().trim();
            String password = mPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                mEmail.setError("Email obrigatório.");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                mPassword.setError("Senha obrigatória.");
                return;
            }

            fAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login bem-sucedido", Toast.LENGTH_LONG).show();
                            FirebaseUser user = fAuth.getCurrentUser();
                            if (user != null) {
                                uid = user.getUid();
                                loadUserData(uid, v);
                            }
                        } else {
                            Toast.makeText(this,
                                    "Erro no login: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Se já estiver logado, pula direto para HomeActivity
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }


        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Register1Activity.class));
            }
        });
    }
    private void loadUserData(String uid, View view) {
        Log.d("LoadUserData", "Iniciando carregamento de dados para uid: " + uid);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                Log.d("LoadUserData", "DataSnapshot recebido: " + dataSnapshot.getValue());

                if (dataSnapshot.exists()) {
                    Log.d("LoadUserData", "Dados encontrados para uid: " + uid);

                    // Listando todas as chaves presentes para depuração
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Log.d("LoadUserData", "Chave encontrada: " + child.getKey());
                    }

                    // Recupera as flags para os insole e loga
                    String flagInsoleR = dataSnapshot.child("InsolesR").getValue(String.class);
                    String flagInsoleL = dataSnapshot.child("InsolesL").getValue(String.class);
                    Log.d("LoadUserData", "FlagInsoleR: " + flagInsoleR + " | FlagInsoleL: " + flagInsoleL);

                    // Carrega ConfigData1 caso o insole direito esteja ativado
                    if ("true".equalsIgnoreCase(flagInsoleR)) {
                        DataSnapshot evento= dataSnapshot.child("DATA");
                        if (evento.exists()) {
                            String eventlist = evento.child("eventlist").getValue(String.class);
                            SharedPreferences event = getSharedPreferences("eventos", MODE_PRIVATE);
                            SharedPreferences.Editor eventEditor = event.edit();
                            eventEditor.putString("eventlist", eventlist);
                        }



                        DataSnapshot configRightSnapshot = dataSnapshot.child("ConfigData1");
                        Log.d("LoadUserData", "Dados do ConfigData1: " + configRightSnapshot.getValue());

                        if (configRightSnapshot.exists()) {
                            for (DataSnapshot child : configRightSnapshot.getChildren()) {
                                Log.d("LoadUserData", "ConfigData1 Child: "
                                        + child.getKey() + " - " + child.getValue());
                            }
                        } else {
                            Log.e("LoadUserData", "Nó ConfigData1 não existe!");
                        }

                        ConectInsole.ConfigData configData1 = configRightSnapshot.getValue(ConectInsole.ConfigData.class);
                        if (configData1 != null) {
                            Log.d("LoadUserData", "ConfigData1 carregado com sucesso: " + configData1.toString());
                        } else {
                            Log.e("LoadUserData", "ConfigData1 retornou null!");
                        }
                        conectInsole.setConfigData(configData1);
                        SharedPreferences sharedPreferences = getSharedPreferences("ConfigPrefs1", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        // Salvando os valores de S1 a S9
                        editor.putInt("S1", configData1.S1);
                        editor.putInt("S2", configData1.S2);
                        editor.putInt("S3", configData1.S3);
                        editor.putInt("S4", configData1.S4);
                        editor.putInt("S5", configData1.S5);
                        editor.putInt("S6", configData1.S6);
                        editor.putInt("S7", configData1.S7);
                        editor.putInt("S8", configData1.S8);
                        editor.putInt("S9", configData1.S9);
                        editor.apply();
                    } else {
                        Log.d("LoadUserData", "FlagInsoleR não está 'true'. Não carregou ConfigData1.");
                    }

                    // Carrega ConfigData2 caso o insole esquerdo esteja ativado
                    if ("true".equalsIgnoreCase(flagInsoleL)) {
                        DataSnapshot evento= dataSnapshot.child("DATA");
                        if (evento.exists()) {
                            String eventlist2 = evento.child("eventlist2").getValue(String.class);
                            SharedPreferences event = getSharedPreferences("eventos", MODE_PRIVATE);
                            SharedPreferences.Editor eventEditor = event.edit();
                            eventEditor.putString("eventlist2", eventlist2);
                        }
                        DataSnapshot configLeftSnapshot = dataSnapshot.child("ConfigData2");
                        Log.d("LoadUserData", "Dados do ConfigData2: " + configLeftSnapshot.getValue());

                        ConectInsole2.ConfigData configData2 = configLeftSnapshot.getValue(ConectInsole2.ConfigData.class);
                        if (configData2 != null) {
                            Log.d("LoadUserData", "ConfigData2 carregado com sucesso: " + configData2.toString());
                        } else {
                            Log.e("LoadUserData", "ConfigData2 retornou null!");
                        }
                        conectInsole2.setConfigData2(configData2);
                        SharedPreferences sharedPreferences = getSharedPreferences("ConfigPrefs2", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        // Salvando os valores de S1 a S9
                        editor.putInt("S1", configData2.S1);
                        editor.putInt("S2", configData2.S2);
                        editor.putInt("S3", configData2.S3);
                        editor.putInt("S4", configData2.S4);
                        editor.putInt("S5", configData2.S5);
                        editor.putInt("S6", configData2.S6);
                        editor.putInt("S7", configData2.S7);
                        editor.putInt("S8", configData2.S8);
                        editor.putInt("S9", configData2.S9);
                        editor.apply();
                    } else {
                        Log.d("LoadUserData", "FlagInsoleL não está 'true'. Não carregou ConfigData2.");
                    }

                    // Salva as flags em SharedPreferences (já existente no seu código)
                    SharedPreferences sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("Sright", flagInsoleR);
                    editor.putString("Sleft", flagInsoleL);
                    editor.apply();
                    Log.d("LoadUserData", "Flags salvas nas SharedPreferences.");

                    // --- Recuperando os dados de vibração ---
                    DataSnapshot vibraSnapshot = dataSnapshot.child("vibra");
                    if (vibraSnapshot.exists()) {
                        String vibraTime = vibraSnapshot.child("time").getValue(String.class);
                        String vibraint = vibraSnapshot.child("int").getValue(String.class);
                        String vibraInterval = vibraSnapshot.child("interval").getValue(String.class);
                        String vibraPulse = vibraSnapshot.child("pulse").getValue(String.class);

                        Log.e(TAG, "-----------------------------------------------");
                        Log.e(TAG, vibraint);
                        Log.e(TAG, vibraTime);
                        Log.e(TAG, vibraPulse);
                        Log.e(TAG, vibraInterval);
                        Log.e(TAG, "-----------------------------------------------");

                        SharedPreferences vibraPref = getSharedPreferences("My_Appvibra", MODE_PRIVATE);
                        SharedPreferences.Editor vibraEditor = vibraPref.edit();
                        if (vibraTime != null) {
                            vibraEditor.putString("time", vibraTime);
                        }
                        if (vibraint != null) {
                            vibraEditor.putString("int", vibraint);
                        }
                        if (vibraInterval != null) {
                            vibraEditor.putString("interval", vibraInterval);
                        }
                        if (vibraPulse != null) {
                            vibraEditor.putString("pulse", vibraPulse);
                        }
                        vibraEditor.apply();
                        Log.d("LoadUserData", "Dados de vibração salvos em SharedPreferences 'vibra'.");
                    } else {
                        Log.d("LoadUserData", "Nenhuns dados de vibração encontrados no DataSnapshot.");
                    }
                    // --- Fim da recuperação de vibração ---
                    SharedPreferences shared = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
                    SharedPreferences.Editor editorP = shared.edit();
                    editorP.putString("Sleft", flagInsoleL);
                    editorP.putString("Sright", flagInsoleR);
                    editorP.apply();
                    // Após carregar os dados, inicia a HomeActivity
                    Log.d("LoadUserData", "Iniciando HomeActivity...");
                    Intent intent = new Intent(view.getContext(), HomeActivity.class);
                    startActivity(intent);
                } else {
                    Log.e("LoadUserData", "Nenhum dado encontrado para uid: " + uid);
                    Toast.makeText(LoginActivity.this, "Nenhum dado encontrado.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("LoadUserData", "Erro ao carregar os dados: " + task.getException().getMessage());
                Toast.makeText(LoginActivity.this, "Falha ao carregar os dados.", Toast.LENGTH_SHORT).show();
            }
        });


    }
}
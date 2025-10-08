package com.example.myapplication2.Register;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.LoginActivity;
import com.example.myapplication2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity
{
    EditText mName, mSurname, mEmail, mPassword, mPassword2, mBirth;
    Button mNextBtn;
    TextView mLoginBtn;
    private SharedPreferences sharedPreferences;
    private String followInRight, followInLeft;
    FirebaseAuth fAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private ConectInsole conectar;
    private ConectInsole2 conectar2;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializando os EditText e botões com findViewById
        mName = findViewById(R.id.name);
        mSurname = findViewById(R.id.surname);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mPassword2 = findViewById(R.id.password2);
        mNextBtn = findViewById(R.id.btnNext);
        mLoginBtn = findViewById(R.id.textLogin);

        sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        followInRight = sharedPreferences.getString("Sright", "default");
        followInLeft = sharedPreferences.getString("Sleft", "default");

        // Inicializando FirebaseAuth e o FirebaseDatabase
        fAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();

        // Definindo idioma para o FirebaseAuth
        fAuth.setLanguageCode("pt-BR");

        // Configurando o clique do botão "Próximo"
        mNextBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();
                String passwordConfirm = mPassword2.getText().toString().trim();

                // Verificações para os campos
                if (TextUtils.isEmpty(email))
                {
                    mEmail.setError("Email obrigatório.");
                    return;
                }

                if (TextUtils.isEmpty(password))
                {
                    mPassword.setError("Senha obrigatória.");
                    return;
                }

                if (password.length() < 8)
                {
                    mPassword.setError("Senha deve ter 8 caracteres ou mais.");
                    return;
                }

                if (!password.equals(passwordConfirm))
                {
                    mPassword2.setError("As senhas não coincidem.");
                    return;
                }

                // Registrando o usuário
                fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                        {
                            FirebaseUser user = fAuth.getCurrentUser();
                            if (user != null)
                            {
                                String uid = user.getUid();
                                saveUserData(uid);
                                startActivity(new Intent(getApplicationContext(), Register9Activity.class));
                            }
                        }
                        else
                        {
                            Toast.makeText(RegisterActivity.this, "Erro no registro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Configurando o clique do botão de login
        mLoginBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            }
        });
    }
    // Função para carregar os dados de S1 a S9 e retornar um HashMap com esses dados


    void saveUserData(String uid) {
        // Obtendo os dados dos campos de usuário
        String getName = mName.getText().toString().trim();
        String getSurname = mSurname.getText().toString().trim();
        String getEmail = mEmail.getText().toString().trim();

        // Verifica se algum campo obrigatório está vazio
        if (TextUtils.isEmpty(getName) || TextUtils.isEmpty(getSurname) ||
                TextUtils.isEmpty(getEmail)) {
            Toast.makeText(this, "Todos os campos devem ser preenchidos.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Criando o HashMap para salvar os dados do usuário
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", getName);
        hashMap.put("surname", getSurname);
        hashMap.put("email", getEmail);
        // Envia os dados para o Firebase
        databaseReference.child("Users").child(uid).setValue(hashMap)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(RegisterActivity.this, "Dados do usuário salvos", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(RegisterActivity.this, "Erro ao salvar dados: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
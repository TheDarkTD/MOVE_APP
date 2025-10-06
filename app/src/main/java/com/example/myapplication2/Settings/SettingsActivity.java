package com.example.myapplication2.Settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.Connection.ConnectionActivity;
import com.example.myapplication2.Data.DataActivity;
import com.example.myapplication2.Home.HomeActivity;
import com.example.myapplication2.LoginActivity;
import com.example.myapplication2.R;
import com.example.myapplication2.Register.Register1Activity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class SettingsActivity extends AppCompatActivity {
    Button mLogoutBtn;
    public static final String SHARED_PREFS = "sharedprefs_login";
    TextView mAccountBtn, mParametersBtn, mVibraBtn, mNotificationsBtn, mUpdatesBtn, mUseInstructionsBtn, reconfig;
    FirebaseAuth fAuth;
    ImageView fotoid;
    String userName, userEmail;
    DatabaseReference databaseReference;
    Boolean re=false;
    private SharedPreferences sharedPreferences;
    private String uid;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        TextView mNamespace = findViewById(R.id.nomeusuario);
        TextView mEmailspace = findViewById(R.id.emailusuario);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            uid = user.getUid();  // Pega o UID do usuário logado
            databaseReference = FirebaseDatabase.getInstance("https://bioapp-496ae-default-rtdb.firebaseio.com/")
                    .getReference()
                    .child("Users")
                    .child(uid);  // Salvar dados no nó "Users/{UID}"

            // Inicializando SharedPreferences para armazenar dados localmente
            sharedPreferences = getSharedPreferences("offline_data", Context.MODE_PRIVATE);
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        databaseReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {

                    userName = dataSnapshot.child("name").getValue(String.class);
                    userEmail = dataSnapshot.child("email").getValue(String.class);

                    SharedPreferences userdata = getSharedPreferences("userdata", MODE_PRIVATE);
                    SharedPreferences.Editor userdataEditor = userdata.edit();
                    userdataEditor.putString("name", userName);
                    userdataEditor.putString("email", userEmail);

                } else {
                    Toast.makeText(SettingsActivity.this, "Nenhum dado encontrado.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SettingsActivity.this, "Falha ao carregar os dados.", Toast.LENGTH_SHORT).show();
            }

            mNamespace.setText(userName);
            mEmailspace.setText(userEmail);
        });


    }


    @Override
    public void onStart() {
        super.onStart();

        mAccountBtn = findViewById(R.id.Conta);
        mParametersBtn = findViewById(R.id.Parametros);
        mVibraBtn = findViewById(R.id.Vibra);
        mUseInstructionsBtn = findViewById(R.id.Manual);
        fotoid = findViewById(R.id.fotoid);
        reconfig = findViewById(R.id.reconfigurar);
        mLogoutBtn = findViewById(R.id.btnLogout);
        fAuth = FirebaseAuth.getInstance();


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomnavview2);
        bottomNavigationView.setSelectedItemId(R.id.settings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    finish();
                    return true;
                case R.id.settings:
                    return true;
                case R.id.connection:
                    startActivity(new Intent(getApplicationContext(), ConnectionActivity.class));
                    finish();
                    return true;
                case R.id.data:
                    startActivity(new Intent(getApplicationContext(), DataActivity.class));
                    finish();
                    return true;
            }
            return false;
        });


        mLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fAuth.getCurrentUser() != null)
                    fAuth.signOut();
                SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                SharedPreferences.Editor editor_login = sharedPreferences.edit();
                editor_login.putString("name", "");
                editor_login.apply();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);

            }
        });

        mAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), AccountActivity.class));
            }
        });

        mParametersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ParametersActivity.class));
            }
        });

        mVibraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), VibraActivity.class));
            }
        });
        reconfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Register1Activity.class ));
                re=true;
                SharedPreferences.Editor editor = getSharedPreferences("reconfigurar", MODE_PRIVATE).edit();
                editor.putBoolean("reconfigurar", re);
                editor.apply();

            }
        });



        mUseInstructionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), UseInstructionsActivity.class));
            }
        });

        /*fotoid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(intent, "Escolha sua imagem"), 1);
            }
        });*/
    }

    /*protected void onActivityForResult(int RequestCode, int ResultCode, Intent dados) {
        super.onActivityResult(RequestCode, ResultCode, dados);
        if (ResultCode == Activity.RESULT_OK) {
            if (RequestCode == 1) {
                fotoid.setImageURI(dados.getData());
            }
        }*/

}
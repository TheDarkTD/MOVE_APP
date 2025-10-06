package com.example.myapplication2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.NetworkUtils;
import android.content.Context;
import android.content.SharedPreferences;

public class FirebaseHelper {

    private DatabaseReference mDatabase;
    private String userId;

    private Context context;
    private SharedPreferences sharedPreferences;

    // Construtor que recupera o UID do usuário logado e inicializa o banco de dados com o UID
    public FirebaseHelper(Context context) {
        this.context = context;
        // Recuperar o UID do usuário logado usando FirebaseAuth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            userId = user.getUid();  // Pega o UID do usuário logado
            mDatabase = FirebaseDatabase.getInstance("https://bioapp-496ae-default-rtdb.firebaseio.com/")
                    .getReference()
                    .child("Users")
                    .child(userId);  // Salvar dados no nó "Users/{UID}"

            // Inicializando SharedPreferences para armazenar dados localmente
            sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE);
        }
    }

    // Método para salvar SendData no Firebase para o usuário logado
    public void saveSendData(ConectInsole.SendData sendData, List<String> eventlist) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        if (NetworkUtils.isNetworkAvailable(context)) {
            // Se há conexão com a internet, salva diretamente no Firebase com um ID único
            String id = mDatabase.child("DATA").push().getKey();
            mDatabase.child("DATA").child(currentDate).child(id).setValue(sendData) // Envia a String diretamente
                    .addOnSuccessListener(aVoid -> {
                        System.out.println("SendData salvo no Firebase com sucesso!");
                    })
                    .addOnFailureListener(e -> {
                        System.err.println("Erro ao salvar SendData: " + e.getMessage());
                    });
            /*mDatabase.child("DATA").child(currentDate).child(id).setValue(eventlist.toString()) // Envia a String diretamente
                    .addOnSuccessListener(aVoid -> {
                        System.out.println("SendData salvo no Firebase com sucesso!");
                    })
                    .addOnFailureListener(e -> {
                        System.err.println("Erro ao salvar SendData: " + e.getMessage());
                    });*/
        } else {
            // Se não há conexão com a internet, salva localmente com um ID único
            saveSendDataLocally(sendData, currentDate);
        }
    }

    // Método para salvar SendData2 no Firebase para o usuário logado
    public void saveSendData2(ConectInsole2.SendData sendData) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        if (NetworkUtils.isNetworkAvailable(context)) {
            // Se há conexão com a internet, salva diretamente no Firebase com um ID único
            String id = mDatabase.child("DATA2").push().getKey();
            mDatabase.child("DATA2").child(currentDate).child(id).setValue(sendData) // Envia a String diretamente
                    .addOnSuccessListener(aVoid -> {
                        System.out.println("SendData2 salvo no Firebase com sucesso!");
                    })
                    .addOnFailureListener(e -> {
                        System.err.println("Erro ao salvar SendData2: " + e.getMessage());
                    });
            /*mDatabase.child("DATA2").child(currentDate).child(id).setValue(eventlist2.toString()) // Envia a String diretamente
                    .addOnSuccessListener(aVoid -> {
                        System.out.println("SendData2 salvo no Firebase com sucesso!");
                    })
                    .addOnFailureListener(e -> {
                        System.err.println("Erro ao salvar SendData2: " + e.getMessage());
                    });*/
        } else {
            // Se não há conexão com a internet, salva localmente com um ID único
            saveSendData2Locally(sendData, currentDate);
        }
    }

    // Método para salvar SendData localmente com um ID único
    public void saveSendDataLocally(ConectInsole.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData_" + System.currentTimeMillis(); // Gera uma chave única baseada no timestamp

        // Salva os dados no SharedPreferences sem sobrescrever dados anteriores
        editor.putString(key, sendData.toString()); // Armazena a String diretamente
        editor.apply();
    }

    // Método para salvar SendData2 localmente com um ID único
    public void saveSendData2Locally(ConectInsole2.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData2_" + System.currentTimeMillis(); // Gera uma chave única para SendData2

        // Salva os dados no SharedPreferences sem sobrescrever dados anteriores
        editor.putString(key, sendData.toString()); // Armazena a String diretamente
        editor.apply();
    }

    // Método para verificar se há dados offline para sincronizar (para SendData)
    public void syncSendDataOffline() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            boolean dataSynced = false; // Flag para saber se algum dado foi sincronizado

            for (String key : sharedPreferences.getAll().keySet()) {
                String savedData = sharedPreferences.getString(key, null);
                if (savedData != null && key.startsWith("sendData_")) {  // Filtra apenas as chaves relacionadas a SendData
                    String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                    String id = mDatabase.child("DATA").push().getKey(); // Gera um ID único para cada dado
                    mDatabase.child("DATA").child(currentDate).child(id).setValue(savedData); // Envia a String diretamente
                    dataSynced = true;
                }
            }

            // Se os dados foram sincronizados, remove-os do SharedPreferences
            if (dataSynced) {
                editor.apply();
            }
        }
    }

    // Método para verificar se há dados offline para sincronizar (para SendData2)
    public void syncSendData2Offline() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            boolean dataSynced = false; // Flag para saber se algum dado foi sincronizado

            for (String key : sharedPreferences.getAll().keySet()) {
                String savedData = sharedPreferences.getString(key, null);
                if (savedData != null && key.startsWith("sendData2_")) {  // Filtra apenas as chaves relacionadas a SendData2
                    String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                    String id = mDatabase.child("DATA2").push().getKey(); // Gera um ID único para cada dado
                    mDatabase.child("DATA2").child(currentDate).child(id).setValue(savedData); // Envia a String diretamente
                    dataSynced = true;
                }
            }

            // Se os dados foram sincronizados, remove-os do SharedPreferences
            if (dataSynced) {
                editor.apply();
            }
        }
    }
}
//___________________________________________________________________________
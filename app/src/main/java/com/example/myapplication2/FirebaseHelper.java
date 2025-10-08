package com.example.myapplication2;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
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

public class FirebaseHelper {

    // ===================== CAMPOS =====================
    private DatabaseReference mDatabase;
    private String userId;

    private final Context context;
    private final SharedPreferences sharedPreferences; // offline (legado + novo)

    // URL do seu Realtime Database (mantida)
    private static final String DB_URL = "https://bioapp-496ae-default-rtdb.firebaseio.com/";

    // ===================== CONSTRUTOR =====================
    // Mantive seu construtor: seta Users/{uid} como base e prepara "offline_data"
    public FirebaseHelper(Context context) {
        this.context = context;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            mDatabase = FirebaseDatabase.getInstance(DB_URL)
                    .getReference()
                    .child("Users")
                    .child(userId);

        }
        // Sempre inicializa prefs (mesmo sem user, para evitar NPE em testes)
        sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE);
    }

    // =============== NOVOS MÉTODOS: MODE/SESSION POR PACIENTE ===============

    /**
     * Salva um snapshot de leitura (direito ou esquerdo) em:
     * Users/{uid}/patients/{cpf}/{mode}/{sessionId}/
     * Este método é ESTÁTICO para você poder chamá-lo como já fez:
     * FirebaseHelper.saveSendDataForPatient(firebasehelper, snapshot, context, ev, cpf, mode, sessionId);
     */
    public static void saveSendDataForPatient(FirebaseHelper helper,
                                              Object sendDataSnapshot, // ConectInsole.SendData ou ConectInsole2.SendData
                                              Context ctx,
                                              List<String> eventlist,
                                              String cpf,
                                              String mode,
                                              String sessionId) {
        // Garante user logado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cpf == null || mode == null || sessionId == null) return;

        String uid = user.getUid();
        DatabaseReference base = FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child("Users")
                .child(uid)
                .child("patients")
                .child(cpf)
                .child(mode)
                .child(sessionId);

        // Pacote principal (séries e carimbo de hora de chegada)
        Map<String, Object> map = new HashMap<>();
        map.put("payload", sendDataSnapshot); // o próprio objeto com SR1..SR9, battery, etc.
        if (eventlist != null) map.put("events", eventlist);
        map.put("receivedAt", System.currentTimeMillis());

        if (NetworkUtils.isNetworkAvailable(ctx)) {
            base.updateChildren(map);
        } else {
            // offline: guarda no SharedPreferences com uma chave única
            savePatientSnapshotOffline(ctx, cpf, mode, sessionId, map);
        }
    }

    /**
     * Grava metadados da sessão (no STOP).
     * Caminho: Users/{uid}/patients/{cpf}/{mode}/{sessionId}/_meta
     * Exemplo de chamada: FirebaseHelper.saveExamSessionMeta(ctx, cpf, mode, sessionId, System.currentTimeMillis());
     */
    public static void saveExamSessionMeta(Context ctx,
                                           String cpf,
                                           String mode,
                                           String sessionId,
                                           long endTimestamp) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cpf == null || mode == null || sessionId == null) return;

        String uid = user.getUid();
        DatabaseReference metaRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child("Users")
                .child(uid)
                .child("patients")
                .child(cpf)
                .child(mode)
                .child(sessionId)
                .child("_meta");

        Map<String, Object> meta = new HashMap<>();
        meta.put("sessionId", sessionId);
        meta.put("mode", mode);
        meta.put("endedAt", endTimestamp);

        if (NetworkUtils.isNetworkAvailable(ctx)) {
            metaRef.updateChildren(meta);
        } else {
            // offline: guarda local para sincronizar depois
            savePatientSnapshotOffline(ctx, cpf, mode, sessionId, new HashMap<String, Object>() {{
                put("_meta", meta);
            }});
        }
    }

    // --------- Armazenamento offline para o NOVO fluxo ----------
    private static void savePatientSnapshotOffline(Context ctx,
                                                   String cpf,
                                                   String mode,
                                                   String sessionId,
                                                   Map<String, Object> payload) {
        SharedPreferences prefs = ctx.getSharedPreferences("offline_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = "patient_snapshot_" + cpf + "_" + mode + "_" + sessionId + "_" + System.currentTimeMillis();
        // Armazenando como String simples (payload.toString()); se quiser JSON real, pode usar Gson
        editor.putString(key, payload.toString());
        editor.apply();
    }

    /**
     * Sincroniza itens do novo fluxo salvos offline (aqueles com chave "patient_snapshot_...").
     * OBS: como salvamos payload como String (toString), aqui empurramos como texto bruto para um nó "offlineDump".
     * Se quiser reconstruir o objeto inteiro, mude o save offline para JSON (Gson) e desserialize aqui.
     */
    public void syncPatientOffline() {
        if (!NetworkUtils.isNetworkAvailable(context)) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        Map<String, ?> all = sharedPreferences.getAll();
        if (all == null || all.isEmpty()) return;

        DatabaseReference root = FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child("Users")
                .child(uid)
                .child("_offlineSync");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Map.Entry<String, ?> e : all.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k.startsWith("patient_snapshot_") && v instanceof String) {
                // Empurra para um nó de descarte (dump) mantendo a string
                String id = root.push().getKey();
                if (id != null) {
                    root.child(id).setValue(v);
                }
                editor.remove(k);
            }
        }
        editor.apply();
    }

    // ===================== MÉTODOS ANTIGOS (compatibilidade) =====================

    // Salva SendData (direito) no caminho antigo: Users/{uid}/DATA/{date}/{id}
    public void saveSendData(ConectInsole.SendData sendData, List<String> eventlist) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        if (mDatabase == null) {
            // Sem auth (UID nulo)
            saveSendDataLocally(sendData, currentDate);
            return;
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            String id = mDatabase.child("DATA").push().getKey();
            mDatabase.child("DATA").child(currentDate).child(id).setValue(sendData)
                    .addOnSuccessListener(aVoid -> System.out.println("SendData salvo no Firebase com sucesso!"))
                    .addOnFailureListener(e -> System.err.println("Erro ao salvar SendData: " + e.getMessage()));
        } else {
            saveSendDataLocally(sendData, currentDate);
        }
    }

    // Salva SendData2 (esquerdo) no caminho antigo: Users/{uid}/DATA2/{date}/{id}
    public void saveSendData2(ConectInsole2.SendData sendData) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        if (mDatabase == null) {
            saveSendData2Locally(sendData, currentDate);
            return;
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            String id = mDatabase.child("DATA2").push().getKey();
            mDatabase.child("DATA2").child(currentDate).child(id).setValue(sendData)
                    .addOnSuccessListener(aVoid -> System.out.println("SendData2 salvo no Firebase com sucesso!"))
                    .addOnFailureListener(e -> System.err.println("Erro ao salvar SendData2: " + e.getMessage()));
        } else {
            saveSendData2Locally(sendData, currentDate);
        }
    }

    // ====== OFFLINE LEGADO ======
    public void saveSendDataLocally(ConectInsole.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData_" + System.currentTimeMillis();
        editor.putString(key, sendData.toString());
        editor.apply();
    }

    public void saveSendData2Locally(ConectInsole2.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData2_" + System.currentTimeMillis();
        editor.putString(key, sendData.toString());
        editor.apply();
    }

    public void syncSendDataOffline() {
        if (!NetworkUtils.isNetworkAvailable(context) || mDatabase == null) return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean dataSynced = false;

        for (String key : sharedPreferences.getAll().keySet()) {
            String savedData = sharedPreferences.getString(key, null);
            if (savedData != null && key.startsWith("sendData_")) {
                String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String id = mDatabase.child("DATA").push().getKey();
                mDatabase.child("DATA").child(currentDate).child(id).setValue(savedData);
                editor.remove(key);
                dataSynced = true;
            }
        }
        if (dataSynced) editor.apply();
    }

    public void syncSendData2Offline() {
        if (!NetworkUtils.isNetworkAvailable(context) || mDatabase == null) return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean dataSynced = false;

        for (String key : sharedPreferences.getAll().keySet()) {
            String savedData = sharedPreferences.getString(key, null);
            if (savedData != null && key.startsWith("sendData2_")) {
                String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String id = mDatabase.child("DATA2").push().getKey();
                mDatabase.child("DATA2").child(currentDate).child(id).setValue(savedData);
                editor.remove(key);
                dataSynced = true;
            }
        }
        if (dataSynced) editor.apply();
    }
}

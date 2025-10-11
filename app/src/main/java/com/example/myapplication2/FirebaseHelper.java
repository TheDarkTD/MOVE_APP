package com.example.myapplication2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log; // <-- IMPORTAÇÃO ADICIONADA

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
    private static final String TAG = "FirebaseHelper"; // <-- TAG PARA LOGS
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
            Log.i(TAG, "FirebaseHelper inicializado para User ID: " + userId); // <-- LOG
        } else {
            Log.i(TAG, "FirebaseHelper inicializado sem usuário logado (mDatabase nulo)."); // <-- LOG
        }
        // Sempre inicializa prefs (mesmo sem user, para evitar NPE em testes)
        sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE);
    }

    // =============== NOVOS MÉTODOS: MODE/SESSION POR PACIENTE ===============

    /**
     * Salva um snapshot de leitura (direito ou esquerdo) em:
     * Users/{uid}/patients/{cpf}/{mode}/{sessionId}/
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
        if (user == null || cpf == null || mode == null || sessionId == null) {
            Log.e(TAG, "saveSendDataForPatient: Falha. Usuário não logado ou parâmetros nulos."); // <-- LOG
            return;
        }

        String uid = user.getUid();
        DatabaseReference base = FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child("Users")
                .child(uid)
                .child("patients")
                .child(cpf)
                .child(mode)
                .child(sessionId);

        Log.i(TAG, "saveSendDataForPatient: Tentando salvar dado para CPF=" + cpf + ", Mode=" + mode + ", Session=" + sessionId); // <-- LOG

        // Pacote principal (séries e carimbo de hora de chegada)
        Map<String, Object> map = new HashMap<>();
        map.put("payload", sendDataSnapshot); // o próprio objeto com SR1..SR9, battery, etc.
        if (eventlist != null) map.put("events", eventlist);
        map.put("receivedAt", System.currentTimeMillis());

        if (NetworkUtils.isNetworkAvailable(ctx)) {
            base.updateChildren(map)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveSendDataForPatient: Salvo ONLINE com sucesso!")) // <-- LOG
                    .addOnFailureListener(e -> Log.e(TAG, "saveSendDataForPatient: Erro ao salvar ONLINE: " + e.getMessage(), e)); // <-- LOG
        } else {
            // offline: guarda no SharedPreferences com uma chave única
            savePatientSnapshotOffline(ctx, cpf, mode, sessionId, map);
            Log.i(TAG, "saveSendDataForPatient: Salvo OFFLINE (snapshot) com sucesso."); // <-- LOG
        }
    }

    /**
     * Grava metadados da sessão (no STOP).
     */
    public static void saveExamSessionMeta(Context ctx,
                                           String cpf,
                                           String mode,
                                           String sessionId,
                                           long endTimestamp) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cpf == null || mode == null || sessionId == null) {
            Log.e(TAG, "saveExamSessionMeta: Falha. Usuário não logado ou parâmetros nulos."); // <-- LOG
            return;
        }

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

        Log.i(TAG, "saveExamSessionMeta: Tentando salvar meta para CPF=" + cpf + ", Session=" + sessionId); // <-- LOG

        Map<String, Object> meta = new HashMap<>();
        meta.put("sessionId", sessionId);
        meta.put("mode", mode);
        meta.put("endedAt", endTimestamp);

        if (NetworkUtils.isNetworkAvailable(ctx)) {
            metaRef.updateChildren(meta)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveExamSessionMeta: Meta salva ONLINE com sucesso!")) // <-- LOG
                    .addOnFailureListener(e -> Log.e(TAG, "saveExamSessionMeta: Erro ao salvar Meta ONLINE: " + e.getMessage(), e)); // <-- LOG
        } else {
            // offline: guarda local para sincronizar depois
            savePatientSnapshotOffline(ctx, cpf, mode, sessionId, new HashMap<String, Object>() {{
                put("_meta", meta);
            }});
            Log.i(TAG, "saveExamSessionMeta: Meta salva OFFLINE com sucesso."); // <-- LOG
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
        // Armazenando como String simples (payload.toString());
        editor.putString(key, payload.toString());
        editor.apply();
        Log.d(TAG, "savePatientSnapshotOffline: Dados salvos localmente com chave: " + key); // <-- LOG detalhado
    }

    /**
     * Sincroniza itens do novo fluxo salvos offline (aqueles com chave "patient_snapshot_...").
     */
    public void syncPatientOffline() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.i(TAG, "syncPatientOffline: Sem conexão. Sincronização adiada."); // <-- LOG
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "syncPatientOffline: Sem usuário logado. Não é possível sincronizar."); // <-- LOG
            return;
        }
        String uid = user.getUid();

        Map<String, ?> all = sharedPreferences.getAll();
        if (all == null || all.isEmpty()) {
            Log.i(TAG, "syncPatientOffline: Nenhuma snapshot de paciente offline encontrada."); // <-- LOG
            return;
        }

        DatabaseReference root = FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child("Users")
                .child(uid)
                .child("_offlineSync");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        int syncedCount = 0;
        for (Map.Entry<String, ?> e : all.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k.startsWith("patient_snapshot_") && v instanceof String) {
                // Empurra para um nó de descarte (dump) mantendo a string
                String id = root.push().getKey();
                if (id != null) {
                    root.child(id).setValue(v);
                    Log.d(TAG, "syncPatientOffline: Sincronizando e removendo chave: " + k); // <-- LOG detalhado
                    editor.remove(k);
                    syncedCount++;
                } else {
                    Log.e(TAG, "syncPatientOffline: Falha ao obter chave push para o nó _offlineSync."); // <-- LOG
                }
            }
        }
        editor.apply();
        Log.i(TAG, "syncPatientOffline: Sincronização de snapshots concluída. " + syncedCount + " itens processados."); // <-- LOG
    }

    // ===================== MÉTODOS ANTIGOS (compatibilidade) =====================

    // Salva SendData (direito) no caminho antigo: Users/{uid}/DATA/{date}/{id}
    public void saveSendData(ConectInsole.SendData sendData, List<String> eventlist) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Log.i(TAG, "saveSendData (LEGADO): Tentando salvar dado direito para data: " + currentDate); // <-- LOG

        if (mDatabase == null) {
            // Sem auth (UID nulo)
            saveSendDataLocally(sendData, currentDate);
            Log.w(TAG, "saveSendData (LEGADO): Usuário não logado. Salvando apenas localmente."); // <-- LOG (Warning)
            return;
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            String id = mDatabase.child("DATA").push().getKey();
            mDatabase.child("DATA").child(currentDate).child(id).setValue(sendData)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveSendData (LEGADO): Salvo no Firebase com sucesso!")) // <-- LOG
                    .addOnFailureListener(e -> Log.e(TAG, "saveSendData (LEGADO): Erro ao salvar: " + e.getMessage(), e)); // <-- LOG
        } else {
            saveSendDataLocally(sendData, currentDate);
            Log.i(TAG, "saveSendData (LEGADO): Sem conexão. Salvo localmente."); // <-- LOG
        }
    }

    // Salva SendData2 (esquerdo) no caminho antigo: Users/{uid}/DATA2/{date}/{id}
    public void saveSendData2(ConectInsole2.SendData sendData) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Log.i(TAG, "saveSendData2 (LEGADO): Tentando salvar dado esquerdo para data: " + currentDate); // <-- LOG

        if (mDatabase == null) {
            saveSendData2Locally(sendData, currentDate);
            Log.w(TAG, "saveSendData2 (LEGADO): Usuário não logado. Salvando apenas localmente."); // <-- LOG (Warning)
            return;
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            String id = mDatabase.child("DATA2").push().getKey();
            mDatabase.child("DATA2").child(currentDate).child(id).setValue(sendData)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveSendData2 (LEGADO): Salvo no Firebase com sucesso!")) // <-- LOG
                    .addOnFailureListener(e -> Log.e(TAG, "saveSendData2 (LEGADO): Erro ao salvar: " + e.getMessage(), e)); // <-- LOG
        } else {
            saveSendData2Locally(sendData, currentDate);
            Log.i(TAG, "saveSendData2 (LEGADO): Sem conexão. Salvo localmente."); // <-- LOG
        }
    }

    // ====== OFFLINE LEGADO ======
    public void saveSendDataLocally(ConectInsole.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData_" + System.currentTimeMillis();
        editor.putString(key, sendData.toString());
        editor.apply();
        Log.d(TAG, "saveSendDataLocally (LEGADO): Dado direito salvo localmente com chave: " + key); // <-- LOG detalhado
    }

    public void saveSendData2Locally(ConectInsole2.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData2_" + System.currentTimeMillis();
        editor.putString(key, sendData.toString());
        editor.apply();
        Log.d(TAG, "saveSendData2Locally (LEGADO): Dado esquerdo salvo localmente com chave: " + key); // <-- LOG detalhado
    }

    public void syncSendDataOffline() {
        if (!NetworkUtils.isNetworkAvailable(context)) return;
        if (mDatabase == null) {
            Log.e(TAG, "syncSendDataOffline (LEGADO): mDatabase é nulo. Não é possível sincronizar."); // <-- LOG
            return;
        }
        Log.i(TAG, "syncSendDataOffline (LEGADO): Iniciando sincronização de dados DATA."); // <-- LOG

        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean dataSynced = false;
        int syncedCount = 0;

        for (String key : sharedPreferences.getAll().keySet()) {
            String savedData = sharedPreferences.getString(key, null);
            if (savedData != null && key.startsWith("sendData_")) {
                String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String id = mDatabase.child("DATA").push().getKey();
                mDatabase.child("DATA").child(currentDate).child(id).setValue(savedData);
                Log.d(TAG, "syncSendDataOffline (LEGADO): Sincronizando e removendo chave: " + key); // <-- LOG detalhado
                editor.remove(key);
                dataSynced = true;
                syncedCount++;
            }
        }
        if (dataSynced) editor.apply();
        Log.i(TAG, "syncSendDataOffline (LEGADO): Sincronização de DATA concluída. " + syncedCount + " itens processados."); // <-- LOG
    }

    public void syncSendData2Offline() {
        if (!NetworkUtils.isNetworkAvailable(context)) return;
        if (mDatabase == null) {
            Log.e(TAG, "syncSendData2Offline (LEGADO): mDatabase é nulo. Não é possível sincronizar."); // <-- LOG
            return;
        }
        Log.i(TAG, "syncSendData2Offline (LEGADO): Iniciando sincronização de dados DATA2."); // <-- LOG

        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean dataSynced = false;
        int syncedCount = 0;

        for (String key : sharedPreferences.getAll().keySet()) {
            String savedData = sharedPreferences.getString(key, null);
            if (savedData != null && key.startsWith("sendData2_")) {
                String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String id = mDatabase.child("DATA2").push().getKey();
                mDatabase.child("DATA2").child(currentDate).child(id).setValue(savedData);
                Log.d(TAG, "syncSendData2Offline (LEGADO): Sincronizando e removendo chave: " + key); // <-- LOG detalhado
                editor.remove(key);
                dataSynced = true;
                syncedCount++;
            }
        }
        if (dataSynced) editor.apply();
        Log.i(TAG, "syncSendData2Offline (LEGADO): Sincronização de DATA2 concluída. " + syncedCount + " itens processados."); // <-- LOG
    }
}
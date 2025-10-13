package com.example.myapplication2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;   // (mantido por compat; não usamos events)
import java.util.Locale;
import java.util.Map;

public class FirebaseHelper {

    // ===================== CAMPOS =====================
    private static final String TAG = "FirebaseHelper";
    private static final String DB_URL = "https://bioapp-496ae-default-rtdb.firebaseio.com/";

    private DatabaseReference mDatabase;
    private String userId;

    private final Context context;
    private final SharedPreferences sharedPreferences; // offline (legado + novo)

    // ===================== ENUM LADO =====================
    public enum Side {
        RIGHT, LEFT;
        public String key() { return this == RIGHT ? "right" : "left"; }
    }

    // ===================== CONSTRUTOR =====================
    public FirebaseHelper(Context context) {
        this.context = context;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            mDatabase = FirebaseDatabase.getInstance(DB_URL)
                    .getReference()
                    .child("Users")
                    .child(userId);
            Log.i(TAG, "FirebaseHelper inicializado para User ID: " + userId);
        } else {
            Log.i(TAG, "FirebaseHelper inicializado sem usuário logado (mDatabase nulo).");
        }
        // Sempre inicializa prefs (mesmo sem user, para evitar NPE em testes)
        sharedPreferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE);
    }

    // ============================================================
    // ======== FLUXO NOVO: PACIENTE/MODO/SESSÃO + LADO ===========
    // Estrutura:
    // Users/{uid}/patients/{cpf}/{mode}/{sessionId}/{right|left}/payload
    // Users/{uid}/patients/{cpf}/{mode}/{sessionId}/{right|left}/receivedAt
    // ============================================================

    /**
     * Salva snapshot de leitura já separado por lado (RIGHT/LEFT) e sem "events".
     */
    public static void saveSendDataForPatientSide(
            FirebaseHelper helper,
            Object sendDataSnapshot,   // ConectInsole.SendData ou ConectInsole2.SendData
            Context ctx,
            String cpf,
            String mode,
            String sessionId,
            Side side
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cpf == null || mode == null || sessionId == null || side == null) {
            Log.e(TAG, "saveSendDataForPatientSide: parâmetros inválidos ou usuário não logado.");
            return;
        }

        String uid = user.getUid();
        String sideKey = side.key(); // "right" | "left"

        DatabaseReference base = FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child("Users").child(uid)
                .child("patients").child(cpf)
                .child(mode)
                .child(sessionId)
                .child(sideKey);

        Map<String, Object> map = new HashMap<>();
        map.put("payload", sendDataSnapshot);
        map.put("receivedAt", System.currentTimeMillis());

        if (NetworkUtils.isNetworkAvailable(ctx)) {
            base.updateChildren(map)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveSendDataForPatientSide: OK online (" + sideKey + ")"))
                    .addOnFailureListener(e -> Log.e(TAG, "saveSendDataForPatientSide: erro online: " + e.getMessage(), e));
        } else {
            savePatientSnapshotOfflineSide(ctx, cpf, mode, sessionId, sideKey, map);
            Log.i(TAG, "saveSendDataForPatientSide: salvo OFFLINE (snapshot " + sideKey + ").");
        }
    }

    /**
     * (Opcional) Metadados da sessão por lado.
     * Path: Users/{uid}/patients/{cpf}/{mode}/{sessionId}/{right|left}/_meta
     */
    public static void saveExamSessionMetaSide(Context ctx,
                                               String cpf,
                                               String mode,
                                               String sessionId,
                                               Side side,
                                               long endTimestamp) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cpf == null || mode == null || sessionId == null || side == null) {
            Log.e(TAG, "saveExamSessionMetaSide: parâmetros inválidos.");
            return;
        }

        String uid = user.getUid();
        DatabaseReference metaRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference()
                .child("Users").child(uid)
                .child("patients").child(cpf)
                .child(mode).child(sessionId)
                .child(side.key())
                .child("_meta");

        Map<String, Object> meta = new HashMap<>();
        meta.put("sessionId", sessionId);
        meta.put("mode", mode);
        meta.put("side", side.key());
        meta.put("endedAt", endTimestamp);

        if (NetworkUtils.isNetworkAvailable(ctx)) {
            metaRef.updateChildren(meta)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveExamSessionMetaSide: OK (" + side.key() + ")"))
                    .addOnFailureListener(e -> Log.e(TAG, "saveExamSessionMetaSide: erro: " + e.getMessage(), e));
        } else {
            savePatientSnapshotOfflineSide(ctx, cpf, mode, sessionId, side.key(), new HashMap<String, Object>() {{
                put("_meta", meta);
            }});
            Log.i(TAG, "saveExamSessionMetaSide: salvo OFFLINE.");
        }
    }

    // ---------- Armazenamento OFFLINE para o NOVO fluxo ----------
    private static void savePatientSnapshotOfflineSide(Context ctx,
                                                       String cpf,
                                                       String mode,
                                                       String sessionId,
                                                       String sideKey,
                                                       Map<String, Object> payload) {
        SharedPreferences prefs = ctx.getSharedPreferences("offline_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = "patient_snapshot_" + cpf + "_" + mode + "_" + sessionId + "_" + sideKey + "_" + System.currentTimeMillis();
        editor.putString(key, payload.toString());
        editor.apply();
        Log.d(TAG, "savePatientSnapshotOfflineSide: salvo local (" + key + ")");
    }

    /**
     * Sincroniza itens do novo fluxo salvos offline (com chave "patient_snapshot_...").
     * OBS: Este método mantém o mesmo comportamento anterior — faz dump em Users/{uid}/_offlineSync.
     * Caso queira regravar exatamente no path final, seria preciso serializar o conteúdo (em vez de String).
     */
    public void syncPatientOffline() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.i(TAG, "syncPatientOffline: Sem conexão. Adiado.");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "syncPatientOffline: Sem usuário logado.");
            return;
        }
        String uid = user.getUid();

        Map<String, ?> all = sharedPreferences.getAll();
        if (all == null || all.isEmpty()) {
            Log.i(TAG, "syncPatientOffline: Nenhum snapshot offline encontrado.");
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
                String id = root.push().getKey();
                if (id != null) {
                    root.child(id).setValue(v);
                    Log.d(TAG, "syncPatientOffline: dump de " + k);
                    editor.remove(k);
                    syncedCount++;
                } else {
                    Log.e(TAG, "syncPatientOffline: Falha ao obter chave push.");
                }
            }
        }
        editor.apply();
        Log.i(TAG, "syncPatientOffline: concluído. " + syncedCount + " itens enviados.");
    }

    // ============================================================
    // ============== FLUXO LEGADO (DATA / DATA2) =================
    // Mantido para compatibilidade enquanto você migra telas/lógicas.
    // ============================================================

    // Salva SendData (direito) no caminho antigo: Users/{uid}/DATA/{date}/{id}
    public void saveSendData(ConectInsole.SendData sendData, List<String> eventlist /* não usado */) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Log.i(TAG, "saveSendData (LEGADO): data = " + currentDate);

        if (mDatabase == null) {
            saveSendDataLocally(sendData, currentDate);
            Log.w(TAG, "saveSendData (LEGADO): sem usuário logado. Salvando local.");
            return;
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            String id = mDatabase.child("DATA").push().getKey();
            mDatabase.child("DATA").child(currentDate).child(id).setValue(sendData)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveSendData (LEGADO): OK Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "saveSendData (LEGADO): erro: " + e.getMessage(), e));
        } else {
            saveSendDataLocally(sendData, currentDate);
            Log.i(TAG, "saveSendData (LEGADO): sem conexão. Local OK.");
        }
    }

    // Salva SendData2 (esquerdo) no caminho antigo: Users/{uid}/DATA2/{date}/{id}
    public void saveSendData2(ConectInsole2.SendData sendData) {
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Log.i(TAG, "saveSendData2 (LEGADO): data = " + currentDate);

        if (mDatabase == null) {
            saveSendData2Locally(sendData, currentDate);
            Log.w(TAG, "saveSendData2 (LEGADO): sem usuário logado. Salvando local.");
            return;
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            String id = mDatabase.child("DATA2").push().getKey();
            mDatabase.child("DATA2").child(currentDate).child(id).setValue(sendData)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveSendData2 (LEGADO): OK Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "saveSendData2 (LEGADO): erro: " + e.getMessage(), e));
        } else {
            saveSendData2Locally(sendData, currentDate);
            Log.i(TAG, "saveSendData2 (LEGADO): sem conexão. Local OK.");
        }
    }

    // ====== OFFLINE LEGADO ======
    public void saveSendDataLocally(ConectInsole.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData_" + System.currentTimeMillis();
        editor.putString(key, sendData.toString());
        editor.apply();
        Log.d(TAG, "saveSendDataLocally (LEGADO): salvo local (" + key + ")");
    }

    public void saveSendData2Locally(ConectInsole2.SendData sendData, String currentDate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String key = "sendData2_" + System.currentTimeMillis();
        editor.putString(key, sendData.toString());
        editor.apply();
        Log.d(TAG, "saveSendData2Locally (LEGADO): salvo local (" + key + ")");
    }

    public void syncSendDataOffline() {
        if (!NetworkUtils.isNetworkAvailable(context)) return;
        if (mDatabase == null) {
            Log.e(TAG, "syncSendDataOffline (LEGADO): mDatabase é nulo.");
            return;
        }
        Log.i(TAG, "syncSendDataOffline (LEGADO): iniciando...");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        int syncedCount = 0;

        for (String key : sharedPreferences.getAll().keySet()) {
            String savedData = sharedPreferences.getString(key, null);
            if (savedData != null && key.startsWith("sendData_")) {
                String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String id = mDatabase.child("DATA").push().getKey();
                mDatabase.child("DATA").child(currentDate).child(id).setValue(savedData);
                Log.d(TAG, "syncSendDataOffline (LEGADO): enviado e removido " + key);
                editor.remove(key);
                syncedCount++;
            }
        }
        editor.apply();
        Log.i(TAG, "syncSendDataOffline (LEGADO): concluído. " + syncedCount + " itens.");
    }

    public void syncSendData2Offline() {
        if (!NetworkUtils.isNetworkAvailable(context)) return;
        if (mDatabase == null) {
            Log.e(TAG, "syncSendData2Offline (LEGADO): mDatabase é nulo.");
            return;
        }
        Log.i(TAG, "syncSendData2Offline (LEGADO): iniciando...");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        int syncedCount = 0;

        for (String key : sharedPreferences.getAll().keySet()) {
            String savedData = sharedPreferences.getString(key, null);
            if (savedData != null && key.startsWith("sendData2_")) {
                String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                String id = mDatabase.child("DATA2").push().getKey();
                mDatabase.child("DATA2").child(currentDate).child(id).setValue(savedData);
                Log.d(TAG, "syncSendData2Offline (LEGADO): enviado e removido " + key);
                editor.remove(key);
                syncedCount++;
            }
        }
        editor.apply();
        Log.i(TAG, "syncSendData2Offline (LEGADO): concluído. " + syncedCount + " itens.");
    }

    // ============================================================
    // ============= COMPAT: MÉTODO ANTIGO (DEPRECATED) ===========
    // ============================================================

    /**
     * Compatibilidade temporária — redireciona para RIGHT (USD).
     * Remova quando atualizar todas as chamadas para saveSendDataForPatientSide(...).
     */
    @Deprecated
    public static void saveSendDataForPatient(
            FirebaseHelper helper,
            Object sendDataSnapshot,
            Context ctx,
            String cpf,
            String mode,
            String sessionId
    ) {
        saveSendDataForPatientSide(helper, sendDataSnapshot, ctx, cpf, mode, sessionId, Side.RIGHT);
    }

    // ============================================================
    // ============= (OPCIONAL) META ÚNICO DE SESSÃO ==============
    // Mantém seu método antigo se quiser meta sem distinguir lado.
    // ============================================================

    /**
     * Meta de sessão única (sem distinção de lado). Mantido do seu código.
     * Path: Users/{uid}/patients/{cpf}/{mode}/{sessionId}/_meta
     */
    public static void saveExamSessionMeta(Context ctx,
                                           String cpf,
                                           String mode,
                                           String sessionId,
                                           long endTimestamp) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || cpf == null || mode == null || sessionId == null) {
            Log.e(TAG, "saveExamSessionMeta: Falha. Usuário não logado ou parâmetros nulos.");
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

        Log.i(TAG, "saveExamSessionMeta: CPF=" + cpf + ", Session=" + sessionId);

        Map<String, Object> meta = new HashMap<>();
        meta.put("sessionId", sessionId);
        meta.put("mode", mode);
        meta.put("endedAt", endTimestamp);

        if (NetworkUtils.isNetworkAvailable(ctx)) {
            metaRef.updateChildren(meta)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "saveExamSessionMeta: Meta salva ONLINE"))
                    .addOnFailureListener(e -> Log.e(TAG, "saveExamSessionMeta: Erro ao salvar Meta: " + e.getMessage(), e));
        } else {
            // offline: salva dump local
            savePatientSnapshotOfflineSide(ctx, cpf, mode, sessionId, "sessionMeta",
                    new HashMap<String, Object>() {{ put("_meta", meta); }});
            Log.i(TAG, "saveExamSessionMeta: Meta salva OFFLINE.");
        }
    }
}

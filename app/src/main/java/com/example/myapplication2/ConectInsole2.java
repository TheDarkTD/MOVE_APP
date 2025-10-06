package com.example.myapplication2;

import static android.content.Context.MODE_PRIVATE;

import static androidx.core.content.SharedPreferencesKt.edit;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConectInsole2 {
    private static final String TAG = "ConectInsole2";
    private static final String CHANNEL_ID = "notify_pressure";

    private final OkHttpClient client = new OkHttpClient();
    private final FirebaseHelper firebaseHelper;
    private final SharedPreferences prefsConfig;
    private final String baseUrl;
    private final ConectVibra conectar;
    private boolean spikeOnCooldown = false;
    private boolean pendingSpike    = false;
    private final Handler cooldownHandler = new Handler(Looper.getMainLooper());
    private Boolean recebimentoinsole2 = true;
    private Boolean envioinsole2 = true;
    private Calendar calendar;
    private SendData receivedData = new SendData();

    public ConectInsole2(@NonNull Context context) {
        Log.d(TAG, "Constructor: initializing ConectInsole2");
        firebaseHelper = new FirebaseHelper(context);
        prefsConfig = context.getSharedPreferences("My_Appips", MODE_PRIVATE);
        baseUrl = "http://" + prefsConfig.getString("IP2", "");
        Log.d(TAG, "Base URL loaded: " + baseUrl);

        conectar = new ConectVibra(context);
    }

    public static class ConfigData {
        public int cmd, freq;
        public int S1, S2, S3, S4, S5, S6, S7, S8, S9;

        @Override
        public String toString() {
            return "ConfigData{" +
                    "cmd=" + cmd +
                    ", freq=" + freq +
                    ", S1=" + S1 +
                    ", S2=" + S2 +
                    ", S3=" + S3 +
                    ", S4=" + S4 +
                    ", S5=" + S5 +
                    ", S6=" + S6 +
                    ", S7=" + S7 +
                    ", S8=" + S8 +
                    ", S9=" + S9 +
                    '}';
        }
    }

    public static class SendData {
        public int cmd, battery, sensorTrigger;
        public int hour, minute, second, millisecond;
        public ArrayList<Integer> SR1 = new ArrayList<>();
        public ArrayList<Integer> SR2 = new ArrayList<>();
        public ArrayList<Integer> SR3 = new ArrayList<>();
        public ArrayList<Integer> SR4 = new ArrayList<>();
        public ArrayList<Integer> SR5 = new ArrayList<>();
        public ArrayList<Integer> SR6 = new ArrayList<>();
        public ArrayList<Integer> SR7 = new ArrayList<>();
        public ArrayList<Integer> SR8 = new ArrayList<>();
        public ArrayList<Integer> SR9 = new ArrayList<>();
    }

    public void createAndSendConfigData(byte kcmd, byte kfreq,
                                        short kS1, short kS2, short kS3,
                                        short kS4, short kS5, short kS6,
                                        short kS7, short kS8, short kS9) {
        Log.d(TAG, "createAndSendConfigData called with cmd=" + kcmd + " freq=" + kfreq);
        ConfigData configData = new ConfigData();
        configData.cmd = kcmd;
        configData.freq = (byte)10;
        configData.S1 = kS1;
        configData.S2 = kS2;
        configData.S3 = kS3;
        configData.S4 = kS4;
        configData.S5 = kS5;
        configData.S6 = kS6;
        configData.S7 = kS7;
        configData.S8 = kS8;
        configData.S9 = kS9;
        Log.d(TAG, "ConfigData to send: " + configData.toString());
        sendConfigData(configData);
    }

    private void sendConfigData(ConfigData cfg) {

        Log.d(TAG, "sendConfigData: building payload");
        StringBuilder data = new StringBuilder();
        data.append(cfg.cmd).append(",")
                .append(cfg.freq).append(",")
                .append(cfg.S1).append(",")
                .append(cfg.S2).append(",")
                .append(cfg.S3).append(",")
                .append(cfg.S4).append(",")
                .append(cfg.S5).append(",")
                .append(cfg.S6).append(",")
                .append(cfg.S7).append(",")
                .append(cfg.S8).append(",")
                .append(cfg.S9);
        Log.d(TAG, "Payload: " + data);

        RequestBody body = new FormBody.Builder()
                .add("config_data", data.toString())
                .build();

        Request request = new Request.Builder()
                .url(baseUrl + "/config")
                .post(body)
                .build();
        client.newCall(request).enqueue(new LoggingCallback("sendConfigData"));
    }

    public void checkForNewData(Context context) {
        String checkUrl = baseUrl + "/check";
        Log.d(TAG, "checkForNewData: URL=" + checkUrl);
        Request request = new Request.Builder()
                .url(checkUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "checkForNewData failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "checkForNewData response code=" + response.code());
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Log.d(TAG, "checkForNewData raw: " + responseData);
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        boolean newData = jsonObject.getBoolean("newData");
                        Log.d(TAG, "newData flag=" + newData);
                        if (newData) {
                            receiveData(context);
                            Log.d(TAG, "Data capture triggered");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse error in checkForNewData", e);
                    }
                } else {
                    Log.e(TAG, "checkForNewData non-success: " + response.message());
                }
            }
        });
    }

    public void receiveData(Context ctx) {
        String dataUrl = baseUrl + "/data";
        Log.d(TAG, "receiveData: URL=" + dataUrl);
        Request request = new Request.Builder()
                .url(dataUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "receiveData failure", e);
                recebimentoinsole2=false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "receiveData response code=" + response.code());
                if (!response.isSuccessful()) {
                    Log.e(TAG, "receiveData failed: " + response.message());
                    return;
                }
                try {
                    recebimentoinsole2=true;
                    String json = response.body().string();
                    Log.d(TAG, "receiveData raw JSON: " + json);
                    parseJson(json);

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    Log.d(TAG, "User authenticated? " + (user != null));
                    if (user != null) {
                        if (isNetworkAvailable(ctx)) {
                            Log.d(TAG, "Network available: saving to Firebase");
                            firebaseHelper.saveSendData2(receivedData);
                        } else {
                            String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                            Log.d(TAG, "Network unavailable: saving locally with date=" + today);
                            firebaseHelper.saveSendData2Locally(receivedData, today);
                            runToast(ctx, "Sem conexão. Dados salvos localmente.");
                        }
                    }

                    storeReadings(ctx);
                    Log.d(TAG, "Readings stored locally");

                    if (receivedData.cmd == 0x3D) {
                        Log.d(TAG, "Evento: pico de pressão (cmd=0x3D)");

                        // lê parâmetros
                        SharedPreferences prefs = ctx.getSharedPreferences("My_Appvibra", MODE_PRIVATE);
                        byte INT    = Byte.parseByte(prefs.getString("int",       "0"));
                        byte PEST   = Byte.parseByte(prefs.getString("pulse",     "0"));
                        short INEST = Short.parseShort(prefs.getString("interval",  "0"));
                        short TMEST = Short.parseShort(prefs.getString("time",      "0"));

                        // função para enviar o comando
                        Runnable sendSpike = () -> {
                            Log.d(TAG, "Enviando comando de pico (0x1B) — PEST=" + PEST
                                    + ", INT=" + INT + ", TMEST=" + TMEST + ", INEST=" + INEST);
                            conectar.SendConfigData((byte)0x1B, PEST, INT, TMEST, INEST);
                        };

                        if (!spikeOnCooldown) {
                            // 1º envio imediato e entra em cooldown
                            sendSpike.run();
                            spikeOnCooldown = true;

                            // agenda término do cooldown: após TMEST, libera novo envio
                            cooldownHandler.postDelayed(() -> {
                                spikeOnCooldown = false;
                                Log.d(TAG, "Cooldown finalizado — pronto para novo spike");
                            }, TMEST);

                        } else {
                            // em cooldown: ignora qualquer spike extra
                            Log.d(TAG, "Spike recebido durante cooldown — ignorado");
                        }


                        /*createNotificationChannel(ctx);
                        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        NotificationManagerCompat.from(ctx).notify(2, buildNotification(ctx));
                        Log.d(TAG, "Notification dispatched");*/
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "JSON parse error in receiveData", e);
                }
            }
        });
    }

    private void parseJson(String json) throws JSONException {
        Log.d(TAG, "parseJson: starting parse");
        JSONObject j = new JSONObject(json);
        receivedData.cmd = j.getInt("cmd");
        calendar = Calendar.getInstance();
        receivedData.hour = calendar.get(Calendar.HOUR_OF_DAY);
        receivedData.minute = calendar.get(Calendar.MINUTE);
        receivedData.second = calendar.get(Calendar.SECOND);
        receivedData.millisecond = calendar.get(Calendar.MILLISECOND);
        receivedData.battery = j.getInt("battery");
        Log.d(TAG, String.format("Parsed metadata cmd=%d battery=%d", receivedData.cmd, receivedData.battery));

        JSONArray sensorsReads = j.getJSONArray("sensors_reads");
        receivedData.SR1.clear(); receivedData.SR2.clear(); receivedData.SR3.clear();
        receivedData.SR4.clear(); receivedData.SR5.clear(); receivedData.SR6.clear();
        receivedData.SR7.clear(); receivedData.SR8.clear(); receivedData.SR9.clear();
        for (int i=0; i<sensorsReads.length(); i++) {
            JSONObject sensorRead = sensorsReads.getJSONObject(i);
            receivedData.SR1.add(sensorRead.getInt("S1"));
            receivedData.SR2.add(sensorRead.getInt("S2"));
            receivedData.SR3.add(sensorRead.getInt("S3"));
            receivedData.SR4.add(sensorRead.getInt("S4"));
            receivedData.SR5.add(sensorRead.getInt("S5"));
            receivedData.SR6.add(sensorRead.getInt("S6"));
            receivedData.SR7.add(sensorRead.getInt("S7"));
            receivedData.SR8.add(sensorRead.getInt("S8"));
            receivedData.SR9.add(sensorRead.getInt("S9"));
        }
        Log.d(TAG, String.format("parseJson: SR lengths %d,%d,%d,%d,%d,%d,%d,%d,%d",
                receivedData.SR1.size(), receivedData.SR2.size(), receivedData.SR3.size(),
                receivedData.SR4.size(), receivedData.SR5.size(), receivedData.SR6.size(),
                receivedData.SR7.size(), receivedData.SR8.size(), receivedData.SR9.size()));
    }

    private void storeReadings(Context ctx) {
        Log.d(TAG, "storeReadings: saving to SharedPreferences");
        SharedPreferences sp = ctx.getSharedPreferences("My_Appinsolereadings2", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("S1_2", receivedData.SR1.toString());
        editor.putString("S2_2", receivedData.SR2.toString());
        editor.putString("S3_2", receivedData.SR3.toString());
        editor.putString("S4_2", receivedData.SR4.toString());
        editor.putString("S5_2", receivedData.SR5.toString());
        editor.putString("S6_2", receivedData.SR6.toString());
        editor.putString("S7_2", receivedData.SR7.toString());
        editor.putString("S8_2", receivedData.SR8.toString());
        editor.putString("S9_2", receivedData.SR9.toString());
        editor.apply();
    }
    private ConfigData configData;
    // Método para substituir os valores da ConfigData
    public void setConfigData2(ConfigData configData) {
        if (configData != null) {
            // Loga o estado atual do objeto interno
            if (this.configData == null) {
                Log.d(TAG, "this.configData is null, creating new instance.");
                this.configData = new ConfigData();
            } else {
                Log.d(TAG, "this.configData exists before substitution: " + this.configData.toString());
            }

            // Loga os novos valores que serão aplicados
            Log.d(TAG, "Substituting new ConfigData: " + configData.toString());

            // Copia os valores do objeto recebido para a instância interna
            this.configData.S1 = configData.S1;
            this.configData.S2 = configData.S2;
            this.configData.S3 = configData.S3;
            this.configData.S4 = configData.S4;
            this.configData.S5 = configData.S5;
            this.configData.S6 = configData.S6;
            this.configData.S7 = configData.S7;
            this.configData.S8 = configData.S8;
            this.configData.S9 = configData.S9;

            // Loga o estado final após a substituição
            Log.d("ConectInsole2", "After substitution, this.configData: " + this.configData.toString());
        } else {
            Log.d("ConectInsole2", "Received null ConfigData, skipping substitution.");
        }
    }
    private boolean isNetworkAvailable(Context ctx) {
        Log.d(TAG, "isNetworkAvailable: checking connectivity");
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            boolean available = (ni != null && ni.isConnected());
            Log.d(TAG, "Network available=" + available);
            return available;
        }
        return false;
    }

    private void runToast(Context ctx, String msg) {
        Log.d(TAG, "runToast: message='" + msg + "'");
        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }

    private static class LoggingCallback implements Callback {
        private final String name;
        boolean envioinsole2 = true;

        LoggingCallback(String name) {
            this.name = name;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, name + " failed", e);
            envioinsole2 = false;

        }

        @Override
        public void onResponse(Call call, Response response) {
            if (!response.isSuccessful()) Log.e(TAG, name + " error: " + response.message());
            else Log.d(TAG, name + " success");
        }
    }
}

package com.example.myapplication2;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication2.Home.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Locale;
import java.util.UUID;

// Assumindo FirebaseHelper, ConectVibra, NetworkUtils.
public class ConectInsole {

    HomeActivity home = new HomeActivity();
    // Metadados da sessão (preenchidos pela HomeActivity ao iniciar)
    private String currentCpf;
    private String currentMode;
    private String currentSessionId;

    private static final String TAG = "ConectInsoleBLE";
    public static final String CHANNEL_ID = "notify_pressure";
    private static final UUID SERVICE_UUID = UUID.fromString("4FAF0101-FBCF-4309-8A1C-8472B7098485");
    private static final UUID CHARACTERISTIC_CONFIG_UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A8");
    private static final UUID CHARACTERISTIC_DATA_UUID   = UUID.fromString("AEB5483E-36E1-4688-B7F5-EA07361B26A9");
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String ESP32_BLE_NAME = "USD";

    // BLE
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bluetoothScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean isScanning = false;
    private boolean isConnected = false;
    private boolean isConnecting = false;

    // Buffer & controle
    private final Object bufferLock = new Object();
    private boolean isBufferingEnabled = false; // << habilita/desabilita acúmulo em memória

    // Classe / helpers
    private final Context context;
    private final ConectVibra conectar;
    private final FirebaseHelper firebasehelper;
    private SendData receivedData;
    private List<String> eventlist = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ======= Models =======
    public static class ConfigData {
        public int cmd, freq;
        public int S1, S2, S3, S4, S5, S6, S7, S8, S9;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                    "cmd=0x%02X, freq=%d, S1=%d ...", cmd, freq, S1);
        }
        public byte[] toBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte) cmd);
            buffer.put((byte) freq);
            buffer.putShort((short) S1);
            buffer.putShort((short) S2);
            buffer.putShort((short) S3);
            buffer.putShort((short) S4);
            buffer.putShort((short) S5);
            buffer.putShort((short) S6);
            buffer.putShort((short) S7);
            buffer.putShort((short) S8);
            buffer.putShort((short) S9);
            return buffer.array();
        }
    }

    public static class SendData {
        public int cmd, hour, minute, second, millisecond, battery;
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

    // ======= Ctor =======
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public ConectInsole(@NonNull Context context) {
        this.context = context;
        Log.d(TAG, "Constructor: initializing ConectInsole (BLE Scan)");
        receivedData = new SendData();
        firebasehelper = new FirebaseHelper(context);
        conectar = new ConectVibra(context);

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth não está disponível ou habilitado.");
            bluetoothScanner = null;
            return;
        }

        bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        startScanning();
    }

    // ======= Permissões =======
    private boolean checkBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // ======= Scan/Connect =======
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScanning() {
        if (bluetoothScanner == null || isScanning || isConnecting || !checkBlePermissions()) {
            if (!isScanning && !isConnecting) {
                Log.e(TAG, "Falha ao iniciar scan. Verifique BLE/Permissões.");
            }
            handler.postDelayed(this::startScanning, 5000);
            return;
        }

        isScanning = true;
        Log.d(TAG, "Iniciando a busca pelo dispositivo: " + ESP32_BLE_NAME);
        bluetoothScanner.startScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && device.getName().equals(ESP32_BLE_NAME) && !isConnecting) {
                Log.d(TAG, "Dispositivo encontrado: " + ESP32_BLE_NAME + " - Conectando...");
                if (bluetoothScanner != null) bluetoothScanner.stopScan(this);
                isScanning = false;
                connectToDevice(device);
            }
        }
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan BLE falhou: " + errorCode);
            isScanning = false;
            handler.postDelayed(ConectInsole.this::startScanning, 2000);
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToDevice(BluetoothDevice device) {
        if (device == null || isConnecting) {
            Log.w(TAG, "Conexão rejeitada: já em processo de conexão.");
            return;
        }
        if (checkBlePermissions()) {
            isConnecting = true;
            bluetoothGatt = device.connectGatt(context, true, gattCallback);
            Log.d(TAG, "Tentando conectar ao GATT: " + device.getAddress());
        }
    }

    // ======= GATT Callback =======
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                isConnecting = false;
                Log.d(TAG, "Conectado ao GATT Server. Descobrindo serviços...");
                gatt.requestMtu(247); // robustez (mesmo com payload 19B)
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                isConnecting = false;
                Log.d(TAG, "Desconectado do GATT Server. Reiniciando a busca...");
                if (bluetoothGatt != null) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
                handler.postDelayed(ConectInsole.this::startScanning, 2000);
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Serviços descobertos. Habilitando notificações...");
                setCharacteristicNotification(gatt, CHARACTERISTIC_DATA_UUID, true);
            } else {
                Log.w(TAG, "Falha na descoberta de serviços: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "ConfigData enviado com sucesso.");
            } else {
                Log.e(TAG, "Falha ao enviar ConfigData: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (CHARACTERISTIC_DATA_UUID.equals(characteristic.getUuid())) {
                byte[] p = characteristic.getValue();
                if (p == null || p.length != 19) {
                    Log.e(TAG, "Pacote inválido. Esperado 19 bytes (battery + 9*uint16). Veio: " + (p == null ? 0 : p.length));
                    return;
                }
                parseSingleSample(p); // SEMPRE guarda SharedPreferences e atualiza UI
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void setCharacteristicNotification(BluetoothGatt gatt, UUID characteristicUuid, boolean enable) {
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) { Log.e(TAG, "Serviço não encontrado."); return; }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
        if (characteristic == null) { Log.e(TAG, "Característica não encontrada."); return; }

        if (checkBlePermissions()) {
            gatt.setCharacteristicNotification(characteristic, enable);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
            if (descriptor != null) {
                byte[] value = enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                descriptor.setValue(value);
                gatt.writeDescriptor(descriptor);
                Log.d(TAG, "Notificação " + (enable ? "ativada" : "desativada"));
            }
        }
    }

    // ======= Parser de 1 leitura (19B) =======
    private void parseSingleSample(byte[] p) {
        ByteBuffer bb = ByteBuffer.wrap(p).order(ByteOrder.LITTLE_ENDIAN);

        int bat = bb.get() & 0xFF;
        int s1 = bb.getShort() & 0xFFFF;
        int s2 = bb.getShort() & 0xFFFF;
        int s3 = bb.getShort() & 0xFFFF;
        int s4 = bb.getShort() & 0xFFFF;
        int s5 = bb.getShort() & 0xFFFF;
        int s6 = bb.getShort() & 0xFFFF;
        int s7 = bb.getShort() & 0xFFFF;
        int s8 = bb.getShort() & 0xFFFF;
        int s9 = bb.getShort() & 0xFFFF;

        // 1) Atualiza timestamp do recebimento
        updateTimestamp();

        // 2) Sempre persiste nos SharedPreferences (para a UI)
        //    A UI (home.loadColorsR) depende do storeReadings.
        synchronized (bufferLock) {
            // Atualiza sempre a bateria "corrente"
            receivedData.battery = bat;

            // Acúmulo em memória condicionado ao "buffer enable"
            if (isBufferingEnabled) {
                receivedData.SR1.add(s1);
                receivedData.SR2.add(s2);
                receivedData.SR3.add(s3);
                receivedData.SR4.add(s4);
                receivedData.SR5.add(s5);
                receivedData.SR6.add(s6);
                receivedData.SR7.add(s7);
                receivedData.SR8.add(s8);
                receivedData.SR9.add(s9);
            }
        }

        // Persistência “rolling” para a UI ler
        storeReadings(context);

        // Atualiza visual (lê do SharedPreferences)
        home.loadColorsR();
    }

    // ======= API pública: habilitar/desabilitar buffer =======
    /** Liga ou desliga o acúmulo em memória (receivedData.SR1..SR9). */
    public void enableBuffering(boolean enable) {
        synchronized (bufferLock) {
            isBufferingEnabled = enable;
        }
        Log.d(TAG, "Buffering " + (enable ? "habilitado" : "desabilitado"));
    }

    /** Retorna se o acúmulo em memória está habilitado. */
    public boolean isBufferingEnabled() {
        synchronized (bufferLock) { return isBufferingEnabled; }
    }

    // ======= Flush manual: envia snapshot e zera buffer =======
    /** Envia o buffer atual para a nuvem (ou local) e zera as listas. */
    public void flushToCloudNow() {
        final SendData snapshot;
        final ArrayList<String> evSnapshot;

        synchronized (bufferLock) {
            snapshot = new SendData();
            snapshot.cmd  = receivedData.cmd;
            snapshot.hour = receivedData.hour;
            snapshot.minute = receivedData.minute;
            snapshot.second = receivedData.second;
            snapshot.millisecond = receivedData.millisecond;
            snapshot.battery = receivedData.battery;

            snapshot.SR1 = new ArrayList<>(receivedData.SR1);
            snapshot.SR2 = new ArrayList<>(receivedData.SR2);
            snapshot.SR3 = new ArrayList<>(receivedData.SR3);
            snapshot.SR4 = new ArrayList<>(receivedData.SR4);
            snapshot.SR5 = new ArrayList<>(receivedData.SR5);
            snapshot.SR6 = new ArrayList<>(receivedData.SR6);
            snapshot.SR7 = new ArrayList<>(receivedData.SR7);
            snapshot.SR8 = new ArrayList<>(receivedData.SR8);
            snapshot.SR9 = new ArrayList<>(receivedData.SR9);

            evSnapshot = new ArrayList<>(eventlist);

            // Zera o buffer para próxima janela
            receivedData.SR1.clear();
            receivedData.SR2.clear();
            receivedData.SR3.clear();
            receivedData.SR4.clear();
            receivedData.SR5.clear();
            receivedData.SR6.clear();
            receivedData.SR7.clear();
            receivedData.SR8.clear();
            receivedData.SR9.clear();
            eventlist.clear();
        }

        // Chamada fora da região crítica
        updateTimestamp(); // timestamp do momento do flush (opcional)
        FirebaseHelper.saveSendDataForPatient(firebasehelper, snapshot, context, evSnapshot, currentCpf, currentMode, currentSessionId);
        Log.d(TAG, "flushToCloudNow: buffer enviado e zerado.");
    }
    public void setSessionMeta(String cpf, String mode, String sessionId) {
        this.currentCpf = cpf;
        this.currentMode = mode;
        this.currentSessionId = sessionId;
        Log.d(TAG, "setSessionMeta: cpf=" + cpf + ", mode=" + mode + ", sessionId=" + sessionId);
    }

    // ======= Envio de configuração (cmd/freq/máscaras) =======
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void createAndSendConfigData(byte kcmd, byte kfreq,
                                        short kS1, short kS2, short kS3,
                                        short kS4, short kS5, short kS6,
                                        short kS7, short kS8, short kS9) {
        Log.d(TAG, String.format(Locale.getDefault(),"createAndSendConfigData: cmd=0x%02X, freq=%d", kcmd, kfreq));
        ConfigData configData = new ConfigData();
        configData.cmd  = kcmd;         // usa exatamente o cmd passado
        configData.freq = kfreq;        // usa a freq pedida (Hz)
        configData.S1 = kS1; configData.S2 = kS2; configData.S3 = kS3;
        configData.S4 = kS4; configData.S5 = kS5; configData.S6 = kS6;
        configData.S7 = kS7; configData.S8 = kS8; configData.S9 = kS9;
        Log.d(TAG, "ConfigData: " + configData);
        sendConfigData(configData);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void sendConfigData(@NonNull ConfigData configData) {
        if (bluetoothGatt == null || !isConnected) {
            Log.e(TAG, "sendConfigData: BluetoothGatt não está conectado. Configuração falhou.");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) { Log.e(TAG, "Serviço BLE de Configuração não encontrado."); return; }

        BluetoothGattCharacteristic configCharacteristic = service.getCharacteristic(CHARACTERISTIC_CONFIG_UUID);
        if (configCharacteristic == null) { Log.e(TAG, "Característica de Configuração não encontrada."); return; }

        byte[] payload = configData.toBytes();
        if (checkBlePermissions()) {
            configCharacteristic.setValue(payload);
            bluetoothGatt.writeCharacteristic(configCharacteristic);
            Log.d(TAG, "sendConfigData: Enviando " + payload.length + " bytes de configuração via BLE WRITE.");
        }
    }

    // ======= Auxiliares =======
    public String getSendDataAsString() {
        return "battery: " + receivedData.battery + " SR9: " + receivedData.SR9;
    }

    private void updateTimestamp() {
        Calendar calendar = Calendar.getInstance();
        receivedData.hour = calendar.get(Calendar.HOUR_OF_DAY);
        receivedData.minute = calendar.get(Calendar.MINUTE);
        receivedData.second = calendar.get(Calendar.SECOND);
        receivedData.millisecond = calendar.get(Calendar.MILLISECOND);
    }

    private void storeReadings(Context ctx) {
        Log.d(TAG, "storeReadings: saving to SharedPreferences");
        SharedPreferences sp = ctx.getSharedPreferences("My_Appinsolereadings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("S1_1", receivedData.SR1.toString());
        editor.putString("S2_1", receivedData.SR2.toString());
        editor.putString("S3_1", receivedData.SR3.toString());
        editor.putString("S4_1", receivedData.SR4.toString());
        editor.putString("S5_1", receivedData.SR5.toString());
        editor.putString("S6_1", receivedData.SR6.toString());
        editor.putString("S7_1", receivedData.SR7.toString());
        editor.putString("S8_1", receivedData.SR8.toString());
        editor.putString("S9_1", receivedData.SR9.toString());
        editor.apply();
    }

    public void produzirpico(Context context){
        Log.d(TAG, "Evento: pico de pressão (cmd=0x3D). Chamando ConectVibra.");
        SharedPreferences prefs = context.getSharedPreferences("My_Appvibra", MODE_PRIVATE);
        byte INT = Byte.parseByte(prefs.getString("int", "0"));
        byte PEST = Byte.parseByte(prefs.getString("pulse", "0"));
        short INEST = Short.parseShort(prefs.getString("interval", "0"));
        short TMEST = Short.parseShort(prefs.getString("time", "0"));
        conectar.SendConfigData((byte)0x1B, PEST, INT, TMEST, INEST);
    }

    // ======= Utils fornecida =======
    public static class Utils {
        public static void checkLoginAndSaveSendData(FirebaseHelper fh, SendData sd, Context ctx, List<String> ev) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(ctx);
                if (isNetworkAvailable) {
                    fh.saveSendData(sd, ev);
                    showToast(ctx, "SendData enviado com sucesso!");
                } else {
                    String today = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
                    fh.saveSendDataLocally(sd, today);
                    showToast(ctx, "Sem conexão. Dados salvos localmente.");
                }
            } else {
                showToast(ctx, "Você precisa fazer login antes de enviar os dados.");
            }
        }
        private static void showToast(Context ctx, String msg) {
            if (ctx instanceof AppCompatActivity) {
                ((AppCompatActivity) ctx).runOnUiThread(() ->
                        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
            } else {
                new Handler(ctx.getMainLooper()).post(() ->
                        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
            }
        }
    }
}

package com.example.myapplication2;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication2.Home.HomeActivity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

public class ConectInsole extends AppCompatActivity {
    private static final String TAG = "ConectInsoleBLE";

    // ===== UUIDs e nome do dispositivo =====
    private static final UUID SERVICE_UUID               = UUID.fromString("4FAF0101-FBCF-4309-8A1C-8472B7098485");
    private static final UUID CHARACTERISTIC_CONFIG_UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A8");
    private static final UUID CHARACTERISTIC_DATA_UUID   = UUID.fromString("AEB5483E-36E1-4688-B7F5-EA07361B26A9");
    private static final UUID CCCD_UUID                  = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String ESP32_BLE_NAME           = "USD";

    public static final String CHANNEL_ID = "notify_pressure";

    // ===== Permissões/Activity =====
    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String[]> requestPermissionLauncher;

    // ===== Fila GATT =====
    private final Queue<ConfigData> commandQueue = new LinkedList<>();
    private volatile boolean isGattOperationPending = false; // controla exclusão de operação
    private volatile boolean retryScheduled = false;          // debounce para re-tentativas
    private volatile boolean idleQueuedOnce = false;          // garante 0xFF apenas 1x por conexão

    // ===== Guards extras =====
    private volatile boolean servicesReady = false;           // processar onServicesDiscovered 1x
    private volatile boolean cccdWriteIssued = false;         // emitir write do CCCD só 1x
    private volatile boolean cccdForDataEnabled = false;      // confirma CCCD da DATA habilitado
    private volatile boolean notificationsReady = false;      // alias para pronto-notify

    // ===== BLE =====
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bluetoothScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean isScanning = false;
    private boolean isConnected = false;
    private boolean isConnecting = false;

    // ===== Protocolo/Buffer/App =====
    private final Context context;
    private final FirebaseHelper firebasehelper;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object bufferLock = new Object();
    private boolean isBufferingEnabled = false; // única flag de buffering
    private final ArrayList<String> eventlist = new ArrayList<>();
    private String currentCpf, currentMode, currentSessionId;
    private final SendData receivedData = new SendData();

    // Só para manter compatibilidade com seu código atual:
    HomeActivity home = new HomeActivity();

    // ===== Estruturas =====
    public static class ConfigData {
        public int cmd, freq;
        public int S1, S2, S3, S4, S5, S6, S7, S8, S9;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                    "cmd=%d, freq=%d, S1=%d ...", cmd, freq, S1);
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

    // ===== Construtor =====
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public ConectInsole(@NonNull Context context) {
        this.context = context;

        if (!(context instanceof AppCompatActivity)) {
            Log.e(TAG, "ConectInsole requer AppCompatActivity para permissões.");
            this.activity = null; this.bluetoothAdapter = null; this.bluetoothScanner = null; this.firebasehelper = null; this.requestPermissionLauncher = null; return;
        }
        this.activity = (AppCompatActivity) context;
        this.firebasehelper = new FirebaseHelper(context);

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth indisponível/habilitado = false.");
            bluetoothScanner = null;
        } else {
            bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                isGranted -> {
                    boolean allGranted = isGranted.values().stream().allMatch(g -> g);
                    if (allGranted) { onBlePermissionsGranted(); } else { onBlePermissionsDenied(); }
                }
        );

        requestBlePermissions();
    }

    // ===== Permissões =====
    private String[] getPermissionsToRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT };
        } else {
            return new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
        }
    }

    private boolean checkBlePermissions() {
        String[] permissions = getPermissionsToRequest();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void requestBlePermissions() {
        if (activity == null) return;
        if (checkBlePermissions()) { onBlePermissionsGranted(); } else {
            Log.d(TAG, "Solicitando permissões BLE ao usuário.");
            requestPermissionLauncher.launch(getPermissionsToRequest());
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void onBlePermissionsGranted() {
        Log.i(TAG, "Permissões BLE concedidas. Iniciando o scanner.");
        if (bluetoothScanner != null) { startScanning(); } else {
            Log.e(TAG, "Bluetooth indisponível após permissão.");
        }
    }

    private void onBlePermissionsDenied() {
        showToast(context, "Permissões BLE negadas. A busca será bloqueada.");
        Log.e(TAG, "Permissões BLE negadas.");
    }

    // ===== Buffer/Sessão =====
    public void enableBuffering(boolean enable) {
        synchronized (bufferLock) {
            isBufferingEnabled = enable;
        }
        Log.d(TAG, "Buffering " + (enable ? "habilitado" : "desabilitado"));
    }

    public boolean isBufferingEnabled() {
        synchronized (bufferLock) { return isBufferingEnabled; }
    }

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

        updateTimestamp();
        FirebaseHelper.saveSendDataForPatient(firebasehelper, snapshot, context, evSnapshot, currentCpf, currentMode, currentSessionId);
        Log.d(TAG, "flushToCloudNow: buffer enviado e zerado.");
    }

    public void setSessionMeta(String cpf, String mode, String sessionId) {
        this.currentCpf = cpf;
        this.currentMode = mode;
        this.currentSessionId = sessionId;
        Log.d(TAG, "setSessionMeta: cpf=" + cpf + ", mode=" + mode + ", sessionId=" + sessionId);
    }

    private void shrinkToLastSampleLocked() { /* opcional */ }
    private void keepOnlyLast(ArrayList<Integer> list) { /* opcional */ }

    // ===== Scan/Connect =====
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScanning() {
        if (bluetoothScanner == null || isScanning || isConnecting || !checkBlePermissions()) {
            handler.postDelayed(this::startScanning, 5000);
            return;
        }
        isScanning = true;
        Log.d(TAG, "Iniciando scan pelo dispositivo: " + ESP32_BLE_NAME);
        bluetoothScanner.startScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && ESP32_BLE_NAME.equals(device.getName()) && !isConnecting) {
                Log.d(TAG, "Dispositivo encontrado: " + ESP32_BLE_NAME + " -> conectando...");
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
            // autoConnect = false para evitar eventos duplicados e descobertas repetidas
            bluetoothGatt = device.connectGatt(context, /*autoConnect=*/false, gattCallback);
            Log.d(TAG, "Tentando conectar ao GATT: " + device.getAddress());
        }
    }

    // ===== GATT Callbacks =====
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                isConnecting = false;
                synchronized (commandQueue) {
                    commandQueue.clear();
                    isGattOperationPending = false;
                }
                // resets por conexão
                idleQueuedOnce = false;
                notificationsReady = false;
                servicesReady = false;
                cccdWriteIssued = false;
                cccdForDataEnabled = false;
                retryScheduled = false;

                Log.d(TAG, "Conectado ao GATT. Descobrindo serviços...");
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                isConnecting = false;
                idleQueuedOnce = false;
                notificationsReady = false;
                servicesReady = false;
                cccdWriteIssued = false;
                cccdForDataEnabled = false;
                retryScheduled = false;

                Log.d(TAG, "Desconectado do GATT. Reiniciando scan...");
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
                if (servicesReady) {
                    Log.d(TAG, "onServicesDiscovered ignorado (já processado).");
                    return;
                }
                servicesReady = true;

                Log.d(TAG, "Serviços descobertos (1x). Habilitando notificações.");
                setCharacteristicNotification(gatt, CHARACTERISTIC_DATA_UUID, true);

                // NÃO enfileira 0xFF aqui

            } else {
                Log.w(TAG, "Falha na descoberta de serviços: " + status);
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // Libera a fila após a escrita
            isGattOperationPending = false;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Comando anterior concluído com sucesso. Liberando fila...");
            } else {
                Log.e(TAG, "Falha na escrita da característica. Status: " + status);
            }
            attemptToSendNextCommand();
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            isGattOperationPending = false;

            final UUID descUuid = descriptor.getUuid();
            final UUID chUuid = descriptor.getCharacteristic() != null
                    ? descriptor.getCharacteristic().getUuid()
                    : new UUID(0,0);

            if (CCCD_UUID.equals(descUuid) && CHARACTERISTIC_DATA_UUID.equals(chUuid)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    notificationsReady = true;
                    cccdForDataEnabled = true;
                    Log.d(TAG, "Notificação de Dados (CCCD) ativada com sucesso (DATA characteristic).");

                    // Enfileira 0xFF somente 1x por conexão e apenas após confirmar que é o CCCD da DATA
                    if (!idleQueuedOnce) {
                        idleQueuedOnce = true;

                        ConfigData idleCmd = new ConfigData();
                        idleCmd.cmd = 0xFF; idleCmd.freq = 1;
                        idleCmd.S1 = idleCmd.S2 = idleCmd.S3 = idleCmd.S4 = idleCmd.S5 =
                                idleCmd.S6 = idleCmd.S7 = idleCmd.S8 = idleCmd.S9 = 0x0FFF;

                        synchronized (commandQueue) {
                            boolean hasFF = false;
                            for (ConfigData c : commandQueue) {
                                if (c != null && (c.cmd & 0xFF) == 0xFF) { hasFF = true; break; }
                            }
                            if (!hasFF) {
                                Log.d(TAG, "Enfileirando 0xFF (único) após CCCD DATA.");
                                commandQueue.offer(idleCmd);
                            } else {
                                Log.d(TAG, "0xFF já presente na fila. Não enfileira novamente.");
                            }
                        }
                        attemptToSendNextCommand();
                    } else {
                        Log.d(TAG, "idleQueuedOnce já true. Não enfileira 0xFF novamente.");
                    }
                } else {
                    Log.e(TAG, "Falha ao ativar notificação de Dados (DATA CCCD): " + status);
                }
            } else {
                // Ignora writes de outros descritores/CCCDs
                Log.d(TAG, "onDescriptorWrite ignorado (não é CCCD da DATA).");
            }

            // Libera próxima operação, se houver
            attemptToSendNextCommand();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (CHARACTERISTIC_DATA_UUID.equals(characteristic.getUuid())) {
                processReceivedData(characteristic.getValue());
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
                if (cccdWriteIssued) {
                    Log.d(TAG, "CCCD write já emitido. Ignorando repetição.");
                    return;
                }
                cccdWriteIssued = true; // marca antes de escrever para evitar duplo disparo

                byte[] value = enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                descriptor.setValue(value);

                isGattOperationPending = true;
                gatt.writeDescriptor(descriptor);
                Log.d(TAG, "Notificação: Escrita de CCCD enviada para ativação (1x).");
            } else {
                Log.e(TAG, "Descriptor CCCD não encontrado.");
            }
        }
    }

    // ===== Fila de comandos =====
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void enqueueCommand(@NonNull ConfigData command) {
        synchronized (commandQueue) {
            if ((command.cmd & 0xFF) == 0xFF) {
                if (idleQueuedOnce) {
                    for (ConfigData c : commandQueue) {
                        if (c != null && (c.cmd & 0xFF) == 0xFF) {
                            Log.d(TAG, "Ignorando 0xFF duplicado (já enfileirado).");
                            return;
                        }
                    }
                }
            }
            commandQueue.offer(command);
            Log.d(TAG, "Comando 0x" + String.format("%02X", command.cmd) + " enfileirado. Tamanho da fila: " + commandQueue.size());
        }
        attemptToSendNextCommand();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void attemptToSendNextCommand() {
        if (!isConnected || bluetoothGatt == null) {
            Log.w(TAG, "GATT não está conectado. Comando não enviado.");
            return;
        }
        if (isGattOperationPending) {
            return;
        }

        ConfigData nextCommand;
        synchronized (commandQueue) {
            nextCommand = commandQueue.peek();
            if (nextCommand == null) return;
        }

        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) { Log.e(TAG, "Serviço BLE de Configuração não encontrado."); return; }

        BluetoothGattCharacteristic configCharacteristic = service.getCharacteristic(CHARACTERISTIC_CONFIG_UUID);
        if (configCharacteristic == null) { Log.e(TAG, "Característica de Configuração não encontrada."); return; }

        // Seleciona writeType automaticamente
        final int props = configCharacteristic.getProperties();
        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            configCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else {
            configCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }

        byte[] payload = nextCommand.toBytes();
        if (checkBlePermissions()) {
            configCharacteristic.setValue(payload);

            isGattOperationPending = true;
            boolean ok = bluetoothGatt.writeCharacteristic(configCharacteristic);

            if (ok) {
                synchronized (commandQueue) { commandQueue.poll(); }
                Log.i(TAG, "WRITE COMANDO ENVIADO: 0x" + String.format("%02X", nextCommand.cmd) + ". Esperando callback.");
            } else {
                // Não remove da fila e faz backoff com debounce
                isGattOperationPending = false;
                if (!retryScheduled) {
                    retryScheduled = true;
                    handler.postDelayed(() -> {
                        retryScheduled = false;
                        attemptToSendNextCommand();
                    }, 150);
                }
                Log.e(TAG, "WRITE REJEITADO (write=false) para 0x" + String.format("%02X", nextCommand.cmd) + ". Nova tentativa em 150ms.");
            }
        }
    }

    // ===== API de alto nível para criar/enfileirar config =====
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void createAndSendConfigData(byte kcmd, byte kfreq,
                                        short kS1, short kS2, short kS3,
                                        short kS4, short kS5, short kS6,
                                        short kS7, short kS8, short kS9) {
        Log.d(TAG, String.format(Locale.getDefault(),
                "createAndSendConfigData: cmd=0x%02X, freq=%d", kcmd, kfreq));
        ConfigData configData = new ConfigData();
        configData.cmd  = kcmd; configData.freq = kfreq;
        configData.S1 = kS1; configData.S2 = kS2; configData.S3 = kS3;
        configData.S4 = kS4; configData.S5 = kS5; configData.S6 = kS6;
        configData.S7 = kS7; configData.S8 = kS8; configData.S9 = kS9;

        enqueueCommand(configData);
    }

    // ===== Recepção/Parse =====
    private void processReceivedData(byte[] data) {
        if (data == null || data.length != 20) {
            Log.e(TAG, "Pacote completo com tamanho incorreto. Esperado 20, recebido " + (data == null ? 0 : data.length));
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int cmd = buffer.get() & 0xFF;
        int bat = buffer.get() & 0xFF;

        synchronized (bufferLock) {
            receivedData.cmd = cmd;
            receivedData.battery = bat;
        }
        Log.d(TAG, String.format(Locale.getDefault(),
                "Pacote Completo: Cmd=0x%02X, Bat=%d, Leituras=1 (Assumido)", cmd, bat));

        ByteBuffer dataBuffer = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.put(data, 2, 18);
        dataBuffer.rewind();

        int[] lastRead = parseBlocksIntoLastSample(dataBuffer, 1);

        synchronized (bufferLock) {
            if (isBufferingEnabled) {
                dataBuffer.rewind();
                appendAllBlocksLocked(dataBuffer, 1);
                storeReadings(context);
            } else {
                receivedData.SR1.clear();
                receivedData.SR2.clear();
                receivedData.SR3.clear();
                receivedData.SR4.clear();
                receivedData.SR5.clear();
                receivedData.SR6.clear();
                receivedData.SR7.clear();
                receivedData.SR8.clear();
                receivedData.SR9.clear();
                receivedData.SR1.add(lastRead[0]);
                receivedData.SR2.add(lastRead[1]);
                receivedData.SR3.add(lastRead[2]);
                receivedData.SR4.add(lastRead[3]);
                receivedData.SR5.add(lastRead[4]);
                receivedData.SR6.add(lastRead[5]);
                receivedData.SR7.add(lastRead[6]);
                receivedData.SR8.add(lastRead[7]);
                receivedData.SR9.add(lastRead[8]);
                storeReadings(context);
            }
        }

        updateTimestamp();

        if (receivedData.cmd == 0x3D) { produzirpico(context); }
    }

    private int[] parseBlocksIntoLastSample(ByteBuffer buffer, int numReads) {
        int[] last = new int[9];
        for (int i = 0; i < numReads; i++) {
            last[0] = buffer.getShort() & 0xFFFF; last[1] = buffer.getShort() & 0xFFFF;
            last[2] = buffer.getShort() & 0xFFFF; last[3] = buffer.getShort() & 0xFFFF;
            last[4] = buffer.getShort() & 0xFFFF; last[5] = buffer.getShort() & 0xFFFF;
            last[6] = buffer.getShort() & 0xFFFF; last[7] = buffer.getShort() & 0xFFFF;
            last[8] = buffer.getShort() & 0xFFFF;
        }
        return last;
    }

    private void appendAllBlocksLocked(ByteBuffer buffer, int numReads) {
        for (int i = 0; i < numReads; i++) {
            receivedData.SR1.add(buffer.getShort() & 0xFFFF); receivedData.SR2.add(buffer.getShort() & 0xFFFF);
            receivedData.SR3.add(buffer.getShort() & 0xFFFF); receivedData.SR4.add(buffer.getShort() & 0xFFFF);
            receivedData.SR5.add(buffer.getShort() & 0xFFFF); receivedData.SR6.add(buffer.getShort() & 0xFFFF);
            receivedData.SR7.add(buffer.getShort() & 0xFFFF); receivedData.SR8.add(buffer.getShort() & 0xFFFF);
            receivedData.SR9.add(buffer.getShort() & 0xFFFF);
        }
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

        System.out.println(receivedData.SR1.toString());
        System.out.println(receivedData.SR2.toString());
        System.out.println(receivedData.SR3.toString());
        System.out.println(receivedData.SR4.toString());
        System.out.println(receivedData.SR5.toString());
        System.out.println(receivedData.SR6.toString());
        System.out.println(receivedData.SR7.toString());
        System.out.println(receivedData.SR8.toString());
        System.out.println(receivedData.SR9.toString());

        // Se seu HomeActivity depende do contexto de Activity real, cuidado ao instanciar diretamente.
        try { home.loadColorsR(); } catch (Exception ignore) {}
    }

    public void produzirpico(Context context) {
        Log.d(TAG, "Evento: pico de pressão (cmd=0x3D). Chamando ConectVibra.");
        SharedPreferences prefs = context.getSharedPreferences("My_Appvibra", MODE_PRIVATE);
        byte INT   = Byte.parseByte(prefs.getString("int", "0"));
        byte PEST  = Byte.parseByte(prefs.getString("pulse", "0"));
        short INEST= Short.parseShort(prefs.getString("interval", "0"));
        short TMEST= Short.parseShort(prefs.getString("time", "0"));
        // ConectVibra conectar = new ConectVibra(context);
        // conectar.SendConfigData((byte)0x1B, PEST, INT, TMEST, INEST);
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

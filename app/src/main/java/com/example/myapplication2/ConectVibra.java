package com.example.myapplication2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class ConectVibra {
    private static final String TAG = "ConectVibra";
    private static final String PREF_KEY_ST = "pref_stNum";

    private static final int UDP_PORT = 10000;
    private static final String BROADCAST_IP = "255.255.255.255";
    private static final int APPID_DISC = 0x2001;
    private static final int APPID_CONFIG = 0x1001;
    private static final int APPID_STATUS = 0x1002;

    private static final long T1 = 190;   // ms
    private static final long BACKOFF_MAX = 1000;  // ms

    private final SharedPreferences prefsBat, prefsConn;
    private DatagramSocket socket;
    private InetAddress espAddress;
    private final HandlerThread gooseThread = new HandlerThread("GooseThread");
    private final Handler heartbeatHandler;
    private Thread receiveThread;
    private volatile boolean connectedVibra = false;
    private volatile long lastStatusTime = 0;
    private final Object communicationLock = new Object();
    private volatile boolean isRunning = false;
    private int stNum = 0;
    private final int confRev = 1;

    public ConectVibra(Context ctx) {
        prefsBat = ctx.getSharedPreferences("Battery_info", Context.MODE_PRIVATE);
        prefsConn = ctx.getSharedPreferences("My_Appinsolesamount", Context.MODE_PRIVATE);

        // carrega stNum
        stNum = prefsConn.getInt(PREF_KEY_ST, 0);

        // configura socket UDP
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(UDP_PORT));
            socket.setBroadcast(true);
            Log.i(TAG, "[Init] UDP pronto na porta " + UDP_PORT);
        } catch (SocketException ex) {
            Log.e(TAG, "[Init] bind socket falhou", ex);
        }

        // restaura IP se já descoberto
        String ip = prefsConn.getString("espIP", null);
        if (ip != null) {
            try {
                espAddress = InetAddress.getByName(ip);
            } catch (UnknownHostException ignored) {
            }
        }

        // inicia recepção UDP
        receiveData();

        // inicia HandlerThread para heartbeat
        gooseThread.start();
        heartbeatHandler = new Handler(gooseThread.getLooper());
    }

    /**
     * Cancela tudo que estava pendente e inicia um novo ciclo:
     * 1) ++stNum
     * 2) armazena parâmetros
     * 3) limpa Timer e Handler
     * 4) agenda Immediate + Backoff + Heartbeat
     */


    private static class CurrentParams {
        final int stNum;
        final byte cmd;
        final byte PEST;
        final byte INT;
        final short TMEST;
        final short INEST;

        CurrentParams(int stNum, byte cmd, byte PEST, byte INT, short TMEST, short INEST) {
            this.stNum = stNum;
            this.cmd = cmd;
            this.PEST = PEST;
            this.INT = INT;
            this.TMEST = TMEST;
            this.INEST = INEST;
        }
    }

    private CurrentParams currentParams = null;

    public void SendConfigData(byte cmd, byte PEST, byte INT, short TMEST, short INEST) {
        synchronized (communicationLock) {
            // 1. Incrementa e armazena stNum
            stNum = (stNum + 1) % 11;
            prefsConn.edit().putInt(PREF_KEY_ST, stNum).apply();

            // 2. Para qualquer comunicação em andamento
            stopCommunicationInternal();

            // 3. Armazena os novos parâmetros
            currentParams = new CurrentParams(stNum, cmd, PEST, INT, TMEST, INEST);
            byte lastCmd = cmd;
            byte lastPEST = PEST;
            byte lastINT = INT;
            short lastTMEST = TMEST;
            short lastINEST = INEST;

            // 4. Inicia novo ciclo
            startCommunication();
        }
    }

    private void startCommunication() {
        synchronized (communicationLock) {
            if (currentParams == null || isRunning) {
                return;
            }

            isRunning = true;

            // 1. Envio imediato
            sendWithLog("[ImmediateSend]", currentParams.stNum, currentParams.cmd,
                    currentParams.PEST, currentParams.INT,
                    currentParams.TMEST, currentParams.INEST);

            // 2. Inicia backoff
            //startBackoff();

            // 3. Inicia heartbeat
            startHeartbeat();
        }
    }

    private void startBackoff() {
        new Thread(() -> {
            long interval = T1;
            while (isRunning && interval <= BACKOFF_MAX) {
                try {
                    Thread.sleep(interval);
                    if (!isRunning) break;

                    synchronized (communicationLock) {
                        if (currentParams != null) {
                            sendWithLog("[Backoff]", currentParams.stNum, currentParams.cmd,
                                    currentParams.PEST, currentParams.INT,
                                    currentParams.TMEST, currentParams.INEST);
                        }
                    }

                    interval = Math.min(interval * 2, BACKOFF_MAX);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private void startHeartbeat() {
        heartbeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (communicationLock) {
                    if (!isRunning || currentParams == null) {
                        return;
                    }

                    sendWithLog("[Heartbeat]", currentParams.stNum, currentParams.cmd,
                            currentParams.PEST, currentParams.INT,
                            currentParams.TMEST, currentParams.INEST);
                }
            }
        }, BACKOFF_MAX);
    }

    private void stopCommunicationInternal() {
        synchronized (communicationLock) {
            isRunning = false;
            heartbeatHandler.removeCallbacksAndMessages(null);
        }
    }

    public void stopCommunication() {
        synchronized (communicationLock) {
            stopCommunicationInternal();
            if (receiveThread != null) {
                receiveThread.interrupt();
            }
            if (socket != null) {
                socket.close();
            }
            gooseThread.quitSafely();
        }
        Log.i(TAG, "[Stop] Comunicação encerrada");
    }


    private void sendWithLog(String prefix,
                             int sSt,
                             byte sCmd,
                             byte sPEST,
                             byte sINT,
                             short sTM,
                             short sIN) {
        // Usar o Handler do HandlerThread para enviar pacotes na thread de fundo
        heartbeatHandler.post(() -> {
            final String dest = (espAddress != null)
                    ? espAddress.getHostAddress()
                    : BROADCAST_IP;
            Log.i(TAG, prefix + "[Envio CONFIG] " +
                    "stNum=" + sSt + " → " + dest +
                    " cmd=0x" + String.format("%02X", sCmd) +
                    " PEST=" + sPEST +
                    " INT=" + sINT +
                    " TMEST=" + sTM +
                    " INEST=" + sIN);

            byte[] pkt = new byte[15];
            pkt[0] = (byte) ((APPID_CONFIG >> 8) & 0xFF);
            pkt[1] = (byte) (APPID_CONFIG & 0xFF);
            pkt[2] = (byte) ((confRev >> 8) & 0xFF);
            pkt[3] = (byte) (confRev & 0xFF);
            pkt[4] = (byte) ((sSt >> 24) & 0xFF);
            pkt[5] = (byte) ((sSt >> 16) & 0xFF);
            pkt[6] = (byte) ((sSt >> 8) & 0xFF);
            pkt[7] = (byte) (sSt & 0xFF);
            pkt[8] = sCmd;
            pkt[9] = sPEST;
            pkt[10] = sINT;
            pkt[11] = (byte) ((sTM >> 8) & 0xFF);
            pkt[12] = (byte) (sTM & 0xFF);
            pkt[13] = (byte) ((sIN >> 8) & 0xFF);
            pkt[14] = (byte) (sIN & 0xFF);

            try {
                InetAddress d = (espAddress != null)
                        ? espAddress
                        : InetAddress.getByName(BROADCAST_IP);
                socket.send(new DatagramPacket(pkt, pkt.length, d, UDP_PORT));
            } catch (IOException e) {
                Log.e(TAG, "[Erro CONFIG] envio falhou", e);
            }
        });
    }

    // Recepção de Discovery e Status
    public void receiveData() {
        if (receiveThread != null && receiveThread.isAlive()) return;
        receiveThread = new Thread(() -> {
            byte[] buf = new byte[15];
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    socket.receive(dp);
                    int appId = ((buf[0] & 0xFF) << 8) | (buf[1] & 0xFF);

                    if (appId == APPID_DISC && dp.getLength() >= 6) {
                        String ip = String.format("%d.%d.%d.%d",
                                buf[2] & 0xFF, buf[3] & 0xFF,
                                buf[4] & 0xFF, buf[5] & 0xFF);
                        espAddress = InetAddress.getByName(ip);
                        prefsConn.edit().putString("espIP", ip).apply();
                        Log.i(TAG, "[Discovery] ESP IP=" + ip);

                    } else if (appId == APPID_STATUS && dp.getLength() >= 10) {
                        byte cmd = buf[8];
                        int bat = buf[9] & 0xFF;
                        if (!connectedVibra) {
                            connectedVibra = true;
                            Log.i(TAG, "[Status] ESP online");
                        }
                        lastStatusTime = System.currentTimeMillis();
                        prefsBat.edit().putInt("batVibra", bat).apply();
                        Log.i(TAG, String.format(
                                "[Status] stNum=%d cmd=0x%02X bat=%d%%",
                                buf[7] & 0xFF, cmd, bat
                        ));
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "[Erro UDP recv] " + ex.getMessage());
                    break;
                }
            }
        });
        receiveThread.start();
    }

}
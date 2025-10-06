package com.example.myapplication2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.Service;
import android.os.IBinder;

public class AppForegroundService extends Service {

    private static final String CHANNEL_ID = "AppForegroundServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();

        // Criando o canal de notificação (necessário no Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Criando a notificação
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("App em execução")
                .setContentText("O app está funcionando em segundo plano")
                .setSmallIcon(R.drawable.logoapppp)  // ícone da notificação
                .build();

        // Iniciando o serviço em primeiro plano
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // **Não precisamos de lógica específica aqui**. Apenas queremos manter o serviço rodando.

        // Retorna START_STICKY, garantindo que o serviço será reiniciado caso o sistema o mate
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // Esse serviço não é vinculado, então retornamos null
    }
}

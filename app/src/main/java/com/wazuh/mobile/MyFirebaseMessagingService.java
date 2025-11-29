package com.wazuh.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseService";
    private static final String CHANNEL_ID = "wazuh_critical_alerts";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Cek jika pesan mengandung payload notifikasi dari server/worker
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification Received: " + title);

            sendNotification(title, body);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);
        // Token ini nanti akan diambil oleh MainActivity saat login
    }

    private void sendNotification(String title, String messageBody) {
        // 1. Siapkan Intent (Tujuan saat diklik)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // --- BAGIAN PENTING: Titip Pesan untuk Buka Tab Alerts ---
        intent.putExtra("TARGET_FRAGMENT", "ALERTS");
        // ---------------------------------------------------------

        // 2. Bungkus dalam PendingIntent
        // FLAG_IMMUTABLE wajib untuk Android 12+
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // 3. Setup Suara & Channel
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Buat Channel untuk Android Oreo ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Critical Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifikasi untuk alert Wazuh level tinggi");
            notificationManager.createNotificationChannel(channel);
        }

        // 4. Rakit Notifikasi
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notifications) // Pastikan icon ini ada di drawable
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true) // Hilang saat diklik
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent) // <--- Pasang "Pemicu" disini
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        // 5. Tembak Notifikasi
        notificationManager.notify(0, notificationBuilder.build());
    }
}
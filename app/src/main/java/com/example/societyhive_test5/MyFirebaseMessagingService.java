package com.example.societyhive_test5;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Handles FCM token refresh and foreground message display.
 *
 * Token lifecycle:
 *   - onNewToken() fires when the token is first generated or rotated.
 *     We save it to users/{uid}.fcmToken so Cloud Functions can address this device.
 *   - MainActivity also fetches and saves the token on every launch to cover
 *     the case where the user logs in after the token was generated.
 *
 * Notifications:
 *   - Background: FCM delivers the notification automatically (no code needed).
 *   - Foreground: onMessageReceived() fires; we build and post the notification ourselves.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    static final String CHANNEL_ID   = "societyhive_pins";
    static final String CHANNEL_NAME = "Pins";

    // -------------------------------------------------------------------------
    // Token
    // -------------------------------------------------------------------------

    @Override
    public void onNewToken(String token) {
        saveTokenToFirestore(token);
    }

    /** Saves the FCM token to the current user's Firestore document. */
    static void saveTokenToFirestore(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || token == null) return;
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("fcmToken", token)
                .addOnFailureListener(e -> {
                    // If the field doesn't exist yet (new user doc), use set with merge
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("fcmToken", token);
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .set(data, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    // -------------------------------------------------------------------------
    // Foreground message display
    // -------------------------------------------------------------------------

    @Override
    public void onMessageReceived(RemoteMessage message) {
        RemoteMessage.Notification notification = message.getNotification();
        if (notification == null) return;

        String title = notification.getTitle() != null ? notification.getTitle() : "SocietyHive";
        String body  = notification.getBody()  != null ? notification.getBody()  : "";

        ensureChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_clip)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void ensureChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Pinned messages from your societies");
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}

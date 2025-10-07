package com.devszatops.goodhabitapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int habitId = intent.getIntExtra("habitId", -1);
        String habitName = intent.getStringExtra("habitName");

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Tworzymy kanał dla Androida 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "habit_channel", "Habit Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Intent otwierający HabitDetailActivity
        Intent detailIntent = new Intent(context, HabitDetailActivity.class);
        detailIntent.putExtra("habitId", habitId);
        detailIntent.putExtra("habitName", habitName);
        detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                habitId, // unikalny requestCode dla każdego zwyczaju
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "habit_channel")
                .setSmallIcon(R.drawable.ic_notification) // ikona w drawable
                .setContentTitle("Przypomnienie o zwyczaju")
                .setContentText("Pamiętaj, żeby zrobić dzisiaj: " + habitName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent); // ustawiamy PendingIntent

        notificationManager.notify(habitId, builder.build());
    }
}


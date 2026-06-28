package com.claudeusage.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.claudeusage.app.MainActivity
import com.claudeusage.app.R
import com.claudeusage.app.data.model.UsageMeter

/** Creates notification channels and posts limit-reset alerts. */
object Notifier {
    const val CHANNEL_RESETS = "limit_resets"
    const val CHANNEL_STATUS = "background_refresh"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val resets = NotificationChannel(
            CHANNEL_RESETS,
            context.getString(R.string.channel_resets_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_resets_desc) }

        val status = NotificationChannel(
            CHANNEL_STATUS,
            context.getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_MIN,
        ).apply { description = context.getString(R.string.channel_status_desc) }

        manager.createNotificationChannel(resets)
        manager.createNotificationChannel(status)
    }

    fun notifyReset(context: Context, meter: UsageMeter) {
        if (!hasPermission(context)) return
        val contentIntent = PendingIntent.getActivity(
            context,
            meter.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_RESETS)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setColor(ContextCompat.getColor(context, R.color.brand_coral))
            .setContentTitle("${meter.label} reset")
            .setContentText("Your ${meter.label} limit just refreshed — you're back to 0%.")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(meter.id.hashCode(), notification)
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

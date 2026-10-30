package com.mizani.money.reminder

import android.Manifest
import android.app.Notification
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
import com.mizani.money.MainActivity
import com.mizani.money.R
import com.mizani.money.domain.notifications.HabitNotificationKind

object DailyReminderNotifications {
    const val CHANNEL_ID = "daily_transaction_reminders"
    private const val NOTIFICATION_ID = 7401
    private const val TEST_NOTIFICATION_ID = 7402
    private const val HABIT_NOTIFICATION_ID = 7403
    private const val PENDING_INTENT_REQUEST = 7401
    private const val TEST_PENDING_INTENT_REQUEST = 7402
    private const val HABIT_PENDING_INTENT_REQUEST = 7403

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_reminders_description)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java)
                ?.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    fun post(context: Context): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mizani)
            .setContentTitle(context.getString(R.string.notification_daily_reminder_title))
            .setContentText(context.getString(R.string.notification_daily_reminder_body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /** Sends a user-requested delivery check without affecting the daily reminder state. */
    fun postTest(context: Context): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            TEST_PENDING_INTENT_REQUEST,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mizani)
            .setContentTitle(context.getString(R.string.notification_test_reminder_title))
            .setContentText(context.getString(R.string.notification_test_reminder_body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /** Posts generic copy only; the selected category is intentionally not put into the intent. */
    @Suppress("UNUSED_PARAMETER")
    fun postHabit(context: Context, kind: HabitNotificationKind): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            HABIT_PENDING_INTENT_REQUEST,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mizani)
            // Never expose which financial category triggered the local decision. The category
            // can be explained only after the user opens the app and passes app lock if enabled.
            .setContentTitle(context.getString(R.string.notification_habit_title))
            .setContentText(context.getString(R.string.notification_habit_body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(HABIT_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /** Generic notification copy keeps commitment names and amounts off the lock screen. */
    fun postCommitment(context: Context, commitmentId: String): Boolean {
        ensureChannel(context)
        if (!canPost(context)) return false
        val notificationId = 80_000 + (commitmentId.hashCode() and 0x3fff)
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mizani)
            .setContentTitle(context.getString(R.string.notification_commitment_reminder_title))
            .setContentText(context.getString(R.string.notification_commitment_reminder_body))
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(TEST_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(HABIT_NOTIFICATION_ID)
    }

    /** Clears every Mizani notification during local-data replacement or deletion. */
    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}

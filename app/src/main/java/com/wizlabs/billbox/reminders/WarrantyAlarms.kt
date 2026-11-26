package com.wizlabs.billbox.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wizlabs.billbox.MainActivity
import com.wizlabs.billbox.R
import com.wizlabs.billbox.data.Store
import com.wizlabs.billbox.model.AppData
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Arms the daily 10:00 check (inexact, Doze-friendly: no exact-alarm permission) and posts the notifications. */
object WarrantyAlarms {
    const val ACTION_CHECK = "com.wizlabs.billbox.WARRANTY_CHECK"
    const val EXTRA_BILL = "open_bill"
    private const val CHANNEL = "warranty_reminders"
    private const val PREFS = "reminders"
    private const val KEY_DONE = "notified"

    fun load(context: Context): AppData = Store(File(context.filesDir, "bills.json")).load()

    private fun checkIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Schedules the next check, or cancels it when no reminder is on or no bill has a warranty. */
    fun schedule(context: Context, data: AppData = load(context)) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = checkIntent(context)
        val needed = WarrantyScheduler.enabledOffsets(data).isNotEmpty() && data.bills.any { it.warrantyUntil != null }
        if (!needed) { am.cancel(pi); return }
        val at = WarrantyScheduler.nextCheckMillis(System.currentTimeMillis(), ZoneId.systemDefault())
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Runs the daily check: reads the local store only (no network), posts what is due, re-arms the alarm. */
    fun check(context: Context, today: LocalDate = LocalDate.now()) {
        val data = load(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notified = WarrantyScheduler.prune(prefs.getStringSet(KEY_DONE, emptySet()).orEmpty(), data.bills)
        val result = WarrantyScheduler.dueToday(data.bills, WarrantyScheduler.enabledOffsets(data), today, notified)
        if (result.due.isNotEmpty() && canNotify(context)) {
            ensureChannel(context)
            val nm = NotificationManagerCompat.from(context)
            result.due.forEach { d ->
                val b = data.bills.find { it.id == d.billId } ?: return@forEach
                val title = when (d.daysLeft) {
                    0 -> context.getString(R.string.notif_title_today)
                    1 -> context.getString(R.string.notif_title_tomorrow)
                    else -> context.getString(R.string.notif_title_days, d.daysLeft)
                }
                val open = PendingIntent.getActivity(
                    context, b.id.hashCode(),
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(EXTRA_BILL, b.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val n = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat_receipt)
                    .setContentTitle(title)
                    .setContentText("${b.product} · ${b.store}")
                    .setContentIntent(open).setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setColor(0xFF0E9F6E.toInt())
                    .build()
                runCatching { nm.notify(b.id.hashCode(), n) }
            }
        }
        // Remember what was shown (or skipped because it was not due) so nothing repeats.
        if (result.mark.isNotEmpty() || notified.size != (prefs.getStringSet(KEY_DONE, emptySet())?.size ?: 0)) {
            prefs.edit().putStringSet(KEY_DONE, notified + result.mark).apply()
        }
        schedule(context, data)
    }

    fun canNotify(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = context.getString(R.string.notif_channel_desc) },
            )
        }
    }
}

class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        Thread { try { WarrantyAlarms.check(context.applicationContext) } finally { pending.finish() } }.start()
    }
}

/** Re-arms the daily check after a reboot, an app update, or a clock / time-zone change. */
class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WarrantyAlarms.schedule(context.applicationContext)
    }
}

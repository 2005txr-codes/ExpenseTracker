package com.example.expensetracker

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

/** freq: 0 = daily, 1 = weekly, 2 = monthly.
 *  day:  weekly -> Calendar.MONDAY..SUNDAY value, monthly -> day of month 1..31 */
data class Reminder(
    val id: Int, val title: String, val message: String,
    val freq: Int, val hour: Int, val minute: Int, val day: Int,
    val color: Int, val enabled: Boolean
)

object ReminderStore {
    val COLOR_NAMES = listOf("Blue", "Green", "Orange", "Red", "Purple", "Gold", "Pink")
    val COLORS = listOf(
        0xFF1976D2.toInt(), 0xFF388E3C.toInt(), 0xFFF57C00.toInt(), 0xFFD32F2F.toInt(),
        0xFF7B1FA2.toInt(), 0xFFC9A24A.toInt(), 0xFFD9769B.toInt()
    )
    val FREQ_NAMES = listOf("Daily", "Weekly", "Monthly")
    val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val WEEK_VALUES = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    private fun p(c: Context) = c.getSharedPreferences("reminders_v2", Context.MODE_PRIVATE)

    fun all(c: Context): List<Reminder> {
        val out = mutableListOf<Reminder>()
        try {
            val arr = JSONArray(p(c).getString("list", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Reminder(
                        o.getInt("id"), o.getString("title"), o.getString("message"),
                        o.getInt("freq"), o.getInt("hour"), o.getInt("minute"), o.getInt("day"),
                        o.getInt("color"), o.getBoolean("enabled")
                    )
                )
            }
        } catch (e: Exception) { }
        return out
    }

    fun get(c: Context, id: Int): Reminder? = all(c).firstOrNull { it.id == id }

    fun nextId(c: Context): Int = (all(c).maxOfOrNull { it.id } ?: 0) + 1

    fun save(c: Context, r: Reminder) {
        val list = all(c).filter { it.id != r.id }.toMutableList()
        list.add(r)
        write(c, list.sortedBy { it.id })
    }

    fun delete(c: Context, id: Int) {
        write(c, all(c).filter { it.id != id })
    }

    private fun write(c: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(
                JSONObject().put("id", r.id).put("title", r.title).put("message", r.message)
                    .put("freq", r.freq).put("hour", r.hour).put("minute", r.minute)
                    .put("day", r.day).put("color", r.color).put("enabled", r.enabled)
            )
        }
        p(c).edit().putString("list", arr.toString()).apply()
    }

    fun describe(r: Reminder): String {
        val t = String.format(Locale.US, "%02d:%02d", r.hour, r.minute)
        return when (r.freq) {
            0 -> "Daily at $t"
            1 -> "Every ${DAY_NAMES[WEEK_VALUES.indexOf(r.day).coerceAtLeast(0)]} at $t"
            else -> "Monthly on day ${r.day} at $t"
        }
    }
}

object Notifier {
    const val CHANNEL = "moneywise_reminders_v2"

    /** High importance = heads-up banner + sound. Safe to call many times. */
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel("reminder")   // old low-importance channel
            if (nm.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_HIGH)
                ch.description = "MoneyWise reminders"
                ch.enableVibration(true)
                ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun requestPermission(a: Activity) {
        if (Build.VERSION.SDK_INT >= 33 &&
            a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            a.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    fun show(ctx: Context, id: Int, title: String, message: String, color: Int) {
        ensureChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            ctx, id,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(Notification.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, n)
    }
}

object ReminderScheduler {
    private fun pending(ctx: Context, id: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, id, Intent(ctx, ReminderReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun cancel(ctx: Context, id: Int) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(ctx, id))
    }

    /** Sets (or resets) the alarm for the next time this reminder is due. */
    fun schedule(ctx: Context, r: Reminder, fromMillis: Long = System.currentTimeMillis()) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, r.id)
        am.cancel(pi)
        if (!r.enabled) return

        val now = Calendar.getInstance()
        now.timeInMillis = fromMillis
        val at = nextTrigger(r, now).timeInMillis
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun scheduleAll(ctx: Context) {
        ReminderStore.all(ctx).forEach { schedule(ctx, it) }
    }

    /** The next moment (after `now`) this reminder is due. */
    fun nextTrigger(r: Reminder, now: Calendar): Calendar {
        val c = now.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, r.hour)
        c.set(Calendar.MINUTE, r.minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        when (r.freq) {
            0 -> if (!c.after(now)) c.add(Calendar.DAY_OF_YEAR, 1)
            1 -> {
                c.add(Calendar.DAY_OF_YEAR, (r.day - c.get(Calendar.DAY_OF_WEEK) + 7) % 7)
                if (!c.after(now)) c.add(Calendar.DAY_OF_YEAR, 7)
            }
            else -> {
                setDay(c, r.day)
                if (!c.after(now)) {
                    c.set(Calendar.DAY_OF_MONTH, 1)
                    c.add(Calendar.MONTH, 1)
                    setDay(c, r.day)
                }
            }
        }
        return c
    }

    /** Sets the day of month, clamped to the length of the month (31st -> 30th/28th). */
    private fun setDay(c: Calendar, day: Int) {
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.DAY_OF_MONTH, minOf(day, c.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val r = ReminderStore.get(ctx, id) ?: return
        if (r.enabled) {
            Notifier.show(ctx, r.id, r.title, r.message, ReminderStore.COLORS[r.color.coerceIn(0, ReminderStore.COLORS.size - 1)])
            // queue the next occurrence (daily / weekly / monthly)
            ReminderScheduler.schedule(ctx, r, System.currentTimeMillis() + 60_000)
        }
    }
}

/** Re-creates all alarms after the phone restarts. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ReminderScheduler.scheduleAll(ctx)
    }
}
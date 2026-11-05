package com.ali.agent.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import com.ali.agent.data.db.AliDao
import com.ali.agent.data.db.MobileTaskEntity
import com.ali.agent.data.db.QuickNoteEntity
import java.util.Locale

data class MobileActionOutcome(
    val executed: Boolean,
    val actionBadge: String,
    val humanConfirmation: String
)

class MobileActionExecutor(
    private val context: Context,
    private val dao: AliDao
) {

    suspend fun detectAndExecute(userInput: String): MobileActionOutcome? {
        val text = userInput.trim()
        val lower = text.lowercase(Locale.US)

        // 1. Set Alarm: e.g. "set an alarm for 6:30 am" or "wake me up at 7:15"
        val alarmRegex = Regex("(?i)(?:set\\s+(?:an\\s+)?alarm\\s+for|wake\\s+me\\s+up\\s+at)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
        alarmRegex.find(text)?.let { m ->
            var hour = m.groupValues[1].toIntOrNull() ?: 7
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase()
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "ALI Wake-Up")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Also record in internal task list if device lacks standard clock app
            }

            val formattedTime = String.format(Locale.US, "%02d:%02d", hour, minute)
            dao.insertTask(
                MobileTaskEntity(
                    title = "Alarm set for $formattedTime",
                    dueTimeText = formattedTime,
                    priority = "HIGH"
                )
            )
            return MobileActionOutcome(
                executed = true,
                actionBadge = "Alarm • $formattedTime",
                humanConfirmation = "Done! I've set your alarm for **$formattedTime** and pinned it to your day hub so you're covered."
            )
        }

        // 2. Set Timer: e.g. "set a timer for 10 minutes"
        val timerRegex = Regex("(?i)set\\s+(?:a\\s+)?timer\\s+for\\s+(\\d+)\\s*(minute|minutes|min|mins|second|seconds|sec|secs|hour|hours)")
        timerRegex.find(text)?.let { m ->
            val amount = m.groupValues[1].toIntOrNull() ?: 5
            val unit = m.groupValues[2].lowercase()
            val seconds = when {
                unit.startsWith("hour") -> amount * 3600
                unit.startsWith("min") -> amount * 60
                else -> amount
            }
            try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, "ALI Timer")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
            return MobileActionOutcome(
                executed = true,
                actionBadge = "Timer • $amount $unit",
                humanConfirmation = "Timer started for **$amount $unit**. Stay focused—I've got the clock."
            )
        }

        // 3. Add Reminder / Task: e.g. "remind me to call dentist at 4pm" or "add task finish report"
        val reminderRegex = Regex("(?i)(?:remind\\s+me\\s+to|add\\s+(?:a\\s+)?task:?|new\\s+reminder:?|todo:?)\\s+(.+)")
        reminderRegex.find(text)?.let { m ->
            val rawTask = m.groupValues[1].trim()
            val timeMatch = Regex("(?i)\\b(?:at|by|on)\\s+([0-9:apm\\s]+|tomorrow|tonight|evening|morning)$").find(rawTask)
            val dueText = timeMatch?.groupValues?.get(1)?.trim() ?: "Today"
            val cleanTitle = if (timeMatch != null) {
                rawTask.removeSuffix(timeMatch.value).trim()
            } else {
                rawTask
            }

            dao.insertTask(
                MobileTaskEntity(
                    title = cleanTitle.replaceFirstChar { it.uppercase() },
                    dueTimeText = dueText,
                    priority = if (lower.contains("urgent") || lower.contains("important")) "HIGH" else "NORMAL"
                )
            )
            return MobileActionOutcome(
                executed = true,
                actionBadge = "Task Saved • $dueText",
                humanConfirmation = "Got your back—I've added **\"$cleanTitle\"** ($dueText) to your tasks."
            )
        }

        // 4. Save Note: e.g. "take a note: ..." or "save note ..." or "write a note that ..."
        val noteRegex = Regex("(?i)(?:take\\s+a\\s+note:?|save\\s+(?:a\\s+)?note:?|write\\s+(?:a\\s+)?note(?:\\s+that)?:?|note\\s+to\\s+self:?)\\s+(.+)")
        noteRegex.find(text)?.let { m ->
            val content = m.groupValues[1].trim()
            val titleWords = content.split(Regex("\\s+")).take(5).joinToString(" ")
            dao.insertNote(
                QuickNoteEntity(
                    title = titleWords.replaceFirstChar { it.uppercase() },
                    body = content,
                    tags = "voice-capture"
                )
            )
            return MobileActionOutcome(
                executed = true,
                actionBadge = "Note Captured",
                humanConfirmation = "Locked into your private notes: *\"$content\"*"
            )
        }

        // 5. Draft SMS / Text Message: e.g. "send a message to 98765 saying I am on my way"
        val smsRegex = Regex("(?i)(?:send\\s+(?:a\\s+)?(?:text|message|sms)\\s+to|message)\\s+([a-zA-Z0-9+\\-]+)\\s+(?:saying|that)\\s+(.+)")
        smsRegex.find(text)?.let { m ->
            val recipient = m.groupValues[1].trim()
            val body = m.groupValues[2].trim()
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$recipient")
                    putExtra("sms_body", body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
            } catch (_: Exception) {
            }
            return MobileActionOutcome(
                executed = true,
                actionBadge = "SMS Draft • $recipient",
                humanConfirmation = "I've drafted that message to **$recipient**: *\"$body\"*—ready for you to hit send."
            )
        }

        // 6. Calendar Event: e.g. "schedule a meeting about ... "
        val calRegex = Regex("(?i)(?:schedule\\s+(?:a\\s+)?(?:meeting|event)|add\\s+to\\s+calendar)\\s+(.+)")
        calRegex.find(text)?.let { m ->
            val title = m.groupValues[1].trim()
            try {
                val calIntent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(calIntent)
            } catch (_: Exception) {
            }
            dao.insertTask(
                MobileTaskEntity(
                    title = "Calendar: $title",
                    dueTimeText = "Scheduled",
                    priority = "HIGH"
                )
            )
            return MobileActionOutcome(
                executed = true,
                actionBadge = "Calendar Event",
                humanConfirmation = "I've opened your calendar event for **\"$title\"** and logged it in your schedule."
            )
        }

        // 7. Launch Installed App: e.g. "open whatsapp", "launch camera", "open settings"
        val openAppRegex = Regex("(?i)^(?:open|launch|start)\\s+([a-zA-Z0-9\\s]{2,25})$")
        openAppRegex.find(text)?.let { m ->
            val appName = m.groupValues[1].trim()
            if (appName.equals("settings", ignoreCase = true)) {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return MobileActionOutcome(
                        executed = true,
                        actionBadge = "System Settings",
                        humanConfirmation = "Opening your phone's Settings right now."
                    )
                } catch (_: Exception) {
                }
            }

            val launched = launchAppByName(appName)
            if (launched) {
                return MobileActionOutcome(
                    executed = true,
                    actionBadge = "Launched $appName",
                    humanConfirmation = "Opening **$appName** for you right now."
                )
            }
        }

        return null
    }

    private fun launchAppByName(targetName: String): Boolean {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            val match = packages.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString()
                label.contains(targetName, ignoreCase = true)
            }
            if (match != null) {
                val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    true
                } else false
            } else false
        } catch (_: Exception) {
            false
        }
    }
}

package com.example.expensetracker

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderEditActivity : BaseActivity() {
    private lateinit var etTitle: EditText
    private lateinit var etMsg: EditText
    private lateinit var spFreq: Spinner
    private lateinit var spDay: Spinner
    private lateinit var spColor: Spinner
    private lateinit var tvDayLabel: TextView
    private lateinit var btnTime: Button
    private lateinit var cbEnable: CheckBox
    private lateinit var tvPreview: TextView
    private var hour = 20
    private var minute = 0
    private var rid = -1
    private var lastFreq = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_edit)
        rid = intent.getIntExtra("id", -1)
        val existing = ReminderStore.get(this, rid)
        Nav.back(this, if (existing == null) "New reminder" else "Edit reminder")
        Notifier.requestPermission(this)

        etTitle = findViewById(R.id.etTitle)
        etMsg = findViewById(R.id.etMsg)
        spFreq = findViewById(R.id.spFreq)
        spDay = findViewById(R.id.spDay)
        spColor = findViewById(R.id.spColor)
        tvDayLabel = findViewById(R.id.tvDayLabel)
        btnTime = findViewById(R.id.btnTime)
        cbEnable = findViewById(R.id.cbEnable)
        tvPreview = findViewById(R.id.tvPreview)

        val r = existing ?: Reminder(
            -1, "MoneyWise reminder", "Don't forget to log today's expenses!",
            0, 20, 0, Calendar.MONDAY, 0, true
        )
        etTitle.setText(r.title)
        etMsg.setText(r.message)
        hour = r.hour
        minute = r.minute
        cbEnable.isChecked = r.enabled
        updateTime()

        spFreq.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ReminderStore.FREQ_NAMES)
        spColor.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ReminderStore.COLOR_NAMES)
        spFreq.setSelection(r.freq)
        spColor.setSelection(r.color.coerceIn(0, ReminderStore.COLORS.size - 1))
        lastFreq = r.freq
        fillDays(r.freq, r.day)

        spFreq.onItemSelectedListener = onPick { pos ->
            if (pos != lastFreq) {
                lastFreq = pos
                fillDays(pos, if (pos == 1) Calendar.MONDAY else 1)
            } else updatePreview()
        }
        spDay.onItemSelectedListener = onPick { updatePreview() }
        spColor.onItemSelectedListener = onPick { updatePreview() }
        etTitle.addTextChangedListener(watcher())
        etMsg.addTextChangedListener(watcher())

        btnTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> hour = h; minute = m; updateTime(); updatePreview() },
                hour, minute, DateFormat.is24HourFormat(this)).show()
        }

        findViewById<Button>(R.id.btnSaveReminder).setOnClickListener { save() }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            val t = buildReminder()
            Notifier.show(this, 9999, t.title, t.message, ReminderStore.COLORS[t.color])
            Toast.makeText(this, "Test sent - look at the top of the screen", Toast.LENGTH_SHORT).show()
        }

        val btnDelete = findViewById<Button>(R.id.btnDeleteReminder)
        if (existing != null) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                AlertDialog.Builder(this).setTitle("Delete this reminder?")
                    .setPositiveButton("Delete") { _, _ ->
                        ReminderScheduler.cancel(this, rid)
                        ReminderStore.delete(this, rid)
                        finish()
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        updatePreview()
    }

    private fun onPick(f: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(a: AdapterView<*>?, v: View?, pos: Int, id: Long) { f(pos) }
        override fun onNothingSelected(a: AdapterView<*>?) {}
    }

    private fun watcher() = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { updatePreview() }
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
    }

    /** Shows the day picker that fits the chosen repeat type. */
    private fun fillDays(freq: Int, day: Int) {
        when (freq) {
            0 -> {
                tvDayLabel.visibility = View.GONE
                spDay.visibility = View.GONE
            }
            1 -> {
                tvDayLabel.text = "Day of the week"
                tvDayLabel.visibility = View.VISIBLE
                spDay.visibility = View.VISIBLE
                spDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ReminderStore.DAY_NAMES)
                spDay.setSelection(ReminderStore.WEEK_VALUES.indexOf(day).coerceAtLeast(0))
            }
            else -> {
                tvDayLabel.text = "Day of the month"
                tvDayLabel.visibility = View.VISIBLE
                spDay.visibility = View.VISIBLE
                spDay.adapter = ArrayAdapter(
                    this, android.R.layout.simple_spinner_dropdown_item, (1..31).map { it.toString() }
                )
                spDay.setSelection((day - 1).coerceIn(0, 30))
            }
        }
        updatePreview()
    }

    private fun dayValue(freq: Int): Int = when (freq) {
        1 -> ReminderStore.WEEK_VALUES[spDay.selectedItemPosition.coerceIn(0, 6)]
        2 -> spDay.selectedItemPosition.coerceAtLeast(0) + 1
        else -> Calendar.MONDAY
    }

    private fun buildReminder(): Reminder {
        val freq = spFreq.selectedItemPosition.coerceAtLeast(0)
        return Reminder(
            if (rid > 0) rid else ReminderStore.nextId(this),
            etTitle.text.toString().trim().ifEmpty { "MoneyWise" },
            etMsg.text.toString().trim().ifEmpty { "Time to check your budget!" },
            freq, hour, minute, dayValue(freq),
            spColor.selectedItemPosition.coerceIn(0, ReminderStore.COLORS.size - 1),
            cbEnable.isChecked
        )
    }

    private fun updateTime() {
        btnTime.text = "Time: " + String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    private fun updatePreview() {
        if (!::tvPreview.isInitialized || !::spDay.isInitialized) return
        val r = buildReminder()
        tvPreview.text = "${r.title}\n${r.message}\n\n${ReminderStore.describe(r)}"
        tvPreview.setBackgroundColor(ReminderStore.COLORS[r.color])
    }

    private fun save() {
        val r = buildReminder()
        ReminderStore.save(this, r)
        ReminderScheduler.schedule(this, r)
        val msg = if (r.enabled) {
            val next = ReminderScheduler.nextTrigger(r, Calendar.getInstance())
            "Saved. Next reminder: " +
                    SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(next.time)
        } else "Saved (reminder is off)"
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        finish()
    }
}
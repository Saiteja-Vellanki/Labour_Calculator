package com.labourcalc

import android.Manifest
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var labours: MutableList<Labour>
    private lateinit var adapter: LabourAdapter
    private lateinit var chipTotal: TextView
    private lateinit var chipPaid: TextView
    private lateinit var chipDue: TextView

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        labours = LabourStore.load(this)
        chipTotal = findViewById(R.id.chipTotal)
        chipPaid = findViewById(R.id.chipPaid)
        chipDue = findViewById(R.id.chipDue)

        val rv = findViewById<RecyclerView>(R.id.recycler)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = LabourAdapter(
            labours,
            onEdit = { showDialog(it) },
            onMarkPaid = { markPaid(it) },
            onDelete = { confirmDelete(it) }
        )
        rv.adapter = adapter

        findViewById<ExtendedFloatingActionButton>(R.id.fabAdd).setOnClickListener { showDialog(null) }

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scheduleReminder()
        refresh()
    }

    private fun scheduleReminder() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun refresh() {
        adapter.notifyDataSetChanged()
        val due = labours.filter { !it.isPaid }.sumOf { it.balance }
        val paidCount = labours.count { it.isPaid }
        chipTotal.text = "📋 ${labours.size}"
        chipPaid.text = "✅ $paidCount Paid"
        chipDue.text = "⏳ Due ₹${"%.0f".format(due)}"
        LabourStore.save(this, labours)
    }

    private fun markPaid(l: Labour) {
        l.amountPaid = l.total
        refresh()
    }

    private fun confirmDelete(l: Labour) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${l.place} (${l.date})?")
            .setPositiveButton("Delete") { _, _ ->
                labours.remove(l); refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun today(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    private fun showDialog(existing: Labour?) {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_labour, null)
        val date = v.findViewById<EditText>(R.id.inDate)
        val place = v.findViewById<EditText>(R.id.inPlace)
        val workers = v.findViewById<EditText>(R.id.inWorkers)
        val cost = v.findViewById<EditText>(R.id.inCost)
        val note = v.findViewById<EditText>(R.id.inNote)
        val paid = v.findViewById<EditText>(R.id.inPaid)

        // Date is mandatory - always pre-filled, tap opens calendar picker
        date.setText(existing?.date?.ifBlank { today() } ?: today())
        date.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    date.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y))
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        existing?.let {
            place.setText(it.place)
            workers.setText(it.workers.toString())
            cost.setText(it.costPerWorker.toString())
            note.setText(it.note)
            paid.setText(it.amountPaid.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Entry" else "Edit Entry")
            .setView(v)
            .setPositiveButton("Save") { _, _ ->
                val l = existing ?: Labour().also { labours.add(it) }
                l.date = date.text.toString().trim().ifBlank { today() }
                l.place = place.text.toString().trim()
                l.workers = workers.text.toString().toIntOrNull() ?: 0
                l.costPerWorker = cost.text.toString().toDoubleOrNull() ?: 0.0
                l.note = note.text.toString().trim()
                l.amountPaid = paid.text.toString().toDoubleOrNull() ?: 0.0
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

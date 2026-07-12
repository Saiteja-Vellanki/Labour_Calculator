package com.labourcalc

import android.Manifest
import android.content.Intent
import android.net.Uri
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
            onCall = { call(it) },
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
        chipTotal.text = "👷 ${labours.size}"
        chipPaid.text = "✅ $paidCount Paid"
        chipDue.text = "⏳ Due ₹${"%.0f".format(due)}"
        LabourStore.save(this, labours)
    }

    private fun markPaid(l: Labour) {
        l.amountPaid = l.total
        refresh()
    }

    private fun call(l: Labour) {
        if (l.mobile.isNotBlank()) {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${l.mobile}")))
        }
    }

    private fun confirmDelete(l: Labour) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${l.name}?")
            .setPositiveButton("Delete") { _, _ ->
                labours.remove(l); refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDialog(existing: Labour?) {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_labour, null)
        val name = v.findViewById<EditText>(R.id.inName)
        val mobile = v.findViewById<EditText>(R.id.inMobile)
        val place = v.findViewById<EditText>(R.id.inPlace)
        val hours = v.findViewById<EditText>(R.id.inHours)
        val rate = v.findViewById<EditText>(R.id.inRate)
        val paid = v.findViewById<EditText>(R.id.inPaid)

        existing?.let {
            name.setText(it.name)
            mobile.setText(it.mobile)
            place.setText(it.place)
            hours.setText(it.hours.toString())
            rate.setText(it.ratePerHour.toString())
            paid.setText(it.amountPaid.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Labour" else "Edit Labour")
            .setView(v)
            .setPositiveButton("Save") { _, _ ->
                val l = existing ?: Labour().also { labours.add(it) }
                l.name = name.text.toString().trim()
                l.mobile = mobile.text.toString().trim()
                l.place = place.text.toString().trim()
                l.hours = hours.text.toString().toDoubleOrNull() ?: 0.0
                l.ratePerHour = rate.text.toString().toDoubleOrNull() ?: 0.0
                l.amountPaid = paid.text.toString().toDoubleOrNull() ?: 0.0
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

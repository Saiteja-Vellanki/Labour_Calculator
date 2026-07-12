package com.labourcalc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LabourAdapter(
    private val items: List<Labour>,
    private val onEdit: (Labour) -> Unit,
    private val onMarkPaid: (Labour) -> Unit,
    private val onCall: (Labour) -> Unit,
    private val onDelete: (Labour) -> Unit
) : RecyclerView.Adapter<LabourAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val details: TextView = v.findViewById(R.id.tvDetails)
        val amounts: TextView = v.findViewById(R.id.tvAmounts)
        val status: TextView = v.findViewById(R.id.tvStatus)
        val btnPaid: Button = v.findViewById(R.id.btnMarkPaid)
        val btnCall: Button = v.findViewById(R.id.btnCall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_labour, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val l = items[pos]
        h.name.text = l.name
        h.details.text = "${l.place}  •  ${l.mobile}\n${l.hours} hrs × ₹${"%.2f".format(l.ratePerHour)}/hr"
        h.amounts.text =
            "Total: ₹${"%.2f".format(l.total)}   Paid: ₹${"%.2f".format(l.amountPaid)}   Balance: ₹${"%.2f".format(l.balance)}"

        if (l.isPaid) {
            h.status.text = "PAID ✔"
            h.status.setBackgroundResource(R.drawable.bg_status_paid)
            h.btnPaid.visibility = View.GONE
        } else {
            h.status.text = "DUE ₹${"%.0f".format(l.balance)}"
            h.status.setBackgroundResource(R.drawable.bg_status_due)
            h.btnPaid.visibility = View.VISIBLE
        }

        h.itemView.setOnClickListener { onEdit(l) }
        h.itemView.setOnLongClickListener { onDelete(l); true }
        h.btnPaid.setOnClickListener { onMarkPaid(l) }
        h.btnCall.setOnClickListener { onCall(l) }
    }
}

package com.labourcalc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Labour(
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var mobile: String = "",
    var place: String = "",
    var hours: Double = 0.0,
    var ratePerHour: Double = 0.0,
    var amountPaid: Double = 0.0
) {
    val total: Double get() = hours * ratePerHour
    val balance: Double get() = total - amountPaid
    val isPaid: Boolean get() = balance <= 0.009 && total > 0
}

object LabourStore {
    private const val PREFS = "labour_store"
    private const val KEY = "labours"

    fun load(context: Context): MutableList<Labour> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<Labour>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Labour(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    mobile = o.optString("mobile"),
                    place = o.optString("place"),
                    hours = o.optDouble("hours", 0.0),
                    ratePerHour = o.optDouble("rate", 0.0),
                    amountPaid = o.optDouble("paid", 0.0)
                )
            )
        }
        return list
    }

    fun save(context: Context, list: List<Labour>) {
        val arr = JSONArray()
        for (l in list) {
            arr.put(
                JSONObject()
                    .put("id", l.id)
                    .put("name", l.name)
                    .put("mobile", l.mobile)
                    .put("place", l.place)
                    .put("hours", l.hours)
                    .put("rate", l.ratePerHour)
                    .put("paid", l.amountPaid)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}

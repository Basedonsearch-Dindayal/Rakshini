package com.example.rakshini.ui.period_tracker

import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rakshini.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PeriodTrackerActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var logPeriodFab: FloatingActionButton
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val PREFS_NAME = "PeriodTrackerPrefs"
    private val KEY_LOGGED_DATES = "logged_dates"
    private lateinit var dateAdapter: DateAdapter
    private val loggedDates = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_period_tracker)

        // Initialize views
        calendarView = findViewById(R.id.calendar_view)
        logPeriodFab = findViewById(R.id.log_period_fab)

        // Set up RecyclerView
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.dates_recycler_view)
        dateAdapter = DateAdapter { position ->
            deleteDate(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = dateAdapter

        // Load saved dates
        loadLoggedDates()

        // Set calendar date change listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            try {
                val calendar = Calendar.getInstance()
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)
                if (!loggedDates.contains(selectedDate)) {
                    saveLoggedDate(calendar.timeInMillis)
                    loggedDates.add(selectedDate)
                    dateAdapter.updateDates(loggedDates)
                    Toast.makeText(this, "Date logged: $selectedDate", Toast.LENGTH_SHORT).show()
                    predictNextPeriod()
                } else {
                    Toast.makeText(this, "Date already logged: $selectedDate", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PeriodTracker", "Error on date selection: ${e.message}", e)
                Toast.makeText(this, "Error logging date", Toast.LENGTH_SHORT).show()
            }
        }

        // FAB click to log current date
        logPeriodFab.setOnClickListener {
            try {
                val calendar = Calendar.getInstance()
                val currentDate = dateFormat.format(calendar.time)
                if (!loggedDates.contains(currentDate)) {
                    saveLoggedDate(calendar.timeInMillis)
                    loggedDates.add(currentDate)
                    dateAdapter.updateDates(loggedDates)
                    Toast.makeText(this, "Current date logged: $currentDate", Toast.LENGTH_SHORT).show()
                    predictNextPeriod()
                } else {
                    Toast.makeText(this, "Date already logged: $currentDate", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PeriodTracker", "Error on FAB click: ${e.message}", e)
                Toast.makeText(this, "Error logging date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLoggedDates() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedDatesMillis = prefs.getStringSet(KEY_LOGGED_DATES, mutableSetOf()) ?: mutableSetOf()
            loggedDates.clear()
            savedDatesMillis.forEach { millis ->
                try {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = millis.toLong()
                    val formattedDate = dateFormat.format(calendar.time)
                    if (!loggedDates.contains(formattedDate)) {
                        loggedDates.add(formattedDate)
                    }
                } catch (e: NumberFormatException) {
                    Log.w("PeriodTracker", "Invalid date format in prefs: $millis", e)
                }
            }
            loggedDates.sortByDescending { dateFormat.parse(it) } // Fixed by ensuring Date is imported
            Log.d("PeriodTracker", "Loaded dates: $loggedDates")
            dateAdapter.updateDates(loggedDates)
        } catch (e: Exception) {
            Log.e("PeriodTracker", "Error loading dates: ${e.message}", e)
            Toast.makeText(this, "Error loading dates", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLoggedDate(dateMillis: Long) {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val editor = prefs.edit()
            val savedDates = prefs.getStringSet(KEY_LOGGED_DATES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            if (!savedDates.contains(dateMillis.toString())) {
                savedDates.add(dateMillis.toString())
                editor.putStringSet(KEY_LOGGED_DATES, savedDates)
                editor.apply()
            }
        } catch (e: Exception) {
            Log.e("PeriodTracker", "Error saving date: ${e.message}", e)
            Toast.makeText(this, "Error saving date", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteDate(position: Int) {
        try {
            if (position in 0 until loggedDates.size) {
                val dateToDelete = loggedDates[position]
                val calendar = Calendar.getInstance()
                calendar.time = dateFormat.parse(dateToDelete) ?: return
                val dateMillis = calendar.timeInMillis

                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val editor = prefs.edit()
                val savedDates = prefs.getStringSet(KEY_LOGGED_DATES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                savedDates.remove(dateMillis.toString())
                editor.putStringSet(KEY_LOGGED_DATES, savedDates)
                editor.apply()

                loggedDates.removeAt(position)
                dateAdapter.updateDates(loggedDates)
                Toast.makeText(this, "Date deleted: $dateToDelete", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PeriodTracker", "Error deleting date: ${e.message}", e)
            Toast.makeText(this, "Error deleting date", Toast.LENGTH_SHORT).show()
        }
    }

    private fun predictNextPeriod() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedDatesMillis = prefs.getStringSet(KEY_LOGGED_DATES, mutableSetOf())?.mapNotNull { it.toLongOrNull() } ?: emptyList()
            if (savedDatesMillis.size >= 2) {
                val latestDate = savedDatesMillis.maxOrNull() ?: 0L
                val secondLatestDate = savedDatesMillis.sortedDescending()[1]
                val cycleLength = (latestDate - secondLatestDate) / (1000 * 60 * 60 * 24) // Days
                val nextPeriod = latestDate + (cycleLength * 1000 * 60 * 60 * 24)
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = nextPeriod
                val predictedDate = dateFormat.format(calendar.time)
                Toast.makeText(this, "Next period predicted for $predictedDate", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("PeriodTracker", "Error predicting next period: ${e.message}", e)
            Toast.makeText(this, "Error predicting next period", Toast.LENGTH_SHORT).show()
        }
    }
}

// Custom Adapter for RecyclerView
class DateAdapter(private val onDelete: (Int) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<DateAdapter.ViewHolder>() {

    private var dates = mutableListOf<String>()

    fun updateDates(newDates: List<String>) {
        dates.clear()
        dates.addAll(newDates)
        notifyDataSetChanged()
        Log.d("DateAdapter", "Updated dates: $dates")
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.date_item, parent, false)
        return ViewHolder(view, onDelete)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            holder.bind(dates[position], position)
            Log.d("DateAdapter", "Binding date at position $position: ${dates[position]}")
        } catch (e: Exception) {
            Log.e("DateAdapter", "Error binding view holder at $position: ${e.message}", e)
        }
    }

    override fun getItemCount(): Int = dates.size

    class ViewHolder(itemView: android.view.View, private val onDelete: (Int) -> Unit) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val dateTextView: android.widget.TextView = itemView.findViewById(R.id.date_text)
        private val deleteButton: android.widget.Button = itemView.findViewById(R.id.delete_button)

        fun bind(date: String, position: Int) {
            try {
                dateTextView.text = date
                deleteButton.setOnClickListener {
                    onDelete(position)
                }
                Log.d("ViewHolder", "Bound date: $date at position $position")
            } catch (e: Exception) {
                Log.e("ViewHolder", "Error binding date: $date at position $position: ${e.message}", e)
            }
        }
    }
}
package com.example.gamapulse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.gamapulse.model.MoodData
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ViewCalendarActivity : AppCompatActivity() {
    private lateinit var calendarGrid: GridView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val calendar = Calendar.getInstance()
    private lateinit var adapter: CalendarAdapter
    private val moodData = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_calendar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        calendarGrid = findViewById(R.id.calendarGrid)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        initializeCalendar()

        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
        setupSwipeRefresh()
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            fetchMoodDataForMonth(true)
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.teal_200,
            R.color.blue_500
        )
    }

    private fun initializeCalendar() {
        adapter = CalendarAdapter(this, calendar, moodData)
        calendarGrid.adapter = adapter
        calendarGrid.setOnItemClickListener { _, _, position, _ ->
            val date = adapter.getDateAtPosition(position)
            if (date != null && isInCurrentMonth(date)) {
                val intent = Intent(this, EditMoodNotesActivity::class.java)
                val calendar = Calendar.getInstance().apply { time = date }
                intent.putExtra("day", calendar.get(Calendar.DAY_OF_MONTH))
                intent.putExtra("month", calendar.get(Calendar.MONTH) + 1)
                intent.putExtra("year", calendar.get(Calendar.YEAR))
                startActivity(intent)
            }
        }
        updateCalendar()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data calendar saat activity menjadi visible kembali
        updateCalendar()
    }

    private fun fetchMoodDataForMonth(isRefreshing: Boolean = false) {
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)

        if (!isRefreshing) {
            loadingProgressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                val sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)

                if (token == null) {
                    loadingProgressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    showErrorDialog("Token autentikasi tidak ditemukan")
                    return@launch
                }

                val response = ApiClient.apiService.getCalendarMoods(
                    "Bearer $token",
                    month,
                    year
                )

                loadingProgressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    response.body()?.let { calendarResponse ->
                        processMoodData(calendarResponse.data)
                    }
                } else {
                    Log.e("ViewCalendarActivity", "Error: ${response.code()} - ${response.message()}")
                    showErrorDialog("Gagal memuat data: ${response.message()}")
                }
            } catch (e: Exception) {
                loadingProgressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                Log.e("ViewCalendarActivity", "Exception: ${e.message}", e)
                showErrorDialog("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    private fun processMoodData(apiMoodData: Map<String, MoodData>) {
        moodData.clear()

        for ((day, moodData) in apiMoodData) {
            val cal = calendar.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, day.toInt())

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateKey = dateFormat.format(cal.time)

            // We only need mood_level, ignore mood_note
            val moodDrawableId = when (moodData.mood_level) {
                1 -> R.drawable.icon_mood_marah
                2 -> R.drawable.icon_mood_sedih
                3 -> R.drawable.icon_mood_biasa
                4 -> R.drawable.icon_mood_bahagia
                else -> R.drawable.icon_mood_biasa
            }

            this.moodData[dateKey] = moodDrawableId.toString()
        }

        adapter.notifyDataSetChanged()
    }

    private fun showErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun isInCurrentMonth(date: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = calendar.clone() as Calendar
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun updateCalendar() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("id"))
        tvMonthYear.text = dateFormat.format(calendar.time)
        adapter.updateCalendar(calendar)
        fetchMoodDataForMonth()
    }

    private fun openEditMoodActivity(date: Date) {
        val intent = Intent(this, EditMoodNotesActivity::class.java)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        intent.putExtra("DATE", dateFormat.format(date))
        startActivity(intent)
    }

    inner class CalendarAdapter(
        private val context: Context,
        private var currentCalendar: Calendar,
        private val moodData: HashMap<String, String>
    ) : BaseAdapter() {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val dates = ArrayList<Date?>()
        private val inflater = LayoutInflater.from(context)

        init {
            updateCalendar(currentCalendar)
        }

        fun updateCalendar(calendar: Calendar) {
            this.currentCalendar = calendar.clone() as Calendar
            dates.clear()
            val monthCalendar = calendar.clone() as Calendar
            monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfMonth = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1
            monthCalendar.add(Calendar.DAY_OF_MONTH, -firstDayOfMonth)
            while (dates.size < 42) {
                dates.add(monthCalendar.time)
                monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            notifyDataSetChanged()
        }

        fun getDateAtPosition(position: Int): Date? {
            return if (position >= 0 && position < dates.size) {
                dates[position]
            } else {
                null
            }
        }

        override fun getCount(): Int = dates.size
        override fun getItem(position: Int): Any? = dates[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_calendar_day, parent, false)
            val date = dates[position]
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val ivEmoji: ImageView = view.findViewById(R.id.tvEmoji)

            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                tvDate.text = cal.get(Calendar.DAY_OF_MONTH).toString()

                if (cal.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
                    tvDate.setTextColor(context.getColor(android.R.color.black))
                    val dateKey = dateFormat.format(date)
                    if (moodData.containsKey(dateKey)) {
                        val moodDrawableId = moodData[dateKey]?.toIntOrNull() ?: R.drawable.icon_mood_biasa
                        ivEmoji.setImageResource(moodDrawableId)
                        ivEmoji.visibility = View.VISIBLE
                    } else {
                        ivEmoji.visibility = View.INVISIBLE
                    }
                } else {
                    tvDate.setTextColor(context.getColor(android.R.color.darker_gray))
                    ivEmoji.visibility = View.INVISIBLE
                }

                val today = Calendar.getInstance()
                if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                    view.setBackgroundResource(R.drawable.calendar_cell_today)
                } else {
                    view.setBackgroundResource(R.drawable.calendar_cell_background)
                }
            }
            return view
        }
    }
}
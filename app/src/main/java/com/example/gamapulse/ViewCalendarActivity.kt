package com.example.gamapulse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random

class ViewCalendarActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton

    private val calendar = Calendar.getInstance()
    private lateinit var adapter: CalendarAdapter

    // Emoji yang akan digunakan (4 jenis)
    private val emojiList = listOf("😊", "😢", "😡", "😴")

    // Menyimpan data mood untuk setiap tanggal
    private val moodData = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(R.layout.activity_view_calendar)

        // Inisialisasi views
        calendarGrid = findViewById(R.id.calendarGrid)
        tvMonthYear = findViewById(R.id.tvMonthYear)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        // Setup kalender
        initializeCalendar()

        // Setup listeners untuk navigasi bulan
        btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }
    }

    private fun initializeCalendar() {
        // Acak emoji untuk contoh tampilan
        generateRandomMoodData()

        // Set adapter
        adapter = CalendarAdapter(this, calendar, moodData)
        calendarGrid.adapter = adapter

        // Set click listener pada grid
        calendarGrid.setOnItemClickListener { _, _, position, _ ->
            val date = adapter.getDateAtPosition(position)
            if (date != null && isInCurrentMonth(date)) {
                openEditMoodActivity(date)
            }
        }

        // Update tampilan kalender
        updateCalendar()
    }

    private fun generateRandomMoodData() {
        val random = Random()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Generate acak mood untuk bulan ini
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            monthCalendar.set(Calendar.DAY_OF_MONTH, day)
            val dateKey = dateFormat.format(monthCalendar.time)
            moodData[dateKey] = emojiList[random.nextInt(emojiList.size)]
        }
    }

    private fun isInCurrentMonth(date: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = calendar.clone() as Calendar
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun updateCalendar() {
        // Update teks bulan dan tahun
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("id"))
        tvMonthYear.text = dateFormat.format(calendar.time)

        // Update adapter
        adapter.updateCalendar(calendar)
    }

    private fun openEditMoodActivity(date: Date) {
        val intent = Intent(this, EditMoodNotesActivity::class.java)
        // Tambahkan data tanggal ke intent
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

            // Bersihkan list tanggal
            dates.clear()

            // Dapatkan tanggal awal bulan
            val monthCalendar = calendar.clone() as Calendar
            monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

            // Tentukan hari pertama dalam minggu (0 = Minggu, 1 = Senin, dst)
            val firstDayOfMonth = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1

            // Mundur ke hari Minggu awal
            monthCalendar.add(Calendar.DAY_OF_MONTH, -firstDayOfMonth)

            // Isi sel
            while (dates.size < 42) { // 6 baris dari 7 hari
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
            // Setup tampilan untuk setiap sel
            val view = convertView ?: inflater.inflate(R.layout.item_calendar_day, parent, false)
            val date = dates[position]

            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvEmoji: TextView = view.findViewById(R.id.tvEmoji)

            if (date != null) {
                // Set tanggal
                val cal = Calendar.getInstance().apply { time = date }
                tvDate.text = cal.get(Calendar.DAY_OF_MONTH).toString()

                // Periksa apakah tanggal berada di bulan saat ini
                if (cal.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
                    tvDate.setTextColor(context.getColor(android.R.color.black))

                    // Set emoji
                    val dateKey = dateFormat.format(date)
                    val emoji = moodData[dateKey] ?: ""
                    tvEmoji.text = emoji
                    tvEmoji.visibility = View.VISIBLE
                } else {
                    // Tanggal di luar bulan saat ini
                    tvDate.setTextColor(context.getColor(android.R.color.darker_gray))
                    tvEmoji.visibility = View.INVISIBLE
                }

                // Periksa apakah tanggal adalah hari ini
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
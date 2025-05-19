package com.example.gamapulse

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.content.Intent
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gamapulse.model.MoodInfo
import com.example.gamapulse.model.ProgressInfo
import com.example.gamapulse.network.ApiClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportFragment : Fragment() {
    private lateinit var moodBarChart: BarChart
    private lateinit var taskLineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var spinnerTimePeriod: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerWeek: Spinner
    private lateinit var tabMoodTracker: TextView
    private lateinit var tabTaskCompletion: TextView
    private lateinit var chartTitle: TextView
    private var currentTab = 0

    // Initialize with empty collections to avoid null reference exceptions
    private val moodData = mutableMapOf<String, MoodInfo>()
    private val progressData = mutableMapOf<String, ProgressInfo>()
    private val averageMoodData = mutableMapOf<String, Double>()
    private val dayLabels = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)
        setupProfileButton(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        moodBarChart = view.findViewById(R.id.moodBarChart)
        taskLineChart = view.findViewById(R.id.taskLineChart)
        barChart = view.findViewById(R.id.barChart)
        spinnerTimePeriod = view.findViewById(R.id.spinner_time_period)
        spinnerMonth = view.findViewById(R.id.spinner_month)
        spinnerWeek = view.findViewById(R.id.spinner_week)
        tabMoodTracker = view.findViewById(R.id.tab_mood_tracker)
        tabTaskCompletion = view.findViewById(R.id.tab_pengerjaan_tugas)
        chartTitle = view.findViewById(R.id.chart_title)

        setupTabs()
        setupSpinners()
        setupMoodBarChart()
        setupTaskLineChart()
        setupBarChart()

        // Fetch initial data
        fetchReportData()
    }

    private fun setupTabs() {
        tabMoodTracker.setOnClickListener {
            if (currentTab != 0) {
                currentTab = 0
                updateTabUI()
                updateCharts()
            }
        }

        tabTaskCompletion.setOnClickListener {
            if (currentTab != 1) {
                currentTab = 1
                updateTabUI()
                updateCharts()
            }
        }

        updateTabUI()
    }

    private fun updateTabUI() {
        if (currentTab == 0) {
            tabMoodTracker.setBackgroundResource(R.drawable.report_selected_tab_background)
            tabTaskCompletion.setBackgroundResource(R.drawable.report_unselected_tab_background)
            chartTitle.text = "Progress Mood dalam waktu"
            moodBarChart.visibility = View.VISIBLE
            taskLineChart.visibility = View.GONE
            barChart.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.mood_distribution_card)?.visibility = View.VISIBLE
        } else {
            tabMoodTracker.setBackgroundResource(R.drawable.report_unselected_tab_background)
            tabTaskCompletion.setBackgroundResource(R.drawable.report_selected_tab_background)
            chartTitle.text = "Progress Pengerjaan Tugas"
            moodBarChart.visibility = View.GONE
            taskLineChart.visibility = View.VISIBLE
            barChart.visibility = View.GONE
            view?.findViewById<View>(R.id.mood_distribution_card)?.visibility = View.GONE
        }
    }

    private fun setupSpinners() {
        val timePeriods = arrayOf("Mingguan", "Bulanan")
        spinnerTimePeriod.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, timePeriods
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        spinnerMonth.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, months
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        // Set current month
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        spinnerMonth.setSelection(currentMonth)

        val weeks = arrayOf("Minggu ke-1", "Minggu ke-2", "Minggu ke-3", "Minggu ke-4")
        spinnerWeek.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, weeks
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerWeek.visibility = View.GONE

        spinnerTimePeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                spinnerWeek.visibility = if (position == 0) View.VISIBLE else View.GONE
                fetchReportData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                fetchReportData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerWeek.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                fetchReportData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchReportData() {
        val selectedMonth = spinnerMonth.selectedItemPosition + 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val isWeekly = spinnerTimePeriod.selectedItemPosition == 0
        val selectedWeek = if (isWeekly) spinnerWeek.selectedItemPosition + 1 else 0

        val loadingDialog = ProgressDialog(requireContext()).apply {
            setMessage("Memuat data laporan...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                val sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)

                if (token == null) {
                    loadingDialog.dismiss()
                    showErrorDialog("Session expired, please login again")
                    return@launch
                }

                val authToken = "Bearer $token"
                val response = ApiClient.apiService.getReport(authToken, selectedMonth, currentYear)

                loadingDialog.dismiss()

                if (response.isSuccessful) {
                    val reportResponse = response.body()

                    if (reportResponse != null && reportResponse.success) {
                        // Clear existing data
                        moodData.clear()
                        progressData.clear()
                        averageMoodData.clear()
                        dayLabels.clear()

                        // Process mood and progress data
                        reportResponse.chartData.mood?.let { moodData.putAll(it) }
                        reportResponse.chartData.progress?.let { progressData.putAll(it) }

                        // Process average mood data - make sure to handle it explicitly
                        reportResponse.chartData.averageMood?.let { avgMoodMap ->
                            Log.d("ReportFragment", "Raw averageMood data: $avgMoodMap")
                            // Only include numeric keys (1-4) and filter out "unknown"
                            for ((key, value) in avgMoodMap) {
                                if (key != "unknown" && key.toIntOrNull() != null) {
                                    averageMoodData[key] = value
                                    Log.d("ReportFragment", "Added mood $key: $value to averageMoodData")
                                }
                            }
                        }

                        Log.d("ReportFragment", "Final averageMoodData: $averageMoodData")

                        // Create day labels
                        if (isWeekly) {
                            createWeekDayLabels(selectedWeek)
                            // Filter data for selected week
                            filterDataForWeek(selectedWeek)
                        } else {
                            createMonthDayLabels(selectedMonth, currentYear)
                        }

                        // Update charts with new data
                        updateCharts()
                    } else {
                        val errorMsg = "Failed to load report data"
                        showErrorDialog(errorMsg)
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("ReportFragment", "API Error: $errorMessage")
                    showErrorDialog("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Log.e("ReportFragment", "Exception fetching report data", e)
                showErrorDialog("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    private fun createWeekDayLabels(weekNumber: Int) {
        // Calculate day range for the selected week (1-7, 8-14, 15-21, 22-28/29/30/31)
        val startDay = (weekNumber - 1) * 7 + 1
        val endDay = when (weekNumber) {
            4 -> {
                // Get actual days in month
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, spinnerMonth.selectedItemPosition)
                cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            else -> weekNumber * 7
        }

        // Create complete set of day labels for the week
        dayLabels.clear()
        for (day in startDay..endDay) {
            dayLabels.add(day)
        }
    }

    private fun createMonthDayLabels(month: Int, year: Int) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month - 1) // Calendar.MONTH is 0-based
        cal.set(Calendar.YEAR, year)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        dayLabels.clear()
        for (day in 1..daysInMonth) {
            dayLabels.add(day)
        }
    }

    private fun filterDataForWeek(weekNumber: Int) {
        // Calculate day range for the selected week (1-7, 8-14, 15-21, 22-31)
        val startDay = (weekNumber - 1) * 7 + 1
        val endDay = if (weekNumber == 4) 31 else weekNumber * 7

        // Filter labels and corresponding data
        val filteredLabels = dayLabels.filter { it in startDay..endDay }
        val filteredDays = filteredLabels.map { it.toString() }

        // Keep only data for days within selected week
        val filteredMoodData = moodData.filterKeys { it in filteredDays }
        val filteredProgressData = progressData.filterKeys { it in filteredDays }

        // Update data collections
        dayLabels.clear()
        dayLabels.addAll(filteredLabels)

        moodData.clear()
        moodData.putAll(filteredMoodData)

        progressData.clear()
        progressData.putAll(filteredProgressData)
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupMoodBarChart() {
        with(moodBarChart) {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(true)
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 5f
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(true)
            }

            // Remove value labels
            setDrawValueAboveBar(false)

            // Improve appearance
            animateY(1000)
            extraBottomOffset = 10f
        }
    }

    private fun setupTaskLineChart() {
        with(taskLineChart) {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(true)
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                setDrawGridLines(false)
                setDrawAxisLine(true)
                valueFormatter = PercentFormatter()
            }

            // Improve appearance
            animateX(1000)
            extraBottomOffset = 10f
        }
    }

    private fun setupBarChart() {
        with(barChart) {
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawAxisLine(true)
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                setDrawGridLines(false)
                setDrawAxisLine(true)
                valueFormatter = PercentFormatter()
            }

            // Remove value labels
            setDrawValueAboveBar(false)

            // Improve appearance
            animateY(1000)
            extraBottomOffset = 10f
        }

        val moodLabels = listOf("Marah", "Sedih", "Bahagia", "Biasa saja")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(moodLabels)
    }

    private fun updateCharts() {
        if (currentTab == 0) {
            updateMoodChart()
            updateAverageMoodChart()
        } else {
            updateProgressChart()
        }
    }

    private fun updateMoodChart() {
        if (dayLabels.isEmpty()) {
            moodBarChart.data = null
            moodBarChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()

        // Create entries for all days, with zero height for days without data
        for (i in dayLabels.indices) {
            val day = dayLabels[i]
            val dayKey = day.toString()

            if (moodData.containsKey(dayKey) && moodData[dayKey]?.mood_level != null) {
                val moodLevel = moodData[dayKey]?.mood_level ?: 0
                entries.add(BarEntry(i.toFloat(), moodLevel.toFloat()))

                val color = when (moodLevel) {
                    1 -> Color.rgb(255, 87, 34)  // Marah
                    2 -> Color.rgb(96, 125, 139) // Sedih
                    3 -> Color.rgb(76, 175, 80)  // Bahagia
                    4 -> Color.rgb(0, 188, 212)  // Biasa saja
                    else -> Color.GRAY
                }
                colors.add(color)
            } else {
                entries.add(BarEntry(i.toFloat(), 0f))
                colors.add(Color.GRAY)
            }
        }

        val dataSet = BarDataSet(entries, "Mood").apply {
            this.colors = colors
            setDrawValues(false)  // Don't show the numeric values
        }

        moodBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels.map { it.toString() })
        moodBarChart.xAxis.labelCount = dayLabels.size

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        moodBarChart.data = barData
        moodBarChart.invalidate()
    }

    private fun updateProgressChart() {
        if (dayLabels.isEmpty()) {
            taskLineChart.data = null
            taskLineChart.invalidate()
            return
        }

        val targetEntries = ArrayList<Entry>()
        val actualEntries = ArrayList<Entry>()

        // Create entries for all days, with zero values for days without data
        for (i in dayLabels.indices) {
            val day = dayLabels[i]
            val dayKey = day.toString()

            if (progressData.containsKey(dayKey)) {
                val progressInfo = progressData[dayKey]
                targetEntries.add(Entry(i.toFloat(), (progressInfo?.expected_target ?: 0.0).toFloat()))
                actualEntries.add(Entry(i.toFloat(), (progressInfo?.actual_target ?: 0.0).toFloat()))
            } else {
                targetEntries.add(Entry(i.toFloat(), 0f))
                actualEntries.add(Entry(i.toFloat(), 0f))
            }
        }

        val targetDataSet = LineDataSet(targetEntries, "Target").apply {
            color = Color.rgb(76, 175, 80)
            setCircleColor(Color.rgb(76, 175, 80))
            setDrawValues(false)
            lineWidth = 2.5f
            circleRadius = 4f
        }

        val actualDataSet = LineDataSet(actualEntries, "Realisasi").apply {
            color = Color.rgb(255, 152, 0)
            setCircleColor(Color.rgb(255, 152, 0))
            setDrawValues(false)
            lineWidth = 2.5f
            circleRadius = 4f
        }

        taskLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels.map { it.toString() })
        taskLineChart.xAxis.labelCount = dayLabels.size

        val lineData = LineData(targetDataSet, actualDataSet)
        taskLineChart.data = lineData
        taskLineChart.invalidate()
    }

    private fun updateAverageMoodChart() {
        Log.d("ReportFragment", "Updating average mood chart with data: $averageMoodData")

        // Ensure the chart card is visible
        view?.findViewById<View>(R.id.mood_distribution_card)?.visibility = View.VISIBLE

        if (averageMoodData.isEmpty()) {
            Log.w("ReportFragment", "No average mood data available")
            barChart.setNoDataText("Tidak ada data mood rata-rata")
            barChart.data = null
            barChart.invalidate()
            return
        }

        val barEntries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()

        // Map mood levels to positions
        val moodLabels = listOf("Marah", "Sedih", "Bahagia", "Biasa saja")

        for (moodLevel in 1..4) {
            val moodKey = moodLevel.toString()
            val value = averageMoodData[moodKey] ?: 0.0
            Log.d("ReportFragment", "Adding mood $moodKey with value: $value")
            barEntries.add(BarEntry((moodLevel - 1).toFloat(), value.toFloat()))

            val color = when (moodLevel) {
                1 -> Color.rgb(255, 87, 34)  // Marah
                2 -> Color.rgb(96, 125, 139) // Sedih
                3 -> Color.rgb(76, 175, 80)  // Bahagia
                4 -> Color.rgb(0, 188, 212)  // Biasa saja
                else -> Color.GRAY
            }
            colors.add(color)
        }

        val barDataSet = BarDataSet(barEntries, "Persentase Mood").apply {
            this.colors = colors
            setDrawValues(true)
            valueFormatter = PercentFormatter()
            valueTextSize = 12f
            valueTextColor = Color.BLACK
        }

        val barData = BarData(barDataSet)
        barData.barWidth = 0.6f

        // Configure the chart for better display
        barChart.apply {
            data = barData
            setFitBars(true)
            description.isEnabled = false
            animateY(1000)
            setExtraOffsets(10f, 10f, 10f, 10f)
            visibility = View.VISIBLE
            invalidate()
        }
    }

    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
                view.postDelayed(action, 100)
            }
            .start()
    }

    private fun setupProfileButton(view: View) {
        val profileButton = view.findViewById<ImageView>(R.id.btn_profil)
        profileButton.setOnClickListener {
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), ProfilActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Helper class for percentage formatting
    inner class PercentFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format("%.1f%%", value)
        }
    }
}
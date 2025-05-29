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
import com.github.mikephil.charting.formatter.PercentFormatter
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
        spinnerTimePeriod.setSelection(1)
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
                        moodData.clear()
                        progressData.clear()
                        averageMoodData.clear()
                        dayLabels.clear()
                        reportResponse.chartData.mood?.let { moodData.putAll(it) }
                        reportResponse.chartData.progress?.let { progressData.putAll(it) }
                        Log.d("ReportFragment", "Processing average mood data: ${reportResponse.averageMood}")
                        reportResponse.chartData.averageMood?.let { avgMoodMap ->
                            for ((key, value) in avgMoodMap) {
                                // Store all numeric keys (filter out "unknown")
                                if (key != "unknown") {
                                    averageMoodData[key] = value
                                    Log.d("ReportFragment", "Added mood $key with value $value")
                                }
                            }
                        }

                        val selectedMonth = spinnerMonth.selectedItemPosition + 1
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val isWeekly = spinnerTimePeriod.selectedItemPosition == 0
                        val selectedWeek = if (isWeekly) spinnerWeek.selectedItemPosition + 1 else 0


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
                // Default to 24 hours but will be dynamic based on data
                axisMaximum = 24f
                granularity = 4f
                setDrawGridLines(false)
                setDrawAxisLine(true)
            }

            // Improve appearance
            animateX(1000)
            extraBottomOffset = 10f
        }
    }

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setMaxVisibleValueCount(60)
            setPinchZoom(false)
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)

            axisLeft.setDrawGridLines(false)
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
        }
    }

    private fun updateCharts() {
        if (currentTab == 0) {
            updateMoodChart()
        } else {
            updateProgressChart()
        }
        updateAverageMoodChart()
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
                colors.add(Color.LTGRAY)
            }
        }

        val dataSet = BarDataSet(entries, "Mood").apply {
            this.colors = colors
            setDrawValues(false)
            highLightAlpha = 150
        }

        moodBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels.map { it.toString() })
        moodBarChart.xAxis.labelCount = Math.min(dayLabels.size, 15) // Show fewer labels if many days

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

        var maxValue = 0.1f // Start with small value to handle empty datasets properly

        // Create entries for all days
        for (i in dayLabels.indices) {
            val day = dayLabels[i]
            val dayKey = day.toString()

            if (progressData.containsKey(dayKey)) {
                val progressInfo = progressData[dayKey]
                val expectedTarget = (progressInfo?.expected_target ?: 0.0).toFloat()
                val actualTarget = (progressInfo?.actual_target ?: 0.0).toFloat()

                targetEntries.add(Entry(i.toFloat(), expectedTarget))
                actualEntries.add(Entry(i.toFloat(), actualTarget))

                // Keep track of max value
                if (expectedTarget > maxValue) maxValue = expectedTarget
                if (actualTarget > maxValue) maxValue = actualTarget
            } else {
                targetEntries.add(Entry(i.toFloat(), 0f))
                actualEntries.add(Entry(i.toFloat(), 0f))
            }
        }

        // Add padding to max value and ensure it doesn't exceed 24
        maxValue = if (maxValue <= 20f) {
            Math.min(24f, maxValue * 1.25f) // 25% padding for smaller values
        } else {
            Math.min(24f, maxValue * 1.1f)  // 10% padding for larger values
        }

        // Ensure minimum scale is reasonable
        if (maxValue < 4f) maxValue = 4f

        taskLineChart.axisLeft.axisMaximum = maxValue

        val targetDataSet = LineDataSet(targetEntries, "Target").apply {
            color = Color.rgb(0, 153, 204) // Blue color
            setCircleColor(Color.rgb(0, 153, 204))
            setDrawValues(false)
            lineWidth = 2.5f
            circleRadius = 4f
            circleHoleRadius = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = Color.rgb(0, 153, 204)
            highLightColor = Color.rgb(0, 102, 153)
        }

        val actualDataSet = LineDataSet(actualEntries, "Realisasi").apply {
            color = Color.rgb(255, 102, 0) // Orange color
            setCircleColor(Color.rgb(255, 102, 0))
            setDrawValues(false)
            lineWidth = 2.5f
            circleRadius = 4f
            circleHoleRadius = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = Color.rgb(255, 102, 0)
            highLightColor = Color.rgb(204, 51, 0)
        }

        // Update x-axis labels
        taskLineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(dayLabels.map { it.toString() })
            labelCount = Math.min(dayLabels.size, 15) // Limit labels to avoid crowding
            labelRotationAngle = if (dayLabels.size > 15) 30f else 0f
        }

        val lineData = LineData(targetDataSet, actualDataSet)
        taskLineChart.data = lineData
        taskLineChart.animateXY(1000, 1000)
        taskLineChart.invalidate()
    }

    private fun updateAverageMoodChart() {
        Log.d("ReportFragment", "Updating average mood chart with data: $averageMoodData")

        if (averageMoodData.isEmpty()) {
            Log.w("ReportFragment", "No average mood data available")
            barChart.setNoDataText("Tidak ada data mood rata-rata")
            barChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()
        val moodLabels = listOf("Marah", "Sedih", "Bahagia", "Biasa saja")

        // Create entries for each mood type (1-4)
        for (i in 1..4) {
            val key = i.toString()
            val value = averageMoodData[key] ?: 0.0
            entries.add(BarEntry((i-1).toFloat(), value.toFloat()))

            val color = when (i) {
                1 -> Color.rgb(255, 87, 34)   // Marah - Red/Orange
                2 -> Color.rgb(96, 125, 139)  // Sedih - Blue/Gray
                3 -> Color.rgb(76, 175, 80)   // Bahagia - Green
                4 -> Color.rgb(0, 188, 212)   // Biasa saja - Light Blue
                else -> Color.GRAY
            }
            colors.add(color)
        }

        val dataSet = BarDataSet(entries, "Persentase Mood").apply {
            this.colors = colors
            setDrawValues(true)
            valueTextSize = 12f
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f%%", value)
                }
            }
        }

        barChart.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)

            xAxis.valueFormatter = IndexAxisValueFormatter(moodLabels)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)

            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false

            data = BarData(dataSet).apply {
                barWidth = 0.65f
            }

            setFitBars(true)
            animateY(1000)
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
}
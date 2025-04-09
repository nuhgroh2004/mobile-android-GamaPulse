package com.example.gamapulse

import android.graphics.Color
import android.os.Bundle
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
        } else {
            tabMoodTracker.setBackgroundResource(R.drawable.report_unselected_tab_background)
            tabTaskCompletion.setBackgroundResource(R.drawable.report_selected_tab_background)
            chartTitle.text = "Progress Pengerjaan Tugas"
            moodBarChart.visibility = View.GONE
            taskLineChart.visibility = View.VISIBLE
            barChart.visibility = View.GONE
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
        spinnerMonth.setSelection(10)

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
                updateCharts()
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
                updateCharts()
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
                updateCharts()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 10f
            }
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
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
        }
    }

    private fun setupBarChart() {
        with(barChart) {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 10f
            }
        }

        val moodLabels = listOf("Marah", "Sedih", "Bahagia", "Biasa")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(moodLabels)
        updateMoodBarChart()
    }

    private fun updateCharts() {
        if (currentTab == 0) {
            if (spinnerTimePeriod.selectedItemPosition == 0) {
                updateMoodWeeklyBarChart()
            } else {
                updateMoodMonthlyBarChart()
            }
            updateMoodBarChart()
        } else {
            if (spinnerTimePeriod.selectedItemPosition == 0) {
                updateTaskWeeklyChart()
            } else {
                updateTaskMonthlyChart()
            }
        }
    }

    private fun updateMoodWeeklyBarChart() {
        val entries = ArrayList<BarEntry>()
        val days = 7
        val colors = ArrayList<Int>()

        for (i in 0 until days) {
            val moodValue = (3 + Math.random() * 7).toFloat()
            entries.add(BarEntry(i.toFloat(), moodValue))

            val color = when {
                moodValue < 4 -> Color.rgb(255, 87, 34)
                moodValue < 5 -> Color.rgb(96, 125, 139)
                moodValue < 7 -> Color.rgb(0, 188, 212)
                else -> Color.rgb(76, 175, 80)
            }
            colors.add(color)
        }

        val dataSet = BarDataSet(entries, "Mood").apply {
            this.colors = colors
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
        }

        val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Ming")
        moodBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        moodBarChart.data = barData
        moodBarChart.invalidate()
    }

    private fun updateMoodMonthlyBarChart() {
        val entries = ArrayList<BarEntry>()
        val days = 30
        val colors = ArrayList<Int>()

        for (i in 0 until days) {
            val trendFactor = Math.sin(i * 0.3) * 2
            val moodValue = (5 + trendFactor + Math.random() * 2).toFloat()
            entries.add(BarEntry(i.toFloat(), moodValue))

            val color = when {
                moodValue < 4 -> Color.rgb(255, 87, 34)
                moodValue < 5 -> Color.rgb(96, 125, 139)
                moodValue < 7 -> Color.rgb(0, 188, 212)
                else -> Color.rgb(76, 175, 80)
            }
            colors.add(color)
        }

        val dataSet = BarDataSet(entries, "Mood").apply {
            this.colors = colors
            setDrawValues(false)
        }

        val weekLabels = listOf("W1", "W2", "W3", "W4", "W5")
        moodBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(weekLabels)
        moodBarChart.xAxis.labelCount = 5

        val barData = BarData(dataSet)
        barData.barWidth = 0.2f
        moodBarChart.data = barData
        moodBarChart.invalidate()
    }

    private fun updateTaskWeeklyChart() {
        val targetEntries = ArrayList<Entry>()
        val actualEntries = ArrayList<Entry>()
        val days = 7

        for (i in 0 until days) {
            val targetHours = (3 + i * 0.5).toFloat()
            val actualHours = (targetHours * (0.7 + Math.random() * 0.6)).toFloat()

            targetEntries.add(Entry(i.toFloat(), targetHours))
            actualEntries.add(Entry(i.toFloat(), actualHours))
        }

        val targetDataSet = LineDataSet(targetEntries, "Target").apply {
            color = Color.rgb(76, 175, 80)
            setCircleColor(Color.rgb(76, 175, 80))
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            lineWidth = 2f
        }

        val actualDataSet = LineDataSet(actualEntries, "Realisasi").apply {
            color = Color.rgb(255, 152, 0)
            setCircleColor(Color.rgb(255, 152, 0))
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            lineWidth = 2f
        }

        val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Ming")
        taskLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dayLabels)

        val lineData = LineData(targetDataSet, actualDataSet)
        taskLineChart.data = lineData
        taskLineChart.invalidate()
    }

    private fun updateTaskMonthlyChart() {
        val targetEntries = ArrayList<Entry>()
        val actualEntries = ArrayList<Entry>()
        val weeks = 5

        for (i in 0 until weeks) {
            val targetHours = (15 + i * 2).toFloat()
            val actualHours = (targetHours * (0.7 + Math.random() * 0.5)).toFloat()

            targetEntries.add(Entry(i.toFloat(), targetHours))
            actualEntries.add(Entry(i.toFloat(), actualHours))
        }

        val targetDataSet = LineDataSet(targetEntries, "Target").apply {
            color = Color.rgb(76, 175, 80)
            setCircleColor(Color.rgb(76, 175, 80))
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            lineWidth = 2f
        }

        val actualDataSet = LineDataSet(actualEntries, "Realisasi").apply {
            color = Color.rgb(255, 152, 0)
            setCircleColor(Color.rgb(255, 152, 0))
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            lineWidth = 2f
        }

        val weekLabels = listOf("W1", "W2", "W3", "W4", "W5")
        taskLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(weekLabels)

        val lineData = LineData(targetDataSet, actualDataSet)
        taskLineChart.data = lineData
        taskLineChart.invalidate()
    }

    private fun updateMoodBarChart() {
        val barEntries = ArrayList<BarEntry>()

        barEntries.add(BarEntry(0f, 4.5f))
        barEntries.add(BarEntry(1f, 2.5f))
        barEntries.add(BarEntry(2f, 7.2f))
        barEntries.add(BarEntry(3f, 4.8f))

        val barDataSet = BarDataSet(barEntries, "Mood Average")

        val colors = listOf(
            Color.rgb(255, 87, 34),
            Color.rgb(96, 125, 139),
            Color.rgb(76, 175, 80),
            Color.rgb(0, 188, 212)
        )
        barDataSet.colors = colors

        val barData = BarData(barDataSet)
        barData.barWidth = 0.6f

        barChart.data = barData
        barChart.invalidate()
    }

    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .withEndAction { action() }
                    .start()
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
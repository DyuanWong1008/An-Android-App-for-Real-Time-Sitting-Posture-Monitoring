package com.example.postureguard.ui.statistic

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.postureguard.PostureData
import com.example.postureguard.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter

class StatisticFragment : Fragment() {

    private lateinit var tvMonitorTimeValue: TextView
    private lateinit var tvPostureScoreValue: TextView
    private lateinit var tvGoodPostureDetectedValue: TextView
    private lateinit var tvBadPostureDetected: TextView

    private lateinit var tvForwardHeadResult: TextView
    private lateinit var tvCrossLegResult: TextView


    private lateinit var datePickerFrom: EditText
    private lateinit var datePickerTo: EditText
    private val calendar = Calendar.getInstance()
    private lateinit var chartTimeMonitoring: LineChart
    private lateinit var chartPostureScore: BarChart
    private lateinit var chartBadPosture: BarChart
    private lateinit var userId: String
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_statistic, container, false)

        // Initialize UI components
        tvMonitorTimeValue = root.findViewById(R.id.tvMonitorTimeValue)
        tvPostureScoreValue = root.findViewById(R.id.tvPostureScoreValue)
        tvGoodPostureDetectedValue = root.findViewById(R.id.tvGoodPostureDetectedValue)
        tvBadPostureDetected = root.findViewById(R.id.tvBadPostureDetected)
        tvForwardHeadResult = root.findViewById(R.id.tvForwardHeadResult)
        tvCrossLegResult = root.findViewById(R.id.tvCrossLegResult)
        datePickerFrom = root.findViewById(R.id.datePickerFrom)
        datePickerTo = root.findViewById(R.id.datePickerTo)
        chartTimeMonitoring = root.findViewById<LineChart>(R.id.chartTimeMonitoring)
        chartPostureScore = root.findViewById<BarChart>(R.id.chartPostureScore)
        chartBadPosture = root.findViewById<BarChart>(R.id.chartBadPosture)

        // Get current user ID
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: return root

        // Set up date pickers
        setDatePickerListener(datePickerFrom)
        setDatePickerListener(datePickerTo)

        // Load this week's statistics initially
        loadThisWeekStatistics()

        // Example of updating the TextViews dynamically
        updateStatistics("01:23:45", 85, 10, 5, 3)

        return root
    }

    private fun updateStatistics(
        monitorTime: String,
        postureScore: Int,
        goodPostureDetected: Int,
        forwardHead: Int,
        crossLeg: Int
    ) {
        tvMonitorTimeValue.text = getString(R.string.monitor_time, monitorTime)
        tvPostureScoreValue.text = getString(R.string.posture_score, postureScore)
        tvGoodPostureDetectedValue.text = getString(R.string.good_posture_detected, goodPostureDetected)
        tvBadPostureDetected.text = getString(R.string.bad_posture_detected, forwardHead + crossLeg)
        tvForwardHeadResult.text = getString(R.string.forwardhead_result, forwardHead)
        tvCrossLegResult.text = getString(R.string.crossleg_result, crossLeg)
    }

    private fun loadThisWeekStatistics() {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY // 设置一周的第一天为星期一
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startOfWeek = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        loadStatistics(startOfWeek, endOfWeek)
    }

    private fun loadStatistics(fromDate: String = datePickerFrom.text.toString(), toDate: String = datePickerTo.text.toString()) {
        // Validate date input
        if (fromDate.isEmpty() || toDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please select both dates", Toast.LENGTH_SHORT).show()
            return
        }

        // Define date formats
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        // Get today's date for Today section
        val today = dateFormat.format(Calendar.getInstance().time)

        // Fetch today's data separately for Today section
        firestore.collection("posture_data")
            .whereEqualTo("user_id", userId)
            .whereGreaterThanOrEqualTo("start_time", "$today 00:00:00")
            .whereLessThanOrEqualTo("start_time", "$today 23:59:59")
            .get()
            .addOnSuccessListener { documents ->
                val todayDataList = documents.map { document ->
                    PostureData(
                        uuid = document.getString("uuid") ?: "",
                        sessionId = document.getString("session_id") ?: "",
                        startTime = document.getString("start_time") ?: "",
                        endTime = document.getString("end_time") ?: "",
                        badPostureCount = document.getLong("bad_posture_count")?.toInt() ?: 0,
                        forwardheadCount = document.getLong("forwardhead_count")?.toInt() ?: 0,
                        crosslegCount = document.getLong("crossleg_count")?.toInt() ?: 0,
                        standardCount = document.getLong("standard_count")?.toInt() ?: 0,
                        totalMonitoringTime = 0L,
                        postureScore = 0,
                        lastModified = document.getString("last_modified") ?: ""
                    )
                }

                // Display Today part using today's data only (no summing across multiple days)
                if (todayDataList.isNotEmpty()) {
                    val todayMonitoringTime = todayDataList.sumOf {
                        val startTime = dateTimeFormat.parse(it.startTime)?.time ?: 0L
                        val endTime = dateTimeFormat.parse(it.endTime)?.time ?: 0L
                        endTime - startTime
                    }
                    val todayForwardheadCount = todayDataList.sumOf { it.forwardheadCount }
                    val todayCrosslegCount = todayDataList.sumOf { it.crosslegCount }
                    val todayStandardCount = todayDataList.sumOf { it.standardCount }
                    val todayBadPostureCount = todayForwardheadCount + todayCrosslegCount

                    val todayPostureScore = calculatePostureScore(todayStandardCount, todayForwardheadCount, todayCrosslegCount)
                    val todayHours = todayMonitoringTime / (1000 * 60 * 60)
                    val todayMinutes = (todayMonitoringTime % (1000 * 60 * 60)) / (1000 * 60)
                    val todaySeconds = (todayMonitoringTime % (1000 * 60)) / 1000

                    // Update UI for today's part
                    tvMonitorTimeValue.text = getString(R.string.monitor_time, "${"%02d".format(todayHours)}h ${"%02d".format(todayMinutes)}m ${"%02d".format(todaySeconds)}s")
                    tvPostureScoreValue.text = getString(R.string.posture_score, todayPostureScore)
                    tvGoodPostureDetectedValue.text = getString(R.string.good_posture_detected, todayStandardCount)
                    tvBadPostureDetected.text = getString(R.string.bad_posture_detected, todayBadPostureCount)
                    tvForwardHeadResult.text = getString(R.string.forwardhead_result, todayForwardheadCount)
                    tvCrossLegResult.text = getString(R.string.crossleg_result, todayCrosslegCount)
                } else {
                    // Display 0 if no data for today
                    tvMonitorTimeValue.text = getString(R.string.monitor_time, "00h 00m 00s")
                    tvPostureScoreValue.text = getString(R.string.posture_score, 0)
                    tvGoodPostureDetectedValue.text = getString(R.string.good_posture_detected, 0)
                    tvBadPostureDetected.text = getString(R.string.bad_posture_detected, 0)
                    tvForwardHeadResult.text = getString(R.string.forwardhead_result, 0)
                    tvCrossLegResult.text = getString(R.string.crossleg_result, 0)
                }
            }
            .addOnFailureListener { e ->
                Log.e("StatisticFragment", "Error fetching today's data", e)
                Toast.makeText(requireContext(), "Error fetching today's data: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Fetch data for the specified date range
        firestore.collection("posture_data")
            .whereEqualTo("user_id", userId)
            .whereGreaterThanOrEqualTo("start_time", fromDate)
            .whereLessThanOrEqualTo("start_time", toDate)
            .get()
            .addOnSuccessListener { documents ->
                val postureDataList = documents.map { document ->
                    PostureData(
                        uuid = document.getString("uuid") ?: "",
                        sessionId = document.getString("session_id") ?: "",
                        startTime = document.getString("start_time") ?: "",
                        endTime = document.getString("end_time") ?: "",
                        badPostureCount = document.getLong("bad_posture_count")?.toInt() ?: 0,
                        forwardheadCount = document.getLong("forwardhead_count")?.toInt() ?: 0,
                        crosslegCount = document.getLong("crossleg_count")?.toInt() ?: 0,
                        standardCount = document.getLong("standard_count")?.toInt() ?: 0,
                        totalMonitoringTime = 0L,
                        postureScore = 0,
                        lastModified = document.getString("last_modified") ?: ""
                    )
                }

                // Log retrieved data for debugging
                Log.d("StatisticFragment", "Retrieved data: $postureDataList")

                // Process and display data
                processAndDisplayData(postureDataList, dateFormat, dateTimeFormat, fromDate, toDate)
            }
            .addOnFailureListener { e ->
                Log.e("StatisticFragment", "Error fetching data", e)
                Toast.makeText(requireContext(), "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processAndDisplayData(postureDataList: List<PostureData>, dateFormat: SimpleDateFormat, dateTimeFormat: SimpleDateFormat, fromDate: String, toDate: String) {
        // Generate date range
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(fromDate) ?: Date()
        val endDate = dateFormat.parse(toDate) ?: Date()

        val dateRange = mutableListOf<String>()
        while (!calendar.time.after(endDate)) {
            dateRange.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Convert list to map by date
        val postureDataMap = postureDataList.groupBy { dateTimeFormat.parse(it.startTime)?.let { dateFormat.format(it) } }

        // Convert map to list of entries for the charts
        val lineEntriesTimeMonitoring = mutableListOf<Entry>()
        val barEntriesPostureScore = mutableListOf<BarEntry>()
        val barEntriesBadPostureCrossleg = mutableListOf<BarEntry>()
        val barEntriesBadPostureForwardhead = mutableListOf<BarEntry>()

        dateRange.forEachIndexed { index, date ->
            val dataList = postureDataMap[date]
            if (dataList != null && dataList.isNotEmpty()) {
                val totalMonitoringTime = dataList.sumOf {
                    val startTime = dateTimeFormat.parse(it.startTime)?.time ?: 0L
                    val endTime = dateTimeFormat.parse(it.endTime)?.time ?: 0L
                    endTime - startTime
                }
                val hours = totalMonitoringTime / (1000 * 60 * 60).toFloat()
                val totalStandardCount = dataList.sumOf { it.standardCount }
                val totalForwardheadCount = dataList.sumOf { it.forwardheadCount }
                val totalCrosslegCount = dataList.sumOf { it.crosslegCount }

                lineEntriesTimeMonitoring.add(Entry(index.toFloat(), hours))
                barEntriesPostureScore.add(BarEntry(index.toFloat(), calculatePostureScore(totalStandardCount, totalForwardheadCount, totalCrosslegCount).toFloat()))
                barEntriesBadPostureCrossleg.add(BarEntry(index.toFloat(), totalCrosslegCount.toFloat()))
                barEntriesBadPostureForwardhead.add(BarEntry(index.toFloat(), totalForwardheadCount.toFloat()))
            } else {
                // Empty values for missing days
                lineEntriesTimeMonitoring.add(Entry(index.toFloat(), 0f))
                barEntriesPostureScore.add(BarEntry(index.toFloat(), 0f))
                barEntriesBadPostureCrossleg.add(BarEntry(index.toFloat(), 0f))
                barEntriesBadPostureForwardhead.add(BarEntry(index.toFloat(), 0f))
            }
        }

        // Ensure UI updates are on the main thread
        activity?.runOnUiThread {
            // Set chart data
            setLineChartData(chartTimeMonitoring, lineEntriesTimeMonitoring, dateRange, "hours")
            setBarChartData(chartPostureScore, barEntriesPostureScore, dateRange, "Posture Score")
            setBadPostureChartData(chartBadPosture, barEntriesBadPostureCrossleg, barEntriesBadPostureForwardhead, dateRange)
        }
    }

    private fun setDatePickerListener(datePicker: EditText) {
        datePicker.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    datePicker.setText(dateFormat.format(calendar.time))

                    // Reload statistics when date changes
                    loadStatistics()
                },
                year, month, day
            )
            datePickerDialog.show()
        }
    }

    // Line Chart: Time Monitoring
    private fun setLineChartData(chart: LineChart, entries: List<Entry>, dateRange: List<String>, yAxisLabel: String) {
        val dataSet = LineDataSet(entries, "Monitoring Time")
        dataSet.valueTextColor = Color.BLACK
        val lineData = LineData(dataSet)
        chart.data = lineData

        chart.legend.textColor = Color.BLACK
        chart.description.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.valueFormatter = DateValueFormatter(dateRange)
        xAxis.textColor = Color.BLACK // Set X-axis label color to white

        val yAxisLeft = chart.axisLeft
        yAxisLeft.textColor = Color.BLACK // Set Y-axis label color to white

        val yAxisRight = chart.axisRight
        yAxisRight.textColor = Color.BLACK // Set Y-axis label color to white

        chart.setVisibleXRangeMaximum(7f)
        chart.isDragEnabled = entries.size > 7
        chart.isScaleXEnabled = true

        chart.invalidate()
    }

    // Bar Chart: Average Posture Score
    private fun setBarChartData(chart: BarChart, entries: List<BarEntry>, dateRange: List<String>, yAxisLabel: String) {
        val dataSet = BarDataSet(entries, yAxisLabel)
        dataSet.valueTextColor = Color.BLACK // Set value label color to white
        val barData = BarData(dataSet)
        chart.data = barData

        chart.legend.textColor = Color.BLACK
        chart.description.isEnabled = false

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.valueFormatter = DateValueFormatter(dateRange)
        xAxis.textColor = Color.BLACK // Set X-axis label color to white

        val yAxisLeft = chart.axisLeft
        yAxisLeft.textColor = Color.BLACK // Set Y-axis label color to white

        val yAxisRight = chart.axisRight
        yAxisRight.textColor = Color.BLACK // Set Y-axis label color to white

        chart.setVisibleXRangeMaximum(7f)
        chart.isDragEnabled = entries.size > 7
        chart.isScaleXEnabled = true

        chart.invalidate()
    }

    // Bar Chart: Bad Posture Detected (crossleg and forwardhead)
    private fun setBadPostureChartData(chart: BarChart, crosslegEntries: List<BarEntry>, forwardheadEntries: List<BarEntry>, dateRange: List<String>) {
        val crosslegDataSet = BarDataSet(crosslegEntries, "Crossleg").apply { color = Color.RED }
        crosslegDataSet.valueTextColor = Color.BLACK // Set value label color to white
        val forwardheadDataSet = BarDataSet(forwardheadEntries, "Forwardhead").apply { color = Color.BLUE }
        forwardheadDataSet.valueTextColor = Color.BLACK // Set value label color to white

        chart.legend.textColor = Color.BLACK
        chart.description.isEnabled = false

        val barData = BarData(crosslegDataSet, forwardheadDataSet)
        chart.data = barData

        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.3f

        barData.barWidth = barWidth
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = dateRange.size.toFloat()
        chart.groupBars(0f, groupSpace, barSpace)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        xAxis.valueFormatter = DateValueFormatter(dateRange)
        xAxis.textColor = Color.BLACK // Set X-axis label color to white
        xAxis.setCenterAxisLabels(true) // Center labels

        val yAxisLeft = chart.axisLeft
        yAxisLeft.textColor = Color.BLACK // Set Y-axis label color to white

        val yAxisRight = chart.axisRight
        yAxisRight.textColor = Color.BLACK // Set Y-axis label color to white

        chart.setVisibleXRangeMaximum(7f)
        chart.isDragEnabled = crosslegEntries.size > 7
        chart.isScaleXEnabled = true

        chart.invalidate()
    }

    private fun calculatePostureScore(standardCount: Int, forwardheadCount: Int, crosslegCount: Int): Int {
        val totalPostureCount = standardCount + forwardheadCount + crosslegCount
        return if (totalPostureCount > 0) {
            ((standardCount.toDouble() / totalPostureCount) * 100).toInt()
        } else {
            0
        }
    }

    class DateValueFormatter(private val dateRange: List<String>) : ValueFormatter() {

        private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val outputDateFormat = SimpleDateFormat("dd-MM", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index >= 0 && index < dateRange.size) {
                val date = inputDateFormat.parse(dateRange[index])
                outputDateFormat.format(date)
            } else {
                ""
            }
        }
    }

}
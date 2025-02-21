package com.example.postureguard.ui.home

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.postureguard.PostureDatabaseHelper
import com.example.postureguard.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.postureguard.camera.CameraSource
import com.example.postureguard.data.Device
import com.example.postureguard.data.Camera
import com.example.postureguard.databinding.FragmentHomeBinding
import com.example.postureguard.ml.ModelType
import com.example.postureguard.ml.MoveNet
import com.example.postureguard.ml.PoseClassifier
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class HomeFragment : Fragment() {

    companion object {
        private const val FRAGMENT_DIALOG = "fragment_dialog"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedLanguage = "en"

    private lateinit var dbHelper: PostureDatabaseHelper

    private lateinit var surfaceView: SurfaceView
    private var device = Device.NNAPI //Device.CPU
    private var selectedCamera = Camera.BACK

    private var forwardheadCounter = 0
    private var crosslegCounter = 0
    private var standardCounter = 0
    private var missingCounter = 0

    private var poseRegister = "standard"

    private lateinit var tvWordReminder: TextView
    private lateinit var tvDebug: TextView
    private lateinit var spnCamera: Spinner

    private var cameraSource: CameraSource? = null
    private var isClassifyPose = true

    private var isMonitoring = false
    private var startTime: Long = 0

    private lateinit var btnReset: Button
    private lateinit var btnStartPause: Button
    private lateinit var btnSave: Button
    private lateinit var tvTimeMonitoring: TextView

    private var forwardheadCount = 0
    private var crosslegCount = 0
    private var standardCount = 0

    private lateinit var forwardheadPlayer: MediaPlayer
    private lateinit var crosslegPlayer: MediaPlayer
    private lateinit var standardPlayer: MediaPlayer

    private var forwardheadPlayerFlag = true
    private var crosslegPlayerFlag = true
    private var standardPlayerFlag = true

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(parentFragmentManager, FRAGMENT_DIALOG)
            }
        }

    private var changeCameraListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, view: View?, direction: Int, id: Long) {
            changeCamera(direction)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        forwardheadCount = 0
        crosslegCount = 0

        tvDebug = binding.tvDebug
        tvWordReminder = binding.tvWordReminder
        tvWordReminder.visibility = View.GONE
        spnCamera = binding.spnCamera
        surfaceView = binding.surfaceView
        initSpinner()

        btnReset = binding.btnReset
        btnStartPause = binding.btnStartPause
        btnSave = binding.btnSave
        tvTimeMonitoring = binding.tvTimeMonitoring

        btnStartPause.text = getString(R.string.start_monitoring)
        btnStartPause.setOnClickListener { startPauseMonitoring() }
        btnReset.setOnClickListener { resetMonitoring() }
        btnSave.setOnClickListener { saveMonitoringData() }

        btnReset.isEnabled = false
        btnSave.isEnabled = false

        dbHelper = PostureDatabaseHelper(requireContext())

        val sharedPreferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        selectedLanguage = sharedPreferences.getString("selectedLanguage", "en") ?: "en"
        updateWordReminder()

        if (isCameraPermissionGranted()) {
            openCamera()
        } else {
            requestPermission()
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            if (cameraSource == null) {
                withContext(Dispatchers.Main) {
                    openCamera()
                }
            } else {
                cameraSource?.resume()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
        releaseMediaPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        releaseMediaPlayer()
        closeCamera()
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(
            Manifest.permission.CAMERA
        )
    }

    private fun openCamera() {
        initializeMediaPlayers()

        lifecycleScope.launch(Dispatchers.IO) {
            if (isCameraPermissionGranted()) {
                if (cameraSource == null) {
                    cameraSource = CameraSource(surfaceView, selectedCamera, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {
                            // Optional: Update FPS display
                        }

                        override fun onDetectedInfo(personScore: Float?, poseLabels: List<Pair<String, Float>>?) {
                            if (isMonitoring) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    if (poseLabels != null && personScore != null && personScore > 0.3) {
                                        handlePoseDetection(poseLabels)
                                    } else {
                                        handleMissingPose()
                                    }
                                }
                            }
                        }
                    }).apply {
                        prepareCamera()
                    }
                    isPoseClassifier()
                    withContext(Dispatchers.Main) {
                        cameraSource?.initCamera()
                    }
                }
                createPoseEstimator()
            }
        }
    }

    private fun handlePoseDetection(poseLabels: List<Pair<String, Float>>) {
        val sortedLabels = poseLabels.sortedByDescending { it.second }
        val topLabel = sortedLabels[0].first

        lifecycleScope.launch(Dispatchers.Main) {
            when (topLabel) {
                "forwardhead" -> {
                    forwardheadCounter++
                    crosslegCounter = 0
                    standardCounter = 0
                    tvDebug.text = getString(R.string.tfe_pe_tv_debug, "$topLabel $forwardheadCounter")
                    if (forwardheadCounter > 60) {
                        forwardheadCount++
                        forwardheadCounter = 0
                        tvWordReminder.text = getWordReminderText("forwardhead_confirm")
                        if (forwardheadPlayerFlag) {
                            forwardheadPlayer.start()
                            forwardheadPlayerFlag = false
                            delay(30000)
                            forwardheadPlayerFlag = true
                        }
                    } else if (forwardheadCounter > 30) {
                        tvWordReminder.text = getWordReminderText("forwardhead_suspect")
                    }
                }

                "crossleg" -> {
                    forwardheadCounter = 0
                    crosslegCounter++
                    standardCounter = 0
                    tvDebug.text = getString(R.string.tfe_pe_tv_debug, "$topLabel $crosslegCounter")
                    if (crosslegCounter > 60) {
                        crosslegCount++
                        crosslegCounter = 0
                        tvWordReminder.text = getWordReminderText("crossleg_confirm")
                        if (crosslegPlayerFlag) {
                            crosslegPlayer.start()
                            crosslegPlayerFlag = false
                            delay(30000)
                            crosslegPlayerFlag = true
                        }
                    } else if (crosslegCounter > 30) {
                        tvWordReminder.text = getWordReminderText("crossleg_suspect")
                    }
                }

                else -> {
                    forwardheadCounter = 0
                    crosslegCounter = 0
                    standardCounter++
                    tvDebug.text = getString(R.string.tfe_pe_tv_debug, "$topLabel $standardCounter")
                    if (standardCounter > 30) {
                        standardCount++
                        standardCounter = 0
                        if (standardPlayerFlag) {
                            standardPlayer.start()
                            standardPlayerFlag = false
                        }
                    }
                    tvWordReminder.text = getWordReminderText("standard")
                }
            }
        }
    }

    private fun handleMissingPose() {
        missingCounter++
        if (missingCounter > 30) {
            tvWordReminder.text = getWordReminderText("no_target")
        }
        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "missing $missingCounter")
    }

    private fun closeCamera() {
        cameraSource?.close()
        cameraSource = null
    }

    private fun isPoseClassifier() {
        cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(requireContext()) else null)
    }

    private fun initSpinner() {
        val cameraSpinner = binding.spnCamera
        val cameraOptions = resources.getStringArray(R.array.tfe_pe_camera_name)
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, cameraOptions)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        cameraSpinner.adapter = adapter
        cameraSpinner.onItemSelectedListener = changeCameraListener
    }

    private fun changeCamera(direction: Int) {
        val targetCamera = when (direction) {
            0 -> Camera.BACK
            else -> Camera.FRONT
        }
        if (selectedCamera == targetCamera) return
        selectedCamera = targetCamera

        cameraSource?.close()
        cameraSource = null
        openCamera()
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(requireContext(), device, ModelType.Thunder)
        poseDetector.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }

    private fun startPauseMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            startTime = System.currentTimeMillis()
            btnStartPause.text = getString(R.string.pause_monitoring)

            btnReset.visibility = View.VISIBLE
            btnSave.visibility = View.VISIBLE
            btnReset.isEnabled = true
            btnSave.isEnabled = true

            tvWordReminder.visibility = View.VISIBLE

            lifecycleScope.launch(Dispatchers.IO) {
                var badPostureStartTime = 0L
                var reminderInterval = 30000L

                while (isMonitoring) {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val minutes = (elapsedTime / 1000) / 60
                    val seconds = (elapsedTime / 1000) % 60

                    withContext(Dispatchers.Main) {
                        tvTimeMonitoring.text = getString(R.string.time_format, minutes, seconds)
                    }

                    if (isBadPostureDetected()) {
                        if (badPostureStartTime == 0L) {
                            badPostureStartTime = System.currentTimeMillis()
                        } else {
                            val badPostureDuration = System.currentTimeMillis() - badPostureStartTime
                            if (badPostureDuration >= reminderInterval) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(requireContext(), getString(R.string.please_correct_posture), Toast.LENGTH_SHORT).show()
                                    playReminderSound()
                                }
                                reminderInterval = maxOf(10000L, reminderInterval - 10000L)
                                badPostureStartTime = System.currentTimeMillis()
                            }
                        }
                    } else {
                        badPostureStartTime = 0L
                        reminderInterval = 30000L
                    }

                    delay(1000)
                }
            }
        } else {
            isMonitoring = false
            btnStartPause.text = getString(R.string.continue_monitoring)
            btnReset.isEnabled = true
            btnSave.isEnabled = true
        }
    }

    private fun isBadPostureDetected(): Boolean {
        return forwardheadCounter > 0 || crosslegCounter > 0
    }

    private fun initializeMediaPlayers() {
        forwardheadPlayer = createMediaPlayer(R.raw.forwardhead_en, R.raw.forwardhead_cn, R.raw.forwardhead_my)
        crosslegPlayer = createMediaPlayer(R.raw.crossleg_en, R.raw.crossleg_cn, R.raw.crossleg_my)
        standardPlayer = createMediaPlayer(R.raw.standard_en, R.raw.standard_cn, R.raw.standard_my)
    }

    private fun createMediaPlayer(defaultResId: Int, cnResId: Int, myResId: Int): MediaPlayer {
        val resId = when (selectedLanguage) {
            "cn" -> cnResId
            "my" -> myResId
            else -> defaultResId
        }
        return MediaPlayer.create(requireContext(), resId)
    }

    private fun releaseMediaPlayer() {
        forwardheadPlayer.release()
        crosslegPlayer.release()
        standardPlayer.release()
    }

    private fun playReminderSound() {
        val soundResId = when {
            forwardheadCounter > 0 -> getSoundResId(R.raw.forwardhead_en, R.raw.forwardhead_cn, R.raw.forwardhead_my)
            crosslegCounter > 0 -> getSoundResId(R.raw.crossleg_en, R.raw.crossleg_cn, R.raw.crossleg_my)
            else -> getSoundResId(R.raw.standard_en, R.raw.standard_cn, R.raw.standard_my)
        }

        MediaPlayer().apply {
            setDataSource(requireContext(), Uri.parse("android.resource://${requireContext().packageName}/$soundResId"))
            prepare()
            start()
            setOnCompletionListener {
                it.release()
            }
        }
    }

    private fun getSoundResId(defaultResId: Int, cnResId: Int, myResId: Int): Int {
        return when (selectedLanguage) {
            "cn" -> cnResId
            "my" -> myResId
            else -> defaultResId
        }
    }

    private fun resetMonitoring() {
        isMonitoring = false
        btnStartPause.text = getString(R.string.start_monitoring)
        btnReset.isEnabled = false
        btnSave.isEnabled = false
        startTime = 0
        tvTimeMonitoring.text = "Time: 00:00"
    }

    private fun saveMonitoringData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sessionId = "session_${System.currentTimeMillis()}"

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startTimeFormatted = dateFormat.format(Date(startTime))
            val endTimeFormatted = dateFormat.format(Date(System.currentTimeMillis()))

            val badPostureCount = forwardheadCount + crosslegCount

            dbHelper.insertPostureData(
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                sessionId = sessionId,
                startTime = startTimeFormatted,
                endTime = endTimeFormatted,
                badPostureCount = badPostureCount,
                forwardheadCount = forwardheadCount,
                crosslegCount = crosslegCount,
                standardCount = standardCount
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), getString(R.string.data_saved_successfully), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWordReminder() {
        val wordReminderText = when {
            forwardheadCounter > 0 -> getWordReminderText("forwardhead")
            crosslegCounter > 0 -> getWordReminderText("crossleg")
            else -> getWordReminderText("standard")
        }
        if (binding.tvWordReminder.text != wordReminderText) {
            binding.tvWordReminder.text = wordReminderText
        }
    }

    private fun getWordReminderText(postureType: String): String {
        return when (postureType) {
            "forwardhead_confirm" -> when (selectedLanguage) {
                "cn" -> "请别伸长脖子！"
                "my" -> "Jangan memanjangkan leher!"
                else -> "Don’t stretch your neck!"
            }
            "forwardhead_suspect" -> when (selectedLanguage) {
                "cn" -> "注意，你在伸脖子?"
                "my" -> "Hati-hati, kamu memanjangkan leher."
                else -> "Be careful, you’re stretching neck."
            }
            "crossleg_confirm" -> when (selectedLanguage) {
                "cn" -> "请别翘二郎腿!"
                "my" -> "Jangan bersilang kaki!"
                else -> "Don’t cross your legs!"
            }
            "crossleg_suspect" -> when (selectedLanguage) {
                "cn" -> "注意，你在翘二郎腿。"
                "my" -> "Perhatikan, kamu bersilang kaki."
                else -> "Be careful, you’re crossing legs."
            }
            "standard" -> when (selectedLanguage) {
                "cn" -> "不错，这家伙姿势不错"
                "my" -> "Bagus, teruskan!"
                else -> "Good, keep it up!"
            }
            else -> when (selectedLanguage) {
                "cn" -> "你在哪里呢？"
                "my" -> "Di manakah kamu?"
                else -> "Where are you?"
            }
        }
    }

    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .create()

        companion object {
            private const val ARG_MESSAGE = "message"

            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
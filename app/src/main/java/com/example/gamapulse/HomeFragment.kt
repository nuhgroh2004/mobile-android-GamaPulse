package com.example.gamapulse

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.gamapulse.network.ApiClient
import com.github.mikephil.charting.BuildConfig
import com.example.gamapulse.model.MahasiswaRoleResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.*
import java.lang.reflect.Array.set
import java.util.Calendar

class HomeFragment : Fragment() {
    private val PREFS_NAME = "MoodPrefs"
    private val KEY_LAST_MOOD_DATE = "LAST_MOOD_DATE"
    private val KEY_LAST_MOOD_TYPE = "LAST_MOOD_TYPE"
    private val KEY_LAST_MOOD_INTENSITY = "LAST_MOOD_INTENSITY"
    private val KEY_LAST_MOOD_NOTE = "LAST_MOOD_NOTE"
    private val KEY_TEMP_NAVIGATING = "TEMP_NAVIGATING_TO_NOTES"
    private val KEY_TEMP_MOOD_TYPE = "TEMP_MOOD_TYPE"
    private val KEY_TEMP_MOOD_INTENSITY = "TEMP_MOOD_INTENSITY"
    private lateinit var usernameLayout: TextView
    private var authToken: String? = null
    private var moodSelectionContainer: LinearLayout? = null
    private var moodParentContainer: LinearLayout? = null
    private var devRefreshButton: Button? = null
    private var mahasiswaId: Int = 0
    private var minIntensity: Int = 1
    private var maxIntensity: Int = 5
    private var midnightRefreshJob: Job? = null

    /* ----------------------------- Fragment Lifecycle Methods ----------------------------- */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        moodParentContainer = view.findViewById<LinearLayout>(R.id.mood_container)?.parent as? LinearLayout
        moodSelectionContainer = view.findViewById(R.id.mood_container)
        devRefreshButton = view.findViewById(R.id.dev_refresh_button)
        usernameLayout = view.findViewById(R.id.usernameLayout)

        // Set initial visibility for current mood card
        view.findViewById<View>(R.id.current_mood_card).visibility = View.GONE

        // Fetch data and setup UI
        getAuthToken()
        fetchUserProfile()
        setupMoodEmojis(view)
        setupTaskLogButton(view)
        setupProfileButton(view)
        setupDevRefreshButton()
        checkMoodSelectionStatus()
        updateMoodDisplay(view)

        // Start countdown when current mood card becomes visible
        val currentMoodCard = view.findViewById<View>(R.id.current_mood_card)
        currentMoodCard.viewTreeObserver.addOnGlobalLayoutListener {
            if (currentMoodCard.visibility == View.VISIBLE) {
                Log.d("HomeFragment", "current_mood_card is visible. Starting countdown.")
                startCountdownToRefresh()
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAuthToken()
        fetchUserProfile()
        fetchNotifications()
        checkTodayMood()
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        val isTemporarilyNavigating = sharedPref.getBoolean(KEY_TEMP_NAVIGATING, false)
        val lastMoodDate = sharedPref.getString(KEY_LAST_MOOD_DATE, "")
        val currentDate = getCurrentDate()
        if (isTemporarilyNavigating) {
            with(sharedPref.edit()) {
                putBoolean(KEY_TEMP_NAVIGATING, false)
                apply()
            }
        }
        if (lastMoodDate == currentDate) {
            moodSelectionContainer?.visibility = View.GONE
            moodParentContainer?.visibility = View.GONE
            view?.findViewById<View>(R.id.current_mood_card)?.visibility = View.VISIBLE
        } else {
            moodSelectionContainer?.visibility = View.VISIBLE
            moodParentContainer?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.current_mood_card)?.visibility = View.GONE
            if (!lastMoodDate.isNullOrEmpty() && lastMoodDate != currentDate) {
                with(sharedPref.edit()) {
                    putString(KEY_LAST_MOOD_DATE, "")
                    apply()
                }
            }
        }
        updateMoodDisplay(requireView())
        fetchNotifications()
    }
    /* ----------------------------- End Fragment Lifecycle Methods ----------------------------- */

    /* ----------------------------- Fungsi refres 24 jam  ----------------------------- */
    private fun startCountdownToRefresh() {
        if (devRefreshButton == null) {
            Log.e("HomeFragment", "devRefreshButton is null. Countdown cannot proceed.")
            return
        }
        midnightRefreshJob?.cancel()
        midnightRefreshJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val currentTime = Calendar.getInstance()
                val targetTime = Calendar.getInstance().apply {
                    if (get(Calendar.HOUR_OF_DAY) != 0 || get(Calendar.MINUTE) != 0) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val delayMs = targetTime.timeInMillis - currentTime.timeInMillis
                Log.d("HomeFragment", "Scheduled refresh at midnight. Waiting for ${delayMs/1000} seconds")
                delay(delayMs)
                Log.d("HomeFragment", "Midnight reached. Simulating button press.")
                devRefreshButton?.performClick()
                delay(1000)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        midnightRefreshJob?.cancel()
    }
    /* ----------------------------- End Fungsi refres 24 jam  ----------------------------- */

    /* ----------------------------- GET Username login ----------------------------- */
    private fun getAuthToken() {
        val sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", AppCompatActivity.MODE_PRIVATE)
        authToken = sharedPreferences.getString("token", "")
    }
    private fun fetchUserProfile() {
        if (authToken.isNullOrEmpty()) {
            return
        }
        val loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Memuat profil..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()
        val bearerToken = "Bearer $authToken"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getProfile(bearerToken)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()
                    if (response.isSuccessful && response.body() != null) {
                        val profileData = response.body()!!
                        usernameLayout.alpha = 0f
                        usernameLayout.visibility = View.VISIBLE
                        usernameLayout.text = "HALO ${profileData.user.name}"
                        usernameLayout.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                        mahasiswaId = profileData.mahasiswa.mahasiswa_id
                        fetchMahasiswaRole(bearerToken, mahasiswaId)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()

                    Toast.makeText(requireContext(), "Gagal memuat profil: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    /* ----------------------------- GET Username login ----------------------------- */

    /* ----------------------------- Developer Mode Methods ----------------------------- */
    private fun setupDevRefreshButton() {
        if (BuildConfig.DEBUG) {
            devRefreshButton?.visibility = View.VISIBLE
            devRefreshButton?.setTextColor(requireContext().getColor(R.color.white))
        }
        devRefreshButton?.setOnClickListener {
            animateButtonAndExecute(it) {
                val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    remove(KEY_LAST_MOOD_DATE)
                    remove(KEY_LAST_MOOD_TYPE)
                    remove(KEY_LAST_MOOD_INTENSITY)
                    remove(KEY_LAST_MOOD_NOTE)
                    remove(KEY_TEMP_NAVIGATING)
                    apply()
                }
                moodSelectionContainer?.visibility = View.VISIBLE
                moodParentContainer?.visibility = View.VISIBLE
                checkMoodSelectionStatus()
                updateMoodDisplay(requireView())
                showMessageToast("Mood selection reset for testing")
            }
        }
    }
    private fun showMessageToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    /* ----------------------------- End Developer Mode Methods ----------------------------- */

    /* ----------------------------- Mood Selection Status ----------------------------- */
    private fun checkMoodSelectionStatus() {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        val lastMoodDate = sharedPref.getString(KEY_LAST_MOOD_DATE, "")
        val currentDate = getCurrentDate()
        if (lastMoodDate.isNullOrEmpty() || lastMoodDate != currentDate) {
            moodParentContainer?.visibility = View.VISIBLE
        } else {
            moodParentContainer?.visibility = View.GONE
        }
    }
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
    /* ----------------------------- End Mood Selection Status ----------------------------- */

    /* ----------------------------- Emoji Setup Methods ----------------------------- */
    private fun setupMoodEmojis(view: View) {
        val moodContainer = view.findViewById<LinearLayout>(R.id.mood_container)
        val marahContainer = moodContainer.getChildAt(0) as LinearLayout
        val sedihContainer = moodContainer.getChildAt(1) as LinearLayout
        val bahagiaContainer = moodContainer.getChildAt(2) as LinearLayout
        val biasaContainer = moodContainer.getChildAt(3) as LinearLayout
        marahContainer.id = View.generateViewId()
        sedihContainer.id = View.generateViewId()
        bahagiaContainer.id = View.generateViewId()
        biasaContainer.id = View.generateViewId()
        val marahEmoji = marahContainer.getChildAt(0) as ImageView
        val sedihEmoji = sedihContainer.getChildAt(0) as ImageView
        val bahagiaEmoji = bahagiaContainer.getChildAt(0) as ImageView
        val biasaEmoji = biasaContainer.getChildAt(0) as ImageView
        marahEmoji.id = View.generateViewId()
        sedihEmoji.id = View.generateViewId()
        bahagiaEmoji.id = View.generateViewId()
        biasaEmoji.id = View.generateViewId()
        setupEmojiClickListener(marahEmoji, "Marah")
        setupEmojiClickListener(sedihEmoji, "Sedih")
        setupEmojiClickListener(bahagiaEmoji, "Bahagia")
        setupEmojiClickListener(biasaEmoji, "Biasa")
    }
    private fun setupEmojiClickListener(emojiView: ImageView, moodType: String) {
        emojiView.setOnClickListener {
            animateEmoji(emojiView)
            showMoodRatingPopup(moodType)
        }
    }
    /* ----------------------------- End Emoji Setup Methods ----------------------------- */

    /* ----------------------------- Mood Display Methods ----------------------------- */
    private fun updateMoodDisplay(view: View) {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        val moodType = sharedPref.getString(KEY_LAST_MOOD_TYPE, null)
        val moodIntensity = sharedPref.getInt(KEY_LAST_MOOD_INTENSITY, 0)
        val currentMoodContainer = view.findViewById<LinearLayout>(R.id.current_mood_container)
        val currentMoodCard = view.findViewById<View>(R.id.current_mood_card)
        if (moodType == null || moodIntensity == 0) {
            currentMoodCard.visibility = View.GONE
            return
        }
        currentMoodCard.visibility = View.VISIBLE
        val moodEmoji = view.findViewById<ImageView>(R.id.current_mood_emoji)
        val emojiResource = when (moodType) {
            "Marah" -> R.drawable.icon_mood_marah
            "Sedih" -> R.drawable.icon_mood_sedih
            "Bahagia" -> R.drawable.icon_mood_bahagia
            else -> R.drawable.icon_mood_biasa
        }
        moodEmoji.setImageResource(emojiResource)
        val moodText = view.findViewById<TextView>(R.id.current_mood_text)
        moodText.text = "Saya merasa $moodType dengan intensitas $moodIntensity pada hari ini."
        val moodNote = sharedPref.getString(KEY_LAST_MOOD_NOTE, "")
        if (!moodNote.isNullOrEmpty()) {
            val noteTextView = view.findViewById<TextView>(R.id.current_mood_note)
            noteTextView?.text = moodNote
            noteTextView?.visibility = View.VISIBLE
        }
    }
    /* ----------------------------- End Mood Display Methods ----------------------------- */

    /* ----------------------------- Animation Methods ----------------------------- */
    private fun animateEmoji(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.3f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 500
        animatorSet.interpolator = OvershootInterpolator()
        animatorSet.start()
    }
    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            view.postDelayed({
                action()
            }, 150)
        }.start()
    }
    /* ----------------------------- End Animation Methods ----------------------------- */

    /* ----------------------------- UI Helper Methods ----------------------------- */
    private fun getRippleDrawable(color: Int): RippleDrawable {
        return RippleDrawable(
            ColorStateList.valueOf(requireContext().getColor(R.color.ripple_color)),
            null,
            ColorDrawable(color)
        )
    }
    /* ----------------------------- End UI Helper Methods ----------------------------- */

    /* ----------------------------- Popup Dialog Methods ----------------------------- */
    private fun showMoodRatingPopup(moodType: String) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.popup_intensitas_mood, null)
        val titleTextView = popupView.findViewById<TextView>(R.id.popup_title)
        val numberPickerValue = popupView.findViewById<TextView>(R.id.number_value)
        val increaseButton = popupView.findViewById<View>(R.id.increase_button)
        val decreaseButton = popupView.findViewById<View>(R.id.decrease_button)
        val cancelButton = popupView.findViewById<Button>(R.id.cancel_button)
        val okButton = popupView.findViewById<Button>(R.id.ok_button)
        titleTextView.text = "Seberapa $moodType kamu?"
        var currentValue = minIntensity
        numberPickerValue.text = currentValue.toString()
        numberPickerValue.text = currentValue.toString()
        cancelButton.foreground = getRippleDrawable(requireContext().getColor(R.color.teal))
        okButton.foreground = getRippleDrawable(requireContext().getColor(R.color.teal))
        titleTextView.text = "Seberapa $moodType kamu?\ndari ($minIntensity - $maxIntensity)"
        increaseButton.setOnClickListener {
            if (currentValue < maxIntensity) {
                currentValue++
                numberPickerValue.text = currentValue.toString()
            }
        }
        decreaseButton.setOnClickListener {
            if (currentValue > minIntensity) {
                currentValue--
                numberPickerValue.text = currentValue.toString()
            }
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(popupView)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        cancelButton.setOnClickListener {
            animateButtonAndExecute(cancelButton) {
                dialog.dismiss()
            }
        }
        okButton.setOnClickListener {
            animateButtonAndExecute(okButton) {
                saveMoodRating(moodType, currentValue)
                dialog.dismiss()
                val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean(KEY_TEMP_NAVIGATING, true)
                    apply()
                }
                moodSelectionContainer?.visibility = View.GONE
                val intent = Intent(requireContext(), Notes::class.java)
                intent.putExtra("MOOD_TYPE", moodType)
                intent.putExtra("MOOD_INTENSITY", currentValue)
                startActivity(intent)
            }
        }
        dialog.show()
    }
    /* ----------------------------- End Popup Dialog Methods ----------------------------- */

    /* ----------------------------- Data Methods ----------------------------- */
    private fun saveMoodRating(moodType: String, rating: Int) {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_TEMP_MOOD_TYPE, moodType)
            putInt(KEY_TEMP_MOOD_INTENSITY, rating)
            putBoolean(KEY_TEMP_NAVIGATING, true)
            apply()
        }
        checkMoodSelectionStatus()
        println("Mood: $moodType, Rating: $rating")
    }
    /* ----------------------------- End Data Methods ----------------------------- */

    /* ----------------------------- Navigation ----------------------------- */
    private fun setupTaskLogButton(view: View) {
        view.findViewById<View>(R.id.btnTaksLog)?.setOnClickListener {
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), TaskLogActivity::class.java)
                startActivity(intent)
            }
        }
        view.findViewById<View>(R.id.btnViewCalendar)?.setOnClickListener {
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), ViewCalendarActivity::class.java)
                startActivity(intent)
            }
        }
    }
    /* ----------------------------- End Navigation ----------------------------- */

    /* ----------------------------- Profile Navigation ----------------------------- */
    private fun setupProfileButton(view: View) {
        val profileButton = view.findViewById<ImageView>(R.id.btn_profil)
        profileButton.setOnClickListener {
            animateButtonAndExecute(it) {
                val intent = Intent(requireContext(), ProfilActivity::class.java)
                startActivity(intent)
            }
        }
    }
    /* ----------------------------- End Profile Navigation ----------------------------- */

    /* ----------------------------- Fetch Notifications ----------------------------- */
    private fun fetchNotifications() {
        if (authToken.isNullOrEmpty()) {
            return
        }
        val bearerToken = "Bearer $authToken"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getNotifications(bearerToken)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val notificationResponse = response.body()!!
                        val unreadCount = notificationResponse.unread_notifications?.size ?: 0
                        if (activity is MainActivity) {
                            (activity as MainActivity).updateNotificationBadge(unreadCount)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to fetch notifications: ${e.message}", e)
            }
        }
    }
    /* ----------------------------- End Fetch Notifications ----------------------------- */

    /* ----------------------------- Fetch Mahasiswa Role ----------------------------- */
    private fun fetchMahasiswaRole(token: String, id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getMahasiswaRole(token, id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val roleData = response.body()!!
                        minIntensity = roleData.role.min_intensity
                        maxIntensity = roleData.role.max_intensity
                        Log.d("HomeFragment", "Intensity range: $minIntensity-$maxIntensity")
                    } else {
                        Log.e("HomeFragment", "Failed to get role data: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching role: ${e.message}", e)
            }
        }
    }
    /* ----------------------------- End Fetch Mahasiswa Role ----------------------------- */

    /* ---------------------------- cek tampilan mood tracker ----------------------------- */
    private fun checkTodayMood() {
        if (authToken.isNullOrEmpty()) {
            return
        }
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val bearerToken = "Bearer $authToken"
        val loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Memeriksa data mood..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getMoodNotes(bearerToken, day, month, year)
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()
                    if (response.isSuccessful && response.body() != null) {
                        val moodData = response.body()!!
                        if (moodData.mood != null) {
                            moodSelectionContainer?.visibility = View.GONE
                            moodParentContainer?.visibility = View.GONE
                            view?.findViewById<View>(R.id.current_mood_card)?.visibility = View.VISIBLE
                            val moodType = when(moodData.mood.mood_level) {
                                1 -> "Marah"
                                2 -> "Sedih"
                                3 -> "Biasa"
                                4 -> "Bahagia"
                                else -> "Biasa"
                            }
                            val moodIntensity = moodData.mood.mood_intensity
                            val moodNote = moodData.mood.mood_note ?: ""
                            val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString(KEY_LAST_MOOD_DATE, getCurrentDate())
                                putString(KEY_LAST_MOOD_TYPE, moodType)
                                putInt(KEY_LAST_MOOD_INTENSITY, moodIntensity)
                                putString(KEY_LAST_MOOD_NOTE, moodNote)
                                apply()
                            }
                            updateMoodDisplay(requireView())
                        } else {
                            moodSelectionContainer?.visibility = View.VISIBLE
                            moodParentContainer?.visibility = View.VISIBLE
                            view?.findViewById<View>(R.id.current_mood_card)?.visibility = View.GONE
                        }
                    } else {
                        moodSelectionContainer?.visibility = View.VISIBLE
                        moodParentContainer?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.current_mood_card)?.visibility = View.GONE
                        Log.e("HomeFragment", "API Error: ${response.code()} - ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()
                    moodSelectionContainer?.visibility = View.VISIBLE
                    moodParentContainer?.visibility = View.VISIBLE
                    view?.findViewById<View>(R.id.current_mood_card)?.visibility = View.GONE
                    Log.e("HomeFragment", "Error checking today's mood: ${e.message}")
                }
            }
        }
    }
    /* ----------------------------- End cek tampilan mood tracker ----------------------------- */

}
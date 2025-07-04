package com.example.gamapulse

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.gamapulse.model.NotificationActionRequest
import com.example.gamapulse.model.NotificationItem
import com.example.gamapulse.network.ApiClient
import kotlinx.coroutines.launch

class NotificationFragment : Fragment(), NotificationAdapter.NotificationActionListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabKotakMasuk: TextView
    private lateinit var tabRiwayat: TextView
    private lateinit var inboxAdapter: NotificationAdapter
    private lateinit var historyAdapter: NotificationAdapter
    private val inboxNotifications = mutableListOf<NotificationItem>()
    private val historyNotifications = mutableListOf<NotificationItem>()
    private var isInboxActive = true
    private lateinit var loadingDialog: SweetAlertDialog
    private var isReturningFromProfile = false
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    /* ----------------------------- onCreateView ----------------------------- */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }
    /* ----------------------------- onCreateView ----------------------------- */

    /* ----------------------------- onViewCreated ----------------------------- */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.rvNotifications)
        tabKotakMasuk = view.findViewById(R.id.tab_kotak_masuk)
        tabRiwayat = view.findViewById(R.id.tab_riwayat)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        setupProfileButton(view)
        setupTabs()
        setupSwipeRefresh()
        setupNotifications()
    }
    /* ----------------------------- onViewCreated ----------------------------- */

    /* ----------------------------- setupProfileButton ----------------------------- */
    private fun setupProfileButton(view: View) {
        val profileButton = view.findViewById<ImageView>(R.id.btn_profil)
        profileButton.setOnClickListener {
            animateButtonAndExecute(it) {
                isReturningFromProfile = true
                val intent = Intent(requireContext(), ProfilActivity::class.java)
                startActivity(intent)
            }
        }
    }
    /* ----------------------------- setupProfileButton ----------------------------- */

    /* ----------------------------- setupSwipeRefresh ----------------------------- */
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            refreshNotificationsWithoutLoadingDialog()
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.teal_200,
            R.color.blue_500
        )
    }
    /* ----------------------------- setupSwipeRefresh ----------------------------- */

    /* ----------------------------- animateButtonAndExecute ----------------------------- */
    private fun animateButtonAndExecute(view: View, action: () -> Unit) {
        val originalElevation = view.elevation
        view.elevation = 0f

        view.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .withEndAction {
                        view.elevation = originalElevation
                        action()
                    }
                    .start()
            }
            .start()
    }
    /* ----------------------------- animateButtonAndExecute ----------------------------- */

    /* ----------------------------- setupNotifications ----------------------------- */
    private fun setupNotifications() {
        loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Memuat notifikasi..."
        loadingDialog.setCancelable(false)
        setupRecyclerView()
        fetchNotifications()
    }
    /* ----------------------------- setupNotifications ----------------------------- */

    /* ----------------------------- fetchNotifications ----------------------------- */
    private fun fetchNotifications() {
        try {
            if (!isAdded || context == null) return

            loadingDialog.show()
            lifecycleScope.launch {
                try {
                    val sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                    val token = sharedPreferences.getString("token", null)
                    if (token == null) {
                        if (isAdded && context != null && loadingDialog.isShowing) {
                            loadingDialog.dismissWithAnimation()
                        }
                        showErrorDialog("Token autentikasi tidak ditemukan")
                        return@launch
                    }

                    Log.d("NotificationFragment", "Mengambil notifikasi dengan token: ${token.take(10)}...")
                    val response = ApiClient.apiService.getNotifications("Bearer $token")

                    if (isAdded && context != null && loadingDialog.isShowing) {
                        loadingDialog.dismissWithAnimation()
                    }

                    if (response.isSuccessful) {
                        val notificationResponse = response.body()
                        val unreadNotifications = notificationResponse?.unread_notifications ?: emptyList()
                        val historyNotifs = notificationResponse?.history_notifications ?: emptyList()

                        Log.d("NotificationFragment", "Notifikasi belum dibaca: ${unreadNotifications.size}")
                        Log.d("NotificationFragment", "Riwayat notifikasi: ${historyNotifs.size}")

                        val inbox = unreadNotifications.map { notification ->
                            NotificationItem(
                                id = notification.notification_id,
                                sender = notification.name,
                                message = "${notification.name} ingin melihat laporan Anda",
                                email = notification.email,
                                status = NotificationStatus.PENDING
                            )
                        }

                        val history = historyNotifs.map { notification ->
                            val status = when (notification.request_status.lowercase()) {
                                "accepted", "allowed" -> NotificationStatus.ALLOWED
                                "rejected" -> NotificationStatus.REJECTED
                                else -> NotificationStatus.PENDING
                            }
                            NotificationItem(
                                id = notification.notification_id,
                                sender = notification.name,
                                message = "${notification.name} ingin melihat laporan Anda",
                                email = notification.email,
                                status = status
                            )
                        }

                        inboxNotifications.clear()
                        inboxNotifications.addAll(inbox)
                        inboxAdapter.updateNotifications(inboxNotifications)

                        historyNotifications.clear()
                        historyNotifications.addAll(history)
                        historyAdapter.updateNotifications(historyNotifications)
                        updateBadgeCount(unreadNotifications.size)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("NotificationFragment", "Error: ${response.code()} - $errorBody")
                        showErrorDialog("Gagal memuat notifikasi: ${response.message()}")
                    }
                } catch (e: Exception) {
                    if (isAdded && context != null && loadingDialog.isShowing) {
                        loadingDialog.dismissWithAnimation()
                    }
                    Log.e("NotificationFragment", "Exception: ${e.message}", e)
                    showErrorDialog("Terjadi kesalahan: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error starting fetch", e)
        }
    }
    /* ----------------------------- fetchNotifications ----------------------------- */

    /* ----------------------------- showErrorDialog ----------------------------- */
    private fun showErrorDialog(message: String) {
        if (!isAdded || context == null) return

        try {
            SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Error")
                .setContentText(message)
                .setConfirmText("OK")
                .setConfirmClickListener { it.dismissWithAnimation() }
                .show()
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error showing dialog: ${e.message}", e)
        }
    }
    /* ----------------------------- showErrorDialog ----------------------------- */

    /* ----------------------------- refreshNotifications ----------------------------- */
    private fun refreshNotifications() {
        fetchNotifications()
    }
    /* ----------------------------- refreshNotifications ----------------------------- */

    /* ----------------------------- onResume ----------------------------- */
    override fun onResume() {
        super.onResume()

        try {
            if (isReturningFromProfile) {
                isReturningFromProfile = false
                view?.postDelayed({
                    if (isAdded && context != null) {
                        refreshNotificationsWithoutLoadingDialog()
                    }
                }, 300)
            } else {
                if (isAdded && context != null) {
                    refreshNotifications()
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error in onResume: ${e.message}", e)
        }
    }
    /* ----------------------------- onResume ----------------------------- */

    /* ----------------------------- refreshNotificationsWithoutLoadingDialog ----------------------------- */
    private fun refreshNotificationsWithoutLoadingDialog() {
        lifecycleScope.launch {
            try {
                val sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)

                if (token == null) {
                    swipeRefreshLayout.isRefreshing = false
                    showErrorDialog("Token autentikasi tidak ditemukan")
                    return@launch
                }

                val response = ApiClient.apiService.getNotifications("Bearer $token")
                swipeRefreshLayout.isRefreshing = false

                if (response.isSuccessful) {
                    val notificationResponse = response.body()
                    if (notificationResponse != null) {
                        inboxNotifications.clear()
                        historyNotifications.clear()

                        val unreadNotifications = notificationResponse.unread_notifications ?: emptyList()
                        val historyNotifs = notificationResponse.history_notifications ?: emptyList()

                        inboxNotifications.addAll(unreadNotifications.map { notification ->
                            NotificationItem(
                                id = notification.notification_id,
                                sender = notification.name,
                                message = "${notification.name} ingin melihat laporan Anda",
                                email = notification.email,
                                status = NotificationStatus.PENDING
                            )
                        })

                        historyNotifications.addAll(historyNotifs.map { notification ->
                            val status = when (notification.request_status.lowercase()) {
                                "accepted", "allowed" -> NotificationStatus.ALLOWED
                                "rejected" -> NotificationStatus.REJECTED
                                else -> NotificationStatus.PENDING
                            }
                            NotificationItem(
                                id = notification.notification_id,
                                sender = notification.name,
                                message = "${notification.name} ingin melihat laporan Anda",
                                email = notification.email,
                                status = status
                            )
                        })

                        inboxAdapter.updateNotifications(inboxNotifications)
                        historyAdapter.updateNotifications(historyNotifications)
                        updateBadgeCount(unreadNotifications.size)
                    } else {
                        showErrorDialog("Gagal memuat notifikasi: Respons kosong")
                    }
                } else {
                    showErrorDialog("Error: ${response.code()}")
                }
            } catch (e: Exception) {
                swipeRefreshLayout.isRefreshing = false
                Log.e("NotificationFragment", "Error refreshing notifications", e)
                showErrorDialog("Terjadi kesalahan: ${e.message}")
            }
        }
    }
    /* ----------------------------- refreshNotificationsWithoutLoadingDialog ----------------------------- */

    /* ----------------------------- updateBadgeCount ----------------------------- */
    private fun updateBadgeCount(count: Int) {
        if (activity is MainActivity) {
            (activity as MainActivity).updateNotificationBadge(count)
        }
    }
    /* ----------------------------- updateBadgeCount ----------------------------- */

    /* ----------------------------- setupTabs ----------------------------- */
    private fun setupTabs() {
        tabKotakMasuk.setOnClickListener {
            if (!isInboxActive) {
                switchToInbox()
            }
        }
        tabRiwayat.setOnClickListener {
            if (isInboxActive) {
                switchToHistory()
            }
        }
    }
    /* ----------------------------- setupTabs ----------------------------- */

    /* ----------------------------- switchToInbox ----------------------------- */
    private fun switchToInbox() {
        tabKotakMasuk.setBackgroundResource(R.drawable.report_selected_tab_background)
        tabRiwayat.setBackgroundResource(R.drawable.report_unselected_tab_background)
        recyclerView.adapter = inboxAdapter
        isInboxActive = true
    }
    /* ----------------------------- switchToInbox ----------------------------- */

    /* ----------------------------- switchToHistory ----------------------------- */
    private fun switchToHistory() {
        tabRiwayat.setBackgroundResource(R.drawable.report_selected_tab_background)
        tabKotakMasuk.setBackgroundResource(R.drawable.report_unselected_tab_background)
        recyclerView.adapter = historyAdapter
        isInboxActive = false
    }
    /* ----------------------------- switchToHistory ----------------------------- */

    /* ----------------------------- setupRecyclerView ----------------------------- */
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        inboxAdapter = NotificationAdapter(inboxNotifications, object : NotificationAdapter.NotificationActionListener {
            override fun onAllowClick(notification: NotificationItem) {
                showConfirmationDialog(
                    "Konfirmasi Izinkan",
                    "Apakah Anda yakin ingin mengizinkan ${notification.sender} melihat laporan Anda?",
                    "Izinkan",
                    SweetAlertDialog.SUCCESS_TYPE
                ) {
                    processAllowNotification(notification)
                }
            }
            override fun onRejectClick(notification: NotificationItem) {
                showConfirmationDialog(
                    "Konfirmasi Tolak",
                    "Apakah Anda yakin ingin menolak permintaan dari ${notification.sender}?",
                    "Tolak",
                    SweetAlertDialog.WARNING_TYPE
                ) {
                    processRejectNotification(notification)
                }
            }
            override fun onDeleteClick(notification: NotificationItem) {
                showConfirmationDialog(
                    "Konfirmasi Hapus",
                    "Apakah Anda yakin ingin menghapus notifikasi ini?",
                    "Hapus",
                    SweetAlertDialog.WARNING_TYPE
                ) {
                    processDeleteNotification(notification)
                }
            }
        }, true)
        historyAdapter = NotificationAdapter(historyNotifications, this, false)
        recyclerView.adapter = inboxAdapter
    }
    /* ----------------------------- setupRecyclerView ----------------------------- */

    /* ----------------------------- showConfirmationDialog ----------------------------- */
    private fun showConfirmationDialog(
        title: String,
        message: String,
        confirmText: String,
        alertType: Int,
        onConfirm: () -> Unit
    ) {
        if (!isAdded || context == null) return

        try {
            val dialog = SweetAlertDialog(requireContext(), alertType)
                .setTitleText(title)
                .setContentText(message)
                .setCancelText("Batal")
                .setConfirmText(confirmText)
                .showCancelButton(true)
                .setConfirmClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                    onConfirm()
                }
                .setCancelClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                }
            dialog.show()
            val cancelButton = dialog.getButton(SweetAlertDialog.BUTTON_CANCEL)
            val confirmButton = dialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)
            cancelButton.apply {
                background = resources.getDrawable(R.drawable.allert_button_cancel, requireActivity().theme)
                setTextColor(Color.WHITE)
                setPadding(24, 12, 24, 12)
                minWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
                ).toInt()
                backgroundTintList = null
            }
            confirmButton.apply {
                background = resources.getDrawable(R.drawable.allert_button_confirm, requireActivity().theme)
                setTextColor(Color.WHITE)
                setPadding(24, 12, 24, 12)
                minWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
                ).toInt()
                backgroundTintList = null
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error showing dialog: ${e.message}", e)
        }
    }
    /* ----------------------------- showConfirmationDialog ----------------------------- */

    /* ----------------------------- showSuccessDialog ----------------------------- */
    private fun showSuccessDialog(message: String, onDismiss: (() -> Unit)? = null) {
        if (!isAdded || context == null) return

        try {
            val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Berhasil!")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .setCancelable(false)
            val dialog = dialogBuilder.create()
            dialog.show()
            val positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.apply {
                setTextColor(Color.WHITE)
                setBackgroundColor(resources.getColor(R.color.white, requireActivity().theme))
                setPadding(24, 12, 24, 12)
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error showing success dialog: ${e.message}", e)
        }
    }
    /* ----------------------------- showSuccessDialog ----------------------------- */

    /* ----------------------------- processAllowNotification ----------------------------- */
    private fun processAllowNotification(notification: NotificationItem) {
        if (!isAdded || context == null) return

        try {
            val loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            loadingDialog.titleText = "Mengizinkan permintaan..."
            loadingDialog.setCancelable(false)
            loadingDialog.show()

            lifecycleScope.launch {
                try {
                    val sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                    val token = sharedPreferences.getString("token", null)
                    if (token == null) {
                        if (isAdded && context != null && loadingDialog.isShowing) {
                            loadingDialog.dismissWithAnimation()
                        }
                        showErrorDialog("Token autentikasi tidak ditemukan")
                        return@launch
                    }

                    val request = NotificationActionRequest("approve")
                    val response = ApiClient.apiService.respondToNotification(
                        "Bearer $token",
                        notification.id,
                        request
                    )

                    if (isAdded && context != null && loadingDialog.isShowing) {
                        loadingDialog.dismissWithAnimation()
                    }

                    if (response.isSuccessful) {
                        if (isAdded && context != null) {
                            val successDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Berhasil!")
                                .setMessage("Permintaan telah diizinkan")
                                .setPositiveButton("OK", null)
                                .setCancelable(true)
                                .create()
                            successDialog.show()
                        }

                        refreshNotificationsWithoutLoadingDialog()
                        switchToHistory()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("NotificationFragment", "Error: ${response.code()} - $errorBody")
                        showErrorDialog("Gagal memproses permintaan: ${response.message()}")
                    }
                } catch (e: Exception) {
                    if (isAdded && context != null && loadingDialog.isShowing) {
                        loadingDialog.dismissWithAnimation()
                    }
                    Log.e("NotificationFragment", "Exception: ${e.message}", e)
                    showErrorDialog("Terjadi kesalahan: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error starting allow process: ${e.message}", e)
        }
    }
    /* ----------------------------- processAllowNotification ----------------------------- */

    /* ----------------------------- processRejectNotification ----------------------------- */
    private fun processRejectNotification(notification: NotificationItem) {
        if (!isAdded || context == null) return

        try {
            val loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            loadingDialog.titleText = "Menolak permintaan..."
            loadingDialog.setCancelable(false)
            loadingDialog.show()

            lifecycleScope.launch {
                try {
                    val sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                    val token = sharedPreferences.getString("token", null)
                    if (token == null) {
                        if (isAdded && context != null && loadingDialog.isShowing) {
                            loadingDialog.dismissWithAnimation()
                        }
                        showErrorDialog("Token autentikasi tidak ditemukan")
                        return@launch
                    }

                    val request = NotificationActionRequest("reject")
                    val response = ApiClient.apiService.respondToNotification(
                        "Bearer $token",
                        notification.id,
                        request
                    )

                    if (isAdded && context != null && loadingDialog.isShowing) {
                        loadingDialog.dismissWithAnimation()
                    }

                    if (response.isSuccessful) {
                        if (isAdded && context != null) {
                            val successDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Berhasil!")
                                .setMessage("Permintaan telah ditolak")
                                .setPositiveButton("OK", null)
                                .setCancelable(true)
                                .create()
                            successDialog.show()
                        }

                        refreshNotificationsWithoutLoadingDialog()
                        switchToHistory()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("NotificationFragment", "Error: ${response.code()} - $errorBody")
                        showErrorDialog("Gagal memproses permintaan: ${response.message()}")
                    }
                } catch (e: Exception) {
                    if (isAdded && context != null && loadingDialog.isShowing) {
                        loadingDialog.dismissWithAnimation()
                    }
                    Log.e("NotificationFragment", "Exception: ${e.message}", e)
                    showErrorDialog("Terjadi kesalahan: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationFragment", "Error starting reject process: ${e.message}", e)
        }
    }
    /* ----------------------------- processRejectNotification ----------------------------- */

    /* ----------------------------- processDeleteNotification ----------------------------- */
    private fun processDeleteNotification(notification: NotificationItem) {
        val index = historyNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            historyNotifications.removeAt(index)
            historyAdapter.updateNotifications(historyNotifications)
            showSuccessDialog("Notifikasi telah dihapus")
        }
    }
    /* ----------------------------- processDeleteNotification ----------------------------- */

    /* ----------------------------- onAllowClick ----------------------------- */
    override fun onAllowClick(notification: NotificationItem) {
        showConfirmationDialog(
            "Konfirmasi Izinkan",
            "Apakah Anda yakin ingin mengizinkan ${notification.sender} melihat laporan Anda?",
            "Izinkan",
            SweetAlertDialog.SUCCESS_TYPE
        ) {
            processAllowNotification(notification)
        }
    }
    /* ----------------------------- onAllowClick ----------------------------- */

    /* ----------------------------- onRejectClick ----------------------------- */
    override fun onRejectClick(notification: NotificationItem) {
        showConfirmationDialog(
            "Konfirmasi Tolak",
            "Apakah Anda yakin ingin menolak permintaan dari ${notification.sender}?",
            "Tolak",
            SweetAlertDialog.WARNING_TYPE
        ) {
            processRejectNotification(notification)
        }
    }
    /* ----------------------------- onRejectClick ----------------------------- */

    /* ----------------------------- onDeleteClick ----------------------------- */
    override fun onDeleteClick(notification: NotificationItem) {
        showConfirmationDialog(
            "Konfirmasi Hapus",
            "Apakah Anda yakin ingin menghapus notifikasi ini?",
            "Hapus",
            SweetAlertDialog.WARNING_TYPE
        ) {
            processDeleteNotification(notification)
        }
    }
    /* ----------------------------- onDeleteClick ----------------------------- */
}
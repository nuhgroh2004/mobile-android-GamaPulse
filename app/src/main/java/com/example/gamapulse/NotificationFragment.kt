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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvNotifications)
        tabKotakMasuk = view.findViewById(R.id.tab_kotak_masuk)
        tabRiwayat = view.findViewById(R.id.tab_riwayat)

        setupProfileButton(view)
        setupTabs()
        setupNotifications() // Ganti loadDummyData() dengan ini
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

    private fun setupNotifications() {
        // Setup loading dialog
        loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Memuat notifikasi..."
        loadingDialog.setCancelable(false)

        // Setup adapters first (empty lists)
        setupRecyclerView()

        // Fetch notifications
        fetchNotifications()
    }

    private fun fetchNotifications() {
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                val sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                val token = sharedPreferences.getString("token", null)

                if (token == null) {
                    loadingDialog.dismissWithAnimation()
                    showErrorDialog("Token autentikasi tidak ditemukan")
                    return@launch
                }

                Log.d("NotificationFragment", "Mengambil notifikasi dengan token: ${token.take(10)}...")
                val response = ApiClient.apiService.getNotifications("Bearer $token")
                loadingDialog.dismissWithAnimation()

                if (response.isSuccessful) {
                    val notificationResponse = response.body()

                    // Proses notifikasi belum dibaca (kotak masuk)
                    val unreadNotifications = notificationResponse?.unread_notifications ?: emptyList()
                    val historyNotifs = notificationResponse?.history_notifications ?: emptyList()

                    Log.d("NotificationFragment", "Notifikasi belum dibaca: ${unreadNotifications.size}")
                    Log.d("NotificationFragment", "Riwayat notifikasi: ${historyNotifs.size}")

                    // Konversi data API ke model UI
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

                    // Update adapters
                    inboxNotifications.clear()
                    inboxNotifications.addAll(inbox)
                    inboxAdapter.updateNotifications(inboxNotifications)

                    historyNotifications.clear()
                    historyNotifications.addAll(history)
                    historyAdapter.updateNotifications(historyNotifications)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("NotificationFragment", "Error: ${response.code()} - $errorBody")
                    showErrorDialog("Gagal memuat notifikasi: ${response.message()}")
                }
            } catch (e: Exception) {
                loadingDialog.dismissWithAnimation()
                Log.e("NotificationFragment", "Exception: ${e.message}", e)
                showErrorDialog("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    private fun showErrorDialog(message: String) {
        SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error")
            .setContentText(message)
            .setConfirmText("OK")
            .setConfirmClickListener { it.dismissWithAnimation() }
            .show()
    }

    private fun refreshNotifications() {
        fetchNotifications()
    }

    // Tambahkan fungsi ini ke onResume untuk memuat notifikasi setiap kali fragment muncul
    override fun onResume() {
        super.onResume()
        refreshNotifications()
    }

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

    private fun switchToInbox() {
        tabKotakMasuk.setBackgroundResource(R.drawable.report_selected_tab_background)
        tabRiwayat.setBackgroundResource(R.drawable.report_unselected_tab_background)

        recyclerView.adapter = inboxAdapter
        isInboxActive = true
    }

    private fun switchToHistory() {
        tabRiwayat.setBackgroundResource(R.drawable.report_selected_tab_background)
        tabKotakMasuk.setBackgroundResource(R.drawable.report_unselected_tab_background)

        recyclerView.adapter = historyAdapter
        isInboxActive = false
    }

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

    private fun showConfirmationDialog(
        title: String,
        message: String,
        confirmText: String,
        alertType: Int,
        onConfirm: () -> Unit
    ) {
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
    }

    private fun showSuccessDialog(message: String) {
        val successDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("Berhasil!")
            .setContentText(message)
            .setConfirmText("OK")

        successDialog.show()

        successDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)?.apply {
            background = resources.getDrawable(R.drawable.allert_button_ok, requireActivity().theme)
            setTextColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
            minWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 120f, resources.displayMetrics
            ).toInt()
            backgroundTintList = null
        }
    }

    private fun processAllowNotification(notification: NotificationItem) {
        val index = inboxNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            inboxNotifications.removeAt(index)
            inboxAdapter.updateNotifications(inboxNotifications)

            val updatedNotification = notification.copy(status = NotificationStatus.ALLOWED)
            historyNotifications.add(0, updatedNotification)
            historyAdapter.updateNotifications(historyNotifications)

            showSuccessDialog("Permintaan telah diizinkan")
        }
    }

    private fun processRejectNotification(notification: NotificationItem) {
        val index = inboxNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            inboxNotifications.removeAt(index)
            inboxAdapter.updateNotifications(inboxNotifications)

            val updatedNotification = notification.copy(status = NotificationStatus.REJECTED)
            historyNotifications.add(0, updatedNotification)
            historyAdapter.updateNotifications(historyNotifications)

            showSuccessDialog("Permintaan telah ditolak")
        }
    }

    private fun processDeleteNotification(notification: NotificationItem) {
        val index = historyNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            historyNotifications.removeAt(index)
            historyAdapter.updateNotifications(historyNotifications)

            showSuccessDialog("Notifikasi telah dihapus")
        }
    }

    private fun loadDummyData() {
        inboxNotifications.add(NotificationItem(1, "Budi", "Saudara Budi ingin melihat laporan anda", "Budi@gmail.com"))
        inboxNotifications.add(NotificationItem(2, "Ahmad", "Saudara Ahmad ingin melihat laporan anda", "Ahmad@gmail.com"))
        inboxNotifications.add(NotificationItem(3, "Dewi", "Saudara Dewi ingin melihat laporan anda", "Dewi@gmail.com"))
        inboxAdapter.updateNotifications(inboxNotifications)

        historyNotifications.add(NotificationItem(4, "Rini", "Saudara Rini ingin melihat laporan anda", "Rini@gmail.com", NotificationStatus.ALLOWED))
        historyNotifications.add(NotificationItem(5, "Joko", "Saudara Joko ingin melihat laporan anda", "Joko@gmail.com", NotificationStatus.REJECTED))
        historyAdapter.updateNotifications(historyNotifications)
    }

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
}
package com.example.gamapulse

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog

class NotificationFragment : Fragment(), NotificationAdapter.NotificationActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tabKotakMasuk: TextView
    private lateinit var tabRiwayat: TextView

    private lateinit var inboxAdapter: NotificationAdapter
    private lateinit var historyAdapter: NotificationAdapter

    private val inboxNotifications = mutableListOf<NotificationItem>()
    private val historyNotifications = mutableListOf<NotificationItem>()

    private var isInboxActive = true

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
        setupRecyclerView()
        loadDummyData()
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
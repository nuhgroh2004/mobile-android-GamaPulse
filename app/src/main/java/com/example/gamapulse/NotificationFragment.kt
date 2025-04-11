package com.example.gamapulse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
    /* ----------------------------- End Profile Navigation ----------------------------- */

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

        inboxAdapter = NotificationAdapter(inboxNotifications, this, true)
        historyAdapter = NotificationAdapter(historyNotifications, this, false)

        // Set initial adapter
        recyclerView.adapter = inboxAdapter
    }

    private fun loadDummyData() {
        // Add inbox notifications
        inboxNotifications.add(NotificationItem(1, "Saudara Budi ingin melihat laporan anda", "Budi@gmail.com"))
        inboxNotifications.add(NotificationItem(2, "Saudara Ahmad ingin melihat laporan anda", "Ahmad@gmail.com"))
        inboxNotifications.add(NotificationItem(3, "Saudara Dewi ingin melihat laporan anda", "Dewi@gmail.com"))
        inboxAdapter.updateNotifications(inboxNotifications)

        // Add history notifications
        historyNotifications.add(NotificationItem(4, "Saudara Rini ingin melihat laporan anda", "Rini@gmail.com", NotificationStatus.ALLOWED))
        historyNotifications.add(NotificationItem(5, "Saudara Joko ingin melihat laporan anda", "Joko@gmail.com", NotificationStatus.REJECTED))
        historyAdapter.updateNotifications(historyNotifications)
    }

    override fun onAllowClick(notification: NotificationItem) {
        // Remove from inbox
        val index = inboxNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            inboxNotifications.removeAt(index)
            inboxAdapter.updateNotifications(inboxNotifications)

            // Add to history with ALLOWED status
            val updatedNotification = notification.copy(status = NotificationStatus.ALLOWED)
            historyNotifications.add(0, updatedNotification)
            historyAdapter.updateNotifications(historyNotifications)
        }
    }

    override fun onRejectClick(notification: NotificationItem) {
        // Remove from inbox
        val index = inboxNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            inboxNotifications.removeAt(index)
            inboxAdapter.updateNotifications(inboxNotifications)

            // Add to history with REJECTED status
            val updatedNotification = notification.copy(status = NotificationStatus.REJECTED)
            historyNotifications.add(0, updatedNotification)
            historyAdapter.updateNotifications(historyNotifications)
        }
    }

    override fun onDeleteClick(notification: NotificationItem) {
        // Only delete from history
        val index = historyNotifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            historyNotifications.removeAt(index)
            historyAdapter.updateNotifications(historyNotifications)
        }
    }
}
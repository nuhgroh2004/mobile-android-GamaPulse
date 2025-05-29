package com.example.gamapulse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gamapulse.model.NotificationItem

/* ----------------------------- NotificationAdapter ----------------------------- */
class NotificationAdapter(
    private var notifications: List<NotificationItem>,
    private val listener: NotificationActionListener,
    private val isInboxView: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_INBOX = 1
        private const val VIEW_TYPE_HISTORY = 2
    }

    interface NotificationActionListener {
        fun onAllowClick(notification: NotificationItem)
        fun onRejectClick(notification: NotificationItem)
        fun onDeleteClick(notification: NotificationItem)
    }

    /* ----------------------------- InboxViewHolder ----------------------------- */
    inner class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderTextView: TextView = itemView.findViewById(R.id.tvNotificationSender)
        val emailTextView: TextView = itemView.findViewById(R.id.tvNotificationEmail)
        val allowButton: Button = itemView.findViewById(R.id.btnAllow)
        val rejectButton: Button = itemView.findViewById(R.id.btnReject)

        init {
            allowButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onAllowClick(notifications[position])
                }
            }
            rejectButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRejectClick(notifications[position])
                }
            }
        }
    }
    /* ----------------------------- InboxViewHolder ----------------------------- */

    /* ----------------------------- HistoryViewHolder ----------------------------- */
    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderTextView: TextView = itemView.findViewById(R.id.tvNotificationSender)
        val emailTextView: TextView = itemView.findViewById(R.id.tvNotificationEmail)
        val statusTextView: TextView = itemView.findViewById(R.id.tvStatus)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)

        init {
            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(notifications[position])
                }
            }
        }
    }
    /* ----------------------------- HistoryViewHolder ----------------------------- */

    /* ----------------------------- getItemViewType ----------------------------- */
    override fun getItemViewType(position: Int): Int {
        return if (isInboxView) VIEW_TYPE_INBOX else VIEW_TYPE_HISTORY
    }
    /* ----------------------------- getItemViewType ----------------------------- */

    /* ----------------------------- onCreateViewHolder ----------------------------- */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_INBOX -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification_inbox, parent, false)
                InboxViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification_history, parent, false)
                HistoryViewHolder(view)
            }
        }
    }
    /* ----------------------------- onCreateViewHolder ----------------------------- */

    /* ----------------------------- onBindViewHolder ----------------------------- */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notification = notifications[position]
        when (holder) {
            is InboxViewHolder -> {
                holder.senderTextView.text = notification.message
                holder.emailTextView.text = notification.email
            }
            is HistoryViewHolder -> {
                holder.senderTextView.text = notification.message
                holder.emailTextView.text = notification.email
                when (notification.status) {
                    NotificationStatus.ALLOWED -> {
                        holder.statusTextView.text = "Diizinkan"
                        holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.green_allowed))
                    }
                    NotificationStatus.REJECTED -> {
                        holder.statusTextView.text = "Ditolak"
                        holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.red_rejected))
                    }
                    else -> {
                        holder.statusTextView.visibility = View.GONE
                    }
                }
            }
        }
    }
    /* ----------------------------- onBindViewHolder ----------------------------- */

    /* ----------------------------- getItemCount ----------------------------- */
    override fun getItemCount() = notifications.size
    /* ----------------------------- getItemCount ----------------------------- */

    /* ----------------------------- updateNotifications ----------------------------- */
    fun updateNotifications(newNotifications: List<NotificationItem>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
    /* ----------------------------- updateNotifications ----------------------------- */
}
/* ----------------------------- NotificationAdapter ----------------------------- */
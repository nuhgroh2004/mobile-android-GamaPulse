package com.example.gamapulse

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavView: BottomNavigationView
    private var notificationBadge: BadgeDrawable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        bottomNavView = findViewById(R.id.bottom_navigation)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
        bottomNavView.selectedItemId = R.id.home
        bottomNavView.setOnNavigationItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.home -> HomeFragment()
                R.id.report -> ReportFragment()
                R.id.notification -> {
                    clearNotificationBadge()
                    NotificationFragment()
                }
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()
            true
        }
        setupNotificationBadge()
    }
    private fun setupNotificationBadge() {
        notificationBadge = bottomNavView.getOrCreateBadge(R.id.notification)
        notificationBadge?.isVisible = false
    }
    fun updateNotificationBadge(count: Int) {
        if (count > 0) {
            notificationBadge?.apply {
                number = count
                isVisible = true
            }
        } else {
            clearNotificationBadge()
        }
    }
    private fun clearNotificationBadge() {
        notificationBadge?.isVisible = false
    }
}
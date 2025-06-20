package com.example.rakshini.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rakshini.R
import com.example.rakshini.ui.login.LoginActivity
import com.example.rakshini.ui.period_tracker.PeriodTrackerActivity
import com.example.rakshini.ui.blogs.BlogsActivity
import com.example.rakshini.ui.sos.SosActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Bind the new logout button (LinearLayout)
        val logoutButton = findViewById<LinearLayout>(R.id.logout_button)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Set click listener for logout
        logoutButton.setOnClickListener {
            Log.d(TAG, "Logout button clicked")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Set up bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_sos -> {
                    startActivity(Intent(this, SosActivity::class.java))
                    true
                }
                R.id.nav_period_tracker -> {
                    startActivity(Intent(this, PeriodTrackerActivity::class.java))
                    true
                }
                R.id.nav_blogs -> {
                    startActivity(Intent(this, BlogsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
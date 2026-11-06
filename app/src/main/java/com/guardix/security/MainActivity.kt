package com.guardix.security

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.guardix.security.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val welcomeDelay = Runnable {
        openAuthentication()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showWelcomeScreen()

        handler.postDelayed(welcomeDelay, 3000)
    }

    private fun showWelcomeScreen() {
        setContentView(
            WelcomeScreen.create(this)
        )
    }

    private fun openAuthentication() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(10, 16, 35))
        }

        val message = TextView(this).apply {
            text = "GUARDIX AUTHENTICATION"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        root.addView(message)

        setContentView(root)
    }

    override fun onDestroy() {
        handler.removeCallbacks(welcomeDelay)
        super.onDestroy()
    }
}
package com.guardix.security.welcome

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object WelcomeScreen {

    private val backgroundColor = Color.rgb(10, 16, 35)

    fun create(context: Context): ScrollView {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 25, 24, 25)
            setBackgroundColor(backgroundColor)
        }

        val logo = TextView(context).apply {
            text = "🛡️"
            textSize = 65f
            gravity = Gravity.CENTER
        }

        root.addView(logo)

        val title = TextView(context).apply {
            text = "GUARDIX"
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 10)
        }

        root.addView(title)

        val subtitle = TextView(context).apply {
            text = "SMART SECURITY"
            textSize = 17f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        root.addView(subtitle)

        val information = """
            CREATED BY

            Name: Altamash Khan
            Age: 14 Years
            Class: 6th
            Address: Bahraich, Uttar Pradesh, India

            Our goal is to make
            mobile security smarter.
        """.trimIndent()

        val infoText = TextView(context).apply {
            text = information
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 25, 8, 25)
        }

        root.addView(infoText)

        return ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(root)
        }
    }
}
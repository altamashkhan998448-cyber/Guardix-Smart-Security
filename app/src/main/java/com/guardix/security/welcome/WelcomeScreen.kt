package com.guardix.security.welcome

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object WelcomeScreen {

    private const val BACKGROUND_COLOR = 0xFF0A1023.toInt()

    fun create(context: Context): ScrollView {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 25, 24, 25)
            setBackgroundColor(BACKGROUND_COLOR)
        }

        val logo = TextView(context).apply {
            text = "🛡️"
            textSize = 65f
            gravity = Gravity.CENTER
        }

        root.addView(
            logo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

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
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        root.addView(subtitle)

        val creatorInfo = TextView(context).apply {
            text = """
                CREATED BY

                Altamash Khan

                Age: 14 Years
                Class: 6th

                Bahraich,
                Uttar Pradesh, India

                Making Mobile Security Smarter.
            """.trimIndent()

            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setLineSpacing(8f, 1f)
            setPadding(8, 30, 8, 25)
        }

        root.addView(creatorInfo)

        return ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(BACKGROUND_COLOR)
            addView(root)
        }
    }
}
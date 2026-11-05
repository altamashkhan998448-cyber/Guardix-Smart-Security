package com.guardix.security

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private var isHindi = false

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWelcome()
    }

    private fun showWelcome() {

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 30, 28, 30)
            setBackgroundColor(Color.rgb(10, 16, 35))
        }

        val logo = TextView(this).apply {
            text = "🛡️"
            textSize = 65f
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "GUARDIX"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "SMART SECURITY"
            textSize = 15f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        val info = TextView(this).apply {
            text = getCreatorInfo()
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, 35, 10, 35)
            setLineSpacing(10f, 1f)
        }

        val languageButton = Button(this).apply {
            text = "हिंदी / English"
            setOnClickListener {
                isHindi = !isHindi
                info.text = getCreatorInfo()
            }
        }

        val okButton = Button(this).apply {
            text = "OK"
            setOnClickListener {
                showDashboard()
            }
        }

        root.addView(logo)
        root.addView(title)
        root.addView(subtitle)
        root.addView(info)
        root.addView(languageButton)
        root.addView(okButton)

        setContentView(root)
    }

    private fun getCreatorInfo(): String {

        return if (isHindi) {
            """
            ऐप निर्माता

            नाम: Altamash Khan
            उम्र: 14 वर्ष
            कक्षा: 6वीं
            पता: बहराइच, उत्तर प्रदेश, भारत

            इस ऐप को बनाने का उद्देश्य
            मोबाइल सुरक्षा को बेहतर बनाना है।
            """.trimIndent()
        } else {
            """
            CREATED BY

            Name: Altamash Khan
            Age: 14 Years
            Class: 6th
            Address: Bahraich, Uttar Pradesh, India

            Our goal is to make
            mobile security smarter.
            """.trimIndent()
        }
    }

    private fun showDashboard() {

        root.removeAllViews()

        val heading = TextView(this).apply {
            text = "GUARDIX"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = if (isHindi) {
                "स्मार्ट सिक्योरिटी में आपका स्वागत है!"
            } else {
                "Welcome to Smart Security!"
            }
            textSize = 18f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        root.addView(heading)
        root.addView(message)

        setContentView(root)
    }
}
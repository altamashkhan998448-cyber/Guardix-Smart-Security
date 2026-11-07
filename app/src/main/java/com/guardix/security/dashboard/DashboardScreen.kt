package com.guardix.security.dashboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object DashboardScreen {

    private const val BACKGROUND = 0xFF0A1023.toInt()

    fun create(
        context: Context,
        onSetupSecurity: () -> Unit = {},
        onFaceSecurity: () -> Unit = {},
        onSignOut: () -> Unit
    ): View {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 25, 22, 25)
            setBackgroundColor(BACKGROUND)
        }

        fun addText(text: String, size: Float, color: Int) {
            root.addView(TextView(context).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 12)
            })
        }

        addText("🛡️ GUARDIX", 30f, Color.CYAN)
        addText("SMART SECURITY DASHBOARD", 15f, Color.WHITE)
        addText("Welcome to Guardix!", 23f, Color.WHITE)

        addButton(
            root,
            "🔐  SET UP SECURITY",
            "Create or update your Security PIN",
            Color.rgb(20, 115, 190)
        ) {
            onSetupSecurity()
        }

        addButton(
            root,
            "👤  FACE SECURITY",
            "Set up or verify your face",
            Color.rgb(30, 90, 145)
        ) {
            onFaceSecurity()
        }

        addButton(
            root,
            "📱  DEVICE SECURITY",
            "Device security dashboard",
            Color.rgb(24, 35, 62)
        ) { }

        addButton(
            root,
            "⚙️  SETTINGS",
            "Manage your app settings",
            Color.rgb(24, 35, 62)
        ) { }

        addButton(
            root,
            "SIGN OUT",
            "Exit your current session",
            Color.rgb(180, 40, 50)
        ) {
            onSignOut()
        }

        return ScrollView(context).apply {
            isFillViewport = true
            addView(root)
        }
    }

    private fun addButton(
        parent: LinearLayout,
        heading: String,
        description: String,
        background: Int,
        action: () -> Unit
    ) {

        val card = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(background)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        card.addView(TextView(parent.context).apply {
            text = heading
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })

        card.addView(TextView(parent.context).apply {
            text = description
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 0)
        })

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 15)
        parent.addView(card, params)
    }
}
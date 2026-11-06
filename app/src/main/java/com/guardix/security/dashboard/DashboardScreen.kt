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
        onSignOut: () -> Unit
    ): View {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 25, 22, 25)
            setBackgroundColor(BACKGROUND)
        }

        val title = TextView(context).apply {
            text = "🛡️ GUARDIX"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 10)
        }

        root.addView(title)

        val subtitle = TextView(context).apply {
            text = "SMART SECURITY DASHBOARD"
            textSize = 15f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        root.addView(subtitle)

        val welcome = TextView(context).apply {
            text = "Welcome to Guardix!"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 25)
        }

        root.addView(welcome)

        addFeature(
            context,
            root,
            "🔐  PIN Security",
            "Your PIN security setup"
        )

        addFeature(
            context,
            root,
            "👤  Face Security",
            "Face movement verification"
        )

        addFeature(
            context,
            root,
            "📱  Device Security",
            "Device security dashboard"
        )

        addFeature(
            context,
            root,
            "⚙️  Settings",
            "Manage your app settings"
        )

        val signOut = TextView(context).apply {
            text = "SIGN OUT"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(20, 18, 20, 18)
            setBackgroundColor(Color.rgb(180, 40, 50))
            setOnClickListener {
                onSignOut()
            }
        }

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        buttonParams.setMargins(0, 30, 0, 15)

        root.addView(signOut, buttonParams)

        return ScrollView(context).apply {
            isFillViewport = true
            addView(root)
        }
    }

    private fun addFeature(
        context: Context,
        parent: LinearLayout,
        heading: String,
        description: String
    ) {

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(Color.rgb(24, 35, 62))
        }

        val title = TextView(context).apply {
            text = heading
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val detail = TextView(context).apply {
            text = description
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 0)
        }

        card.addView(title)
        card.addView(detail)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 15)

        parent.addView(card, params)
    }
}
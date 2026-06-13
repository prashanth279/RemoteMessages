package com.remote.outpost

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    private val perms: Array<String> by lazy {
        val list = mutableListOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.FOREGROUND_SERVICE,
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            list.add(android.Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
        }
        list.toTypedArray()
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { updateUI() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val root = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(getThemeColor(android.R.attr.windowBackground))
            isVerticalScrollBarEnabled = false
        }
        
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 100, 80, 80)
            gravity = Gravity.START
        }
        root.addView(container)
        setContentView(root)
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        container.removeAllViews()
        
        val textColorPrimary = getThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val textColorSecondary = getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        
        TextView(this).apply {
            text = getString(R.string.permissions_title)
            textSize = 28f
            setPadding(0, 0, 0, 12)
            setTextColor(textColorPrimary)
            typeface = Typeface.DEFAULT_BOLD
            container.addView(this)
        }

        TextView(this).apply {
            text = "Required for persistent operation"
            textSize = 14f
            setTextColor(textColorSecondary)
            setPadding(0, 0, 0, 60)
            container.addView(this)
        }

        // 1. Regular System Permissions
        perms.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            val permissionName = when(permission) {
                android.Manifest.permission.RECEIVE_SMS -> getString(R.string.perm_sms)
                android.Manifest.permission.POST_NOTIFICATIONS -> getString(R.string.perm_notifications)
                android.Manifest.permission.RECEIVE_BOOT_COMPLETED -> getString(R.string.perm_boot)
                else -> permission.substringAfterLast(".")
            }
            addStatusRow(permissionName, isGranted)
        }

        // 2. Battery Optimization
        val isBatteryIgnored = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
        addStatusRow(getString(R.string.unrestricted_battery_label), isBatteryIgnored)

        // 3. Hibernation (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isHibernationDisabled = isHibernationDisabled()
            addStatusRow(getString(R.string.hibernation_label), isHibernationDisabled)
        }

        addSpacer(60)
        
        // Grant Button
        addPrimaryButton(getString(R.string.btn_grant_all)) {
            launcher.launch(perms)
        }

        addSpacer(20)

        // Battery Button
        addSecondaryButton(getString(R.string.btn_battery_settings)) {
            @SuppressLint("BatteryLife")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try { startActivity(intent) } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addSpacer(20)
            addSecondaryButton(getString(R.string.btn_disable_hibernation)) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }

        addSpacer(40)
        TextView(this).apply {
            text = getString(R.string.hibernation_note)
            textSize = 12f
            setTextColor(textColorSecondary)
            setLineSpacing(0f, 1.2f)
            container.addView(this)
        }
    }

    private fun isHibernationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            packageManager.isAutoRevokeWhitelisted
        } else {
            // minSdk is 34, so this branch is theoretically not reached but kept for logic
            true
        }
    }

    private fun addStatusRow(name: String, ok: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 24)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        TextView(this).apply {
            text = name
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 16f
            setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            row.addView(this)
        }

        TextView(this).apply {
            text = if (ok) getString(R.string.status_allowed).uppercase() else getString(R.string.status_not_allowed).uppercase()
            setTextColor(if (ok) Color.parseColor("#00C853") else Color.parseColor("#FF5252"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            row.addView(this)
        }
        
        container.addView(row)
        
        // Add divider
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorOutlineVariant))
            container.addView(this)
        }
    }

    private fun addPrimaryButton(label: String, onClick: () -> Unit) {
        Button(this).apply {
            text = label
            isAllCaps = false
            setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorPrimary))
            setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
            setPadding(0, 40, 0, 40)
            stateListAnimator = null
            elevation = 0f
            setOnClickListener { onClick() }
            container.addView(this)
        }
    }

    private fun addSecondaryButton(label: String, onClick: () -> Unit) {
        Button(this).apply {
            text = label
            isAllCaps = false
            setBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant))
            setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface))
            setPadding(0, 40, 0, 40)
            stateListAnimator = null
            elevation = 0f
            setOnClickListener { onClick() }
            container.addView(this)
        }
    }

    private fun addSpacer(height: Int) {
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, height)
            container.addView(this)
        }
    }

    @ColorInt
    private fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}

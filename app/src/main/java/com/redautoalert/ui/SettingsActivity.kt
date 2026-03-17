package com.redautoalert.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.redautoalert.R
import com.redautoalert.RedAutoAlertApp
import com.redautoalert.model.AlertEvent
import com.redautoalert.service.AlertEventBus
import com.redautoalert.util.DebugLog
import com.redautoalert.util.PermissionHelper
import com.redautoalert.util.PrefsManager

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val POST_NOTIFICATION_REQUEST_CODE = 1001
    }

    private lateinit var prefs: PrefsManager

    // Views
    private lateinit var statusText: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var forwardingSwitch: SwitchMaterial
    private lateinit var ttsSwitch: SwitchMaterial
    private lateinit var languageSpinner: Spinner
    private lateinit var grantPermissionButton: Button
    private lateinit var testAlertButton: Button
    private lateinit var debugLogText: TextView
    private lateinit var debugLogScroll: ScrollView
    private val debugLogListener: () -> Unit = { runOnUiThread { refreshDebugLog() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PrefsManager(this)

        bindViews()
        setupListeners()
        requestPostNotificationPermission()

        DebugLog.addListener(debugLogListener)
        DebugLog.log("App opened")
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        refreshDebugLog()
    }

    override fun onDestroy() {
        super.onDestroy()
        DebugLog.removeListener(debugLogListener)
    }

    private fun bindViews() {
        statusText = findViewById(R.id.statusText)
        permissionStatus = findViewById(R.id.permissionStatus)
        forwardingSwitch = findViewById(R.id.forwardingSwitch)
        ttsSwitch = findViewById(R.id.ttsSwitch)
        languageSpinner = findViewById(R.id.languageSpinner)
        grantPermissionButton = findViewById(R.id.grantPermissionButton)
        testAlertButton = findViewById(R.id.testAlertButton)
        debugLogText = findViewById(R.id.debugLogText)
        debugLogScroll = findViewById(R.id.debugLogScroll)

        // Set initial values
        forwardingSwitch.isChecked = prefs.isForwardingEnabled
        ttsSwitch.isChecked = prefs.isTtsEnabled

        // Language spinner
        val languages = arrayOf("עברית (Hebrew)", "English", "Русский (Russian)", "العربية (Arabic)")
        val languageCodes = arrayOf("he", "en", "ru", "ar")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
        languageSpinner.setSelection(languageCodes.indexOf(prefs.ttsLanguage).coerceAtLeast(0))
    }

    private fun setupListeners() {
        forwardingSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isForwardingEnabled = isChecked
            updateStatus()
        }

        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isTtsEnabled = isChecked
        }

        languageSpinner.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?, view: android.view.View?,
                position: Int, id: Long
            ) {
                val codes = arrayOf("he", "en", "ru", "ar")
                prefs.ttsLanguage = codes[position]
                (application as? RedAutoAlertApp)?.ttsAnnouncer?.updateLanguage()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        grantPermissionButton.setOnClickListener {
            if (PermissionHelper.needsRestrictedSettingsGuidance() &&
                !PermissionHelper.isNotificationListenerEnabled(this)
            ) {
                showRestrictedSettingsDialog()
            } else {
                PermissionHelper.openNotificationListenerSettings(this)
            }
        }

        testAlertButton.setOnClickListener {
            sendTestAlert()
        }
    }

    private fun updatePermissionStatus() {
        val isEnabled = PermissionHelper.isNotificationListenerEnabled(this)
        if (isEnabled) {
            permissionStatus.text = "✅ Notification access granted"
            permissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            grantPermissionButton.text = "Re-configure Permission"
        } else {
            permissionStatus.text = "❌ Notification access NOT granted — tap button below"
            permissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            grantPermissionButton.text = "Grant Notification Access"
        }
        updateStatus()
    }

    private fun updateStatus() {
        val isListenerEnabled = PermissionHelper.isNotificationListenerEnabled(this)
        val isForwarding = prefs.isForwardingEnabled

        statusText.text = when {
            !isListenerEnabled -> "⚠️ Grant notification access to start"
            !isForwarding -> "⏸️ Forwarding is paused"
            else -> "✅ Active — listening for Red Alert notifications"
        }
    }

    private fun sendTestAlert() {
        val testEvent = AlertEvent(
            id = "test-${System.currentTimeMillis()}",
            title = "🚨 Test Alert / התרעת בדיקה",
            text = "תל אביב - מרכז, ראשון לציון, חולון",
            cities = listOf("תל אביב - מרכז", "ראשון לציון", "חולון"),
            alertType = AlertEvent.AlertType.ROCKET,
            timestamp = System.currentTimeMillis(),
            sourcePackage = "com.redautoalert.test"
        )

        AlertEventBus.emitBlocking(testEvent)
        DebugLog.log("Test alert sent")
        Toast.makeText(this, "Test alert sent! Check Android Auto.", Toast.LENGTH_LONG).show()
    }

    private fun showRestrictedSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Android 13+ Restricted Settings")
            .setMessage(
                "On Android 13+, sideloaded apps need extra steps:\n\n" +
                "1. Tap 'App Info' below\n" +
                "2. Tap the ⋮ menu (top right)\n" +
                "3. Select 'Allow restricted settings'\n" +
                "4. Come back and tap 'Grant Notification Access'\n\n" +
                "This is required because the app was installed outside the Play Store."
            )
            .setPositiveButton("Open App Info") { _, _ ->
                PermissionHelper.openAppInfoSettings(this)
            }
            .setNeutralButton("Skip — Try Direct") { _, _ ->
                PermissionHelper.openNotificationListenerSettings(this)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == POST_NOTIFICATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Notification permission needed to show alerts on Android Auto",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun refreshDebugLog() {
        val entries = DebugLog.getEntries()
        debugLogText.text = if (entries.isEmpty()) {
            "No events yet..."
        } else {
            entries.joinToString("\n") { it.formatted() }
        }
        debugLogScroll.post { debugLogScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}

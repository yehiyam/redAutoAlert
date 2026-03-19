package com.redautoalert.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.redautoalert.BuildConfig
import com.redautoalert.R
import com.redautoalert.model.AlertEvent
import com.redautoalert.service.AlertEventBus
import com.redautoalert.util.DebugLog
import com.redautoalert.util.PermissionHelper
import com.redautoalert.util.PrefsManager

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val POST_NOTIFICATION_REQUEST_CODE = 1001
        private const val FILTER_SAVE_DELAY_MS = 500L
    }

    private lateinit var prefs: PrefsManager

    // Views
    private lateinit var statusText: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var forwardingSwitch: SwitchMaterial
    private lateinit var phoneNotificationSwitch: SwitchMaterial
    private lateinit var includeFilterEdit: TextInputEditText
    private lateinit var excludeFilterEdit: TextInputEditText
    private lateinit var grantPermissionButton: Button
    private lateinit var openAndroidAutoButton: Button
    private lateinit var testAlertButton: Button
    private lateinit var debugLogText: TextView
    private lateinit var debugLogScroll: ScrollView
    private lateinit var debugLogCard: MaterialCardView
    private val debugLogListener: () -> Unit = { runOnUiThread { refreshDebugLog() } }

    private val filterSaveHandler = Handler(Looper.getMainLooper())
    private var includeSaveRunnable: Runnable? = null
    private var excludeSaveRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PrefsManager(this)

        bindViews()
        setupListeners()
        requestPostNotificationPermission()

        DebugLog.log("App opened")
    }

    override fun onStart() {
        super.onStart()
        if (DebugLog.isEnabled) {
            DebugLog.addListener(debugLogListener)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        refreshDebugLog()
    }

    override fun onStop() {
        super.onStop()
        DebugLog.removeListener(debugLogListener)
    }

    private fun bindViews() {
        statusText = findViewById(R.id.statusText)
        permissionStatus = findViewById(R.id.permissionStatus)
        forwardingSwitch = findViewById(R.id.forwardingSwitch)
        phoneNotificationSwitch = findViewById(R.id.phoneNotificationSwitch)
        includeFilterEdit = findViewById(R.id.includeFilterEdit)
        excludeFilterEdit = findViewById(R.id.excludeFilterEdit)
        grantPermissionButton = findViewById(R.id.grantPermissionButton)
        openAndroidAutoButton = findViewById(R.id.openAndroidAutoButton)
        testAlertButton = findViewById(R.id.testAlertButton)
        debugLogText = findViewById(R.id.debugLogText)
        debugLogScroll = findViewById(R.id.debugLogScroll)
        debugLogCard = findViewById(R.id.debugLogCard)

        // Only show debug log in debug builds
        debugLogCard.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE

        // Set initial values
        forwardingSwitch.isChecked = prefs.isForwardingEnabled
        phoneNotificationSwitch.isChecked = prefs.isPhoneNotificationEnabled

        // Filter fields
        includeFilterEdit.setText(prefs.includeFilter)
        excludeFilterEdit.setText(prefs.excludeFilter)
    }

    private fun setupListeners() {
        forwardingSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isForwardingEnabled = isChecked
            updateStatus()
        }

        phoneNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isPhoneNotificationEnabled = isChecked
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

        openAndroidAutoButton.setOnClickListener {
            openAndroidAuto()
        }

        includeFilterEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""
                includeSaveRunnable?.let { filterSaveHandler.removeCallbacks(it) }
                includeSaveRunnable = Runnable { prefs.includeFilter = value }.also {
                    filterSaveHandler.postDelayed(it, FILTER_SAVE_DELAY_MS)
                }
            }
        })

        excludeFilterEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString() ?: ""
                excludeSaveRunnable?.let { filterSaveHandler.removeCallbacks(it) }
                excludeSaveRunnable = Runnable { prefs.excludeFilter = value }.also {
                    filterSaveHandler.postDelayed(it, FILTER_SAVE_DELAY_MS)
                }
            }
        })
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

    private fun openAndroidAuto() {
        val androidAutoPackage = "com.google.android.projection.gearhead"
        try {
            // Try to open Android Auto settings directly
            val settingsIntent = Intent().apply {
                component = ComponentName(
                    androidAutoPackage,
                    "com.google.android.apps.auto.settings.CarSettingsActivity"
                )
            }
            if (settingsIntent.resolveActivity(packageManager) != null) {
                startActivity(settingsIntent)
                return
            }
            // Fall back to launching the Android Auto app
            val launchIntent = packageManager.getLaunchIntentForPackage(androidAutoPackage)
            if (launchIntent != null) {
                startActivity(launchIntent)
                return
            }
            // Android Auto not installed — open Play Store
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$androidAutoPackage")
            )
            if (playStoreIntent.resolveActivity(packageManager) != null) {
                startActivity(playStoreIntent)
            } else {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$androidAutoPackage")
                    )
                )
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Could not open Android Auto", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Could not open Android Auto", Toast.LENGTH_SHORT).show()
        }
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
        if (!BuildConfig.DEBUG) return
        val entries = DebugLog.getEntries()
        debugLogText.text = if (entries.isEmpty()) {
            getString(R.string.debug_log_empty)
        } else {
            entries.joinToString("\n") { it.formatted() }
        }
        debugLogScroll.post { debugLogScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}

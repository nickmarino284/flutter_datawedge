package com.circuskitchens.flutter_datawedge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import com.circuskitchens.flutter_datawedge.pigeon.*

// Enums for DataWedge Commands and Events
enum class DWCommand(val cmd: String) {
    CreateProfile("com.symbol.datawedge.api.CREATE_PROFILE"),
    SetConfig("com.symbol.datawedge.api.SET_CONFIG"),
    SetPluginState("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN"),
    RegisterForNotification("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION"),
    UnregisterForNotification("com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION"),
    SoftScanTrigger("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER")
}

enum class DWEvent(val value: String) {
    ResultAction("com.symbol.datawedge.api.RESULT_ACTION"),
    Action("com.symbol.datawedge.api.ACTION"),
    ResultNotification("com.symbol.datawedge.api.NOTIFICATION_ACTION")
}

class DWInterface(val context: Context, val flutterApi: DataWedgeFlutterApi) : BroadcastReceiver(), DataWedgeHostApi {

    private var isReceiverRegistered = false

    // Registers the broadcast receiver
    fun setupBroadcastReceiver() {
        if (isReceiverRegistered) {
            Log.d("DWInterface", "BroadcastReceiver is already registered.")
            return
        }

        val intentFilter = IntentFilter().apply {
            addAction(context.packageName + ".SCAN_EVENT")
            addAction(DWEvent.ResultAction.value)
            addAction(DWEvent.Action.value)
            addAction(DWEvent.ResultNotification.value)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        try {
            context.registerReceiver(this, intentFilter)
            isReceiverRegistered = true
            Log.d("DWInterface", "BroadcastReceiver registered successfully.")
        } catch (e: Exception) {
            Log.e("DWInterface", "Error registering BroadcastReceiver: \${e.message}")
        }
    }

    // Dispose method to unregister the receiver
    fun dispose() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(this)
                isReceiverRegistered = false
                Log.d("DWInterface", "BroadcastReceiver successfully unregistered.")
            } catch (e: IllegalArgumentException) {
                Log.e("DWInterface", "Receiver not registered or already unregistered: \${e.message}")
            }
        } else {
            Log.d("DWInterface", "BroadcastReceiver was not registered, skipping unregistration.")
        }
    }

    override fun createProfile(profileName: String, callback: (Result<Unit>) -> Unit) {
        try {
            val intent = Intent().apply {
                action = "com.symbol.datawedge.api.CREATE_PROFILE"
                putExtra("PROFILE_NAME", profileName)
            }
            context.sendBroadcast(intent)
            Log.d("DWInterface", "Profile creation command sent for: $profileName")
            callback(Result.success(Unit))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error creating profile: ${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun setProfileConfig(config: ProfileConfig, callback: (Result<Unit>) -> Unit) {
        try {
            val intent = Intent().apply {
                action = DWCommand.SetConfig.cmd
                putExtra("PROFILE_NAME", config.profileName)
                putExtra("CONFIG_MODE", config.configMode.name.uppercase())

                val pluginConfig = Bundle().apply {
                    config.barcodeParamters?.decoderConfig?.forEach { decoderConfig ->
                        decoderConfig?.decoder?.let { decoder ->
                            putBoolean("decoder_\${decoder.name.lowercase()}", decoderConfig.enabled ?: false)
                        }
                    }
                }

                putExtra("PLUGIN_CONFIG", pluginConfig)
            }

            context.sendBroadcast(intent)
            Log.d("DWInterface", "Profile configuration sent: \${config.profileName}")
            callback(Result.success(Unit))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error setting profile config: \${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun enablePlugin(callback: (Result<String>) -> Unit) {
        try {
            val intent = Intent().apply {
                action = DWCommand.SetPluginState.cmd
                putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "ENABLE_PLUGIN")
            }
            context.sendBroadcast(intent)
            callback(Result.success("Plugin Enabled"))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error enabling plugin: \${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun disablePlugin(callback: (Result<String>) -> Unit) {
        try {
            val intent = Intent().apply {
                action = DWCommand.SetPluginState.cmd
                putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "DISABLE_PLUGIN")
            }
            context.sendBroadcast(intent)
            Log.d("DWInterface", "Plugin disabled broadcast sent successfully.")
            callback(Result.success("Plugin Disabled"))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error disabling plugin: \${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun suspendPlugin(callback: (Result<String>) -> Unit) {
        try {
            val intent = Intent().apply {
                action = DWCommand.SetPluginState.cmd
                putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "SUSPEND_PLUGIN")
            }
            context.sendBroadcast(intent)
            Log.d("DWInterface", "Plugin suspend broadcast sent successfully.")
            callback(Result.success("Plugin Suspended"))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error suspending plugin: \${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun resumePlugin(callback: (Result<String>) -> Unit) {
        try {
            val intent = Intent().apply {
                action = DWCommand.SetPluginState.cmd
                putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "RESUME_PLUGIN")
            }
            context.sendBroadcast(intent)
            Log.d("DWInterface", "Plugin resume broadcast sent successfully.")
            callback(Result.success("Plugin Resumed"))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error resuming plugin: \${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun setDecoder(decoder: Decoder, enabled: Boolean, profileName: String, callback: (Result<Unit>) -> Unit) {
        try {
            val intent = Intent().apply {
                action = DWCommand.SetConfig.cmd
                putExtra("PROFILE_NAME", profileName)
                putExtra("PLUGIN_NAME", "BARCODE")
                putExtra("RESET_CONFIG", false)

                val params = Bundle().apply {
                    putBoolean("decoder_\${decoder.name.lowercase()}", enabled)
                }

                val config = Bundle().apply {
                    putBundle("PARAM_LIST", params)
                }

                putExtra("PLUGIN_CONFIG", config)
            }

            context.sendBroadcast(intent)
            Log.d("DWInterface", "Decoder configuration sent: decoder=\${decoder.name}, enabled=\$enabled, profile=\$profileName")
            callback(Result.success(Unit))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error setting decoder: \${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun softScanTrigger(on: Boolean, callback: (Result<String>) -> Unit) {
        try {
            val actionString = if (on) "START_SCANNING" else "STOP_SCANNING"
            val intent = Intent().apply {
                action = DWCommand.SoftScanTrigger.cmd
                putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", actionString)
            }
            context.sendBroadcast(intent)
            Log.d("DWInterface", "Soft scan trigger sent: $actionString")
            callback(Result.success("Soft scan trigger sent successfully."))
        } catch (e: Exception) {
            Log.e("DWInterface", "Error sending soft scan trigger: ${e.message}")
            callback(Result.failure(e))
        }
    }

    override fun unregisterForNotifications() {
        Log.d("DWInterface", "Unregistering for notifications...")
        try {
            val intent = Intent().apply {
                action = DWCommand.UnregisterForNotification.cmd
            }
            context.sendBroadcast(intent)
            Log.d("DWInterface", "Successfully unregistered for notifications.")
        } catch (e: Exception) {
            Log.e("DWInterface", "Error while unregistering for notifications: \${e.message}")
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            Log.e("DWInterface", "Received null intent")
            return
        }

        val action = intent.action
        Log.d("DWInterface", "Received action: \$action")

        when (action) {
            DWEvent.ResultAction.value -> {
                val result = intent.getStringExtra("COMMAND_RESULT") ?: "UNKNOWN_RESULT"
                val command = intent.getStringExtra("COMMAND") ?: "UNKNOWN_COMMAND"
                Log.d("DWInterface", "Result: \$result, Command: \$command")
            }
            context?.packageName + ".SCAN_EVENT" -> {
                val scanData = intent.getStringExtra("com.symbol.datawedge.data_string") ?: ""
                Log.d("DWInterface", "Scan Data: \$scanData")
            }
            else -> Log.w("DWInterface", "Unhandled action: \$action")
        }
    }

    override fun getPackageIdentifer(): String {
        val packageName = context.packageName
        Log.d("DWInterface", "Returning package identifier: \$packageName")
        return packageName
    }

    override fun registerForNotifications() {
        Log.d("DWInterface", "Registering for notifications...")
        try {
            val intent = Intent().apply {
                action = DWCommand.RegisterForNotification.cmd
                putExtra("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS")
            }
            context.sendBroadcast(intent)
            Log.d("DWInterface", "Broadcast sent successfully for registering notifications.")
        } catch (e: Exception) {
            Log.e("DWInterface", "Error in registerForNotifications: \${e.message}")
        }
    }
}

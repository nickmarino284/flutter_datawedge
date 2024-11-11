package com.circuskitchens.flutter_datawedge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.circuskitchens.flutter_datawedge.pigeon.*

enum class DWCommand(val cmd: String) {
    CreateProfile("com.symbol.datawedge.api.CREATE_PROFILE"),
    SetConfig("com.symbol.datawedge.api.SET_CONFIG"),
    SetPluginState("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN"),
    RegisterForNotification("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION"),
    UnregisterForNotification("com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION"),
    SoftScanTrigger("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"),
}

enum class DWEvent(val value: String) {
    ResultAction("com.symbol.datawedge.api.RESULT_ACTION"),
    Action("com.symbol.datawedge.api.ACTION"),
    ResultNotification("com.symbol.datawedge.api.NOTIFICATION_ACTION")
}

enum class DWKeys(val value: String) {
    ApplicationName("com.symbol.datawedge.api.APPLICATION_NAME"),
    NotificationType("com.symbol.datawedge.api.NOTIFICATION_TYPE"),
    ScannerStatus("SCANNER_STATUS"),

    SoftScanTriggerStart("START_SCANNING"),
    SoftScanTriggerStop("STOP_SCANNING")
}

class CommandResult(
    val command: String,
    val commandIdentifier: String,
    val extras: Bundle,
    val result: String
) {

}

class DWInterface(val context: Context, val flutterApi: DataWedgeFlutterApi) : BroadcastReceiver(),
    DataWedgeHostApi {

    private var isReceiverRegistered = false

    // Registers a broadcast receive that listens to datawedge intents
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
            Log.e("DWInterface", "Error registering BroadcastReceiver: ${e.message}")
        }
    }



    // A map that contains the callbacks that are associated to the command identifier
    val callbacks = HashMap<String, (Result<CommandResult>) -> Unit>()


    companion object {


        // DataWedge Extras
        const val EXTRA_GET_VERSION_INFO = "com.symbol.datawedge.api.GET_VERSION_INFO"

        const val EXTRA_KEY_APPLICATION_NAME = "com.symbol.datawedge.api.APPLICATION_NAME"
        const val EXTRA_KEY_NOTIFICATION_TYPE = "NOTIFICATION_TYPE"
        const val EXTRA_SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"
        const val EXTRA_RESULT_NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION"
        const val EXTRA_REGISTER_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION"

        const val EXTRA_RESULT_NOTIFICATION_TYPE = "NOTIFICATION_TYPE"
        const val EXTRA_KEY_VALUE_SCANNER_STATUS = "SCANNER_STATUS"
        const val EXTRA_KEY_VALUE_PROFILE_SWITCH = "PROFILE_SWITCH"
        const val EXTRA_KEY_VALUE_PROFILE_ENABLED = "PROFILE_ENABLED"
        const val EXTRA_KEY_VALUE_CONFIGURATION_UPDATE = "CONFIGURATION_UPDATE"
        const val EXTRA_KEY_VALUE_NOTIFICATION_STATUS = "STATUS"
        const val EXTRA_KEY_VALUE_NOTIFICATION_PROFILE_NAME = "PROFILE_NAME"
        const val EXTRA_SEND_RESULT = "SEND_RESULT"

        const val EXTRA_RESULT = "RESULT"
        const val EXTRA_RESULT_GET_ACTIVE_PROFILE =
            "com.symbol.datawedge.api.RESULT_GET_ACTIVE_PROFILE"
        const val EXTRA_RESULT_GET_PROFILES_LIST =
            "com.symbol.datawedge.api.RESULT_GET_PROFILES_LIST"
        const val EXTRA_RESULT_INFO = "RESULT_INFO"
        const val EXTRA_COMMAND = "COMMAND"
        const val EXTRA_COMMAND_IDENTIFIER = "COMMAND_IDENTIFIER"

        // DataWedge Actions
        const val ACTION_GET_SCANNER_STATUS = "com.symbol.datawedge.api.GET_SCANNER_STATUS"
        const val ACTION_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"

        const val DATAWEDGE_SCAN_EXTRA_SOURCE = "com.symbol.datawedge.source"
        const val DATAWEDGE_SCAN_EXTRA_DATA_STRING = "com.symbol.datawedge.data_string"
        const val DATAWEDGE_SCAN_EXTRA_LABEL_TYPE = "com.symbol.datawedge.label_type"
        const val DATAWEDGE_SCAN_EXTRA_DECODE_DATA = "com.symbol.datawedge.decode_data"
        const val DATAWEDGE_SCAN_EXTRA_DECODE_MODE = "com.symbol.datawedge.decoded_mode"

    }


    fun intentToString(intent: Intent?): String? {
        if (intent == null) return ""
        val stringBuilder = StringBuilder("action: ")
            .append(intent.action)
            .append(" data: ")
            .append(intent.dataString)
            .append(" extras: ")
        for (key in intent.extras!!.keySet()) stringBuilder.append(key).append("=").append(
            intent.extras!![key]
        ).append(" ")
        return stringBuilder.toString()
    }

    // Called whenever an intent is passed to the broadcast receiver
    private var lastScannerStatusTime: Long = 0
    private val debounceTime = 20000 // 2 seconds
    private var lastScannerStatus: String? = null

    private fun isDebounced(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastScannerStatusTime < debounceTime) {
            true
        } else {
            lastScannerStatusTime = currentTime
            false
        }
    }

    private fun shouldProcessStatus(status: String): Boolean {
        return if (status == lastScannerStatus) {
            false
        } else {
            lastScannerStatus = status
            true
        }
    }

    private fun mapScannerState(status: String): ScannerState {
        return when (status) {
            "WAITING" -> ScannerState.WAITING
            "DISABLED" -> ScannerState.DISABLED
            "SCANNING" -> ScannerState.SCANNING
            "IDLE" -> ScannerState.IDLE
            "CONNECTED" -> ScannerState.CONNECTED
            "DISCONNECTED" -> ScannerState.DISCONNECTED
            else -> throw IllegalArgumentException("Unknown scanner state: $status")
        }
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            Log.e("DWInterface", "Received null intent")
            return
        }

        val action = intent.action
        val extras = intent.extras ?: run {
            Log.e("DWInterface", "Received intent with null extras")
            return
        }

        val isDebug = 0 != (context?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE) ?: 0)
        if (isDebug) {
            Log.d("DWInterface", "Received action: $action")
        }

        if (action == null) {
            Log.e("DWInterface", "Received intent with null action")
            return
        }

        when (action) {

            (DWEvent.ResultAction.value) -> {
                val result = intent.getStringExtra(DWInterface.EXTRA_RESULT) ?: "UNKNOWN_RESULT"
                val command = intent.getStringExtra(DWInterface.EXTRA_COMMAND) ?: "UNKNOWN_COMMAND"
                val commandIdentifier = intent.getStringExtra(DWInterface.EXTRA_COMMAND_IDENTIFIER) ?: ""

                Log.d("DWInterface", commandIdentifier)

                if (commandIdentifier.isNotBlank() && callbacks.containsKey(commandIdentifier)) {

                } else {
                    Log.e("DWInterface", "Unknown command was returned: $commandIdentifier")
                }
            }

            (context?.packageName + ".SCAN_EVENT") -> {
                val source = intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_SOURCE) ?: "UNKNOWN_SOURCE"
                val scanData = intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DATA_STRING) ?: ""
                val labelType = intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_LABEL_TYPE) ?: ""
                val decodedData: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val byteArray = intent.getByteArrayExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DECODE_DATA)
                    byteArray?.let { listOf(it) } ?: listOf()
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DECODE_DATA) as? List<ByteArray> ?: listOf()
                }


                val decodeMode = intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DECODE_MODE)

                flutterApi.onScanResult(
                    ScanEvent(
                        labelType = convertLabelType(labelType),
                        source = when (source) {
                            "msr" -> ScanSource.MSR
                            "scanner" -> ScanSource.SCANNER
                            "simulscan" -> ScanSource.SIMULSCAN
                            "serial" -> ScanSource.SERIAL
                            "voice" -> ScanSource.VOICE
                            "rfid" -> ScanSource.RFID
                            else -> {
                                Log.e("DWInterface", "Unknown source: $source. This action will not be processed.")
                                return
                            }
                        },
                        dataString = scanData,
                        decodeData = decodedData,
                        decodeMode = when (decodeMode) {
                            "multiple_decode" -> DecodeMode.MULTIPLE
                            "single_decode" -> DecodeMode.SINGLE
                            else -> {
                                Log.e("DWInterface", "Unknown decode mode received: $decodeMode. Skipping this decode mode.")
                                return
                            }
                        }
                    )
                ) { result ->
                    if (result.isSuccess) {
                        Log.d("DWInterface", "Operation succeeded")
                    } else {
                        Log.e("DWInterface", "Operation failed with exception: ${result.exceptionOrNull()}")
                    }
                }
            }

            (DWEvent.ResultNotification.value) -> {
                if (isDebounced()) {
                    Log.d("DWInterface", "Skipping repeated scanner status update due to debounce")
                    return
                }

                if (!intent.hasExtra(EXTRA_RESULT_NOTIFICATION)) {
                    return
                }
                val notification = intent.getBundleExtra(EXTRA_RESULT_NOTIFICATION) ?: return
                val notificationType = notification.getString(EXTRA_KEY_NOTIFICATION_TYPE) ?: return

                Log.d("Notification", notificationType)

                when (notificationType) {
                    EXTRA_KEY_VALUE_SCANNER_STATUS -> {
                        val status = notification.getString(DWInterface.EXTRA_KEY_VALUE_NOTIFICATION_STATUS) ?: "UNKNOWN_STATUS"

                        if (!shouldProcessStatus(status)) {
                            Log.d("DWInterface", "Skipping repeated scanner status update: $status")
                            return
                        }

                        val scannerState = try {
                            mapScannerState(status)
                        } catch (e: IllegalArgumentException) {
                            Log.e("DWInterface", e.message.toString())
                            return
                        }

                        flutterApi.onScannerStatusChanged(
                            StatusChangeEvent(newState = scannerState)
                        ) { }
                    }

                    EXTRA_KEY_VALUE_PROFILE_SWITCH -> {
                        flutterApi.onProfileChange { }
                    }

                    EXTRA_KEY_VALUE_CONFIGURATION_UPDATE -> {
                        flutterApi.onConfigUpdate { }
                    }
                }
            }
        }
    }




    // Clear all callbacks to prevent holding unresolvable references and unregister the broadcast receiver
    fun dispose() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(this)
                isReceiverRegistered = false
                Log.d("DWInterface", "BroadcastReceiver successfully unregistered.")
            } catch (e: IllegalArgumentException) {
                Log.e("DWInterface", "Receiver not registered or already unregistered: ${e.message}")
            }
        } else {
            Log.d("DWInterface", "BroadcastReceiver was not registered, skipping unregistration.")
        }
    }



    // from https://stackoverflow.com/questions/46943860/idiomatic-way-to-generate-a-random-alphanumeric-string-in-kotlin
    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun sendCommandString(
        command: DWCommand,
        parameter: String,
        callback: (Result<CommandResult>) -> Unit
    ) {
        sendCommand(command, parameter, callback)
    }

    fun sendCommandBundle(

        command: DWCommand,
        parameter: Bundle,
        callback: (Result<CommandResult>) -> Unit
    ) {
        sendCommand(command, parameter, callback)
    }

    private fun sendCommand(
        command: DWCommand,
        parameter: Any,
        callback: (Result<CommandResult>) -> Unit
    ) {
        // Generate a random command identifier
        val commandIdentifier = getRandomString(10)
        // Keep a reference to the callback by the command id
        callbacks[commandIdentifier] = callback

        val dwIntent = Intent()
        dwIntent.action = DWEvent.Action.value


        // This is certainly not elegant... not sure how this could be done more elegantly
        if (parameter is String) {
            dwIntent.putExtra(command.cmd, parameter)
        } else if (parameter is Bundle) {
            dwIntent.putExtra(command.cmd, parameter)
        } else {
            callback(Result.failure(Error("Unsupported payload type")))
            return
        }

        dwIntent.putExtra(EXTRA_SEND_RESULT, "true")
        dwIntent.putExtra(EXTRA_COMMAND_IDENTIFIER, commandIdentifier)

        context.sendBroadcast(dwIntent)

    }


    override fun createProfile(
        profileName: String,
        callback: (kotlin.Result<Unit>) -> Unit
    ) {
        sendCommand(DWCommand.CreateProfile, profileName) { result ->
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    callback(Result.failure(exception))
                } else {
                    callback(Result.failure(Exception("Unknown error occurred during profile creation.")))
                }
            } else {
                val cmd = result.getOrThrow()
                when (cmd.result) {
                    "SUCCESS" -> {
                        Log.d("DWInterface", "Profile creation successful: $profileName")
                        callback(Result.success(Unit))
                    }
                    else -> {
                        Log.e("DWInterface", "Profile creation failed: ${cmd.result}")
                        callback(Result.failure(Exception("Failed to create profile: ${cmd.result}")))
                    }
                }
            }
        }
    }


    override fun suspendPlugin(callback: (Result<String>) -> Unit) {
        sendCommandString(DWCommand.SetPluginState, "SUSPEND_PLUGIN") { result ->
            if (result.isFailure) {
                callback(Result.failure(result.exceptionOrNull()!!))

            } else {
                val cmd = result.getOrThrow()
                when (cmd.result) {
                    // Unsure whether this exists, not in the docs
                    "SCANNER_SUSPEND_FAILED" -> callback(Result.failure(Error(cmd.result)))
                    "SCANNER_ALREADY_SUSPENDED" -> callback(Result.failure(Error(cmd.result)))
                    "PLUGIN_DISABLED" -> callback(Result.failure(Error(cmd.result)))
                    else -> callback(Result.success(cmd.result))
                }
            }

        }
    }

    override fun resumePlugin(callback: (Result<String>) -> Unit) {
        sendCommandString(DWCommand.SetPluginState, "RESUME_PLUGIN") { result ->
            if (result.isFailure) {
                callback(Result.failure(result.exceptionOrNull()!!))

            } else {
                val cmd = result.getOrThrow()
                when (cmd.result) {
                    "SCANNER_RESUME_FAILED" -> callback(Result.failure(Error(cmd.result)))
                    "SCANNER_ALREADY_RESUMED" -> callback(Result.failure(Error(cmd.result)))
                    "PLUGIN_DISABLED" -> callback(Result.failure(Error(cmd.result)))
                    else -> callback(Result.success(cmd.result))
                }
            }
        }
    }

    override fun enablePlugin(callback: (Result<String>) -> Unit) {
        sendCommandString(DWCommand.SetPluginState, "ENABLE_PLUGIN") { result ->
            if (result.isFailure) {
                callback(Result.failure(result.exceptionOrNull()!!))

            } else {
                val cmd = result.getOrThrow()
                when (cmd.result) {
                    "SCANNER_ALREADY_ENABLED" -> callback(Result.failure(Error(cmd.result)))
                    "SCANNER_ENABLE_FAILED" -> callback(Result.failure(Error(cmd.result)))
                    else -> callback(Result.success(cmd.result))
                }
            }
        }
    }


    override fun disablePlugin(callback: (Result<String>) -> Unit) {
        sendCommandString(DWCommand.SetPluginState, "DISABLE_PLUGIN") { result ->
            if (result.isFailure) {
                callback(Result.failure(result.exceptionOrNull()!!))

            } else {
                val cmd = result.getOrThrow()
                when (cmd.result) {
                    "SCANNER_ALREADY_DISABLED" -> callback(Result.failure(Error(cmd.result)))
                    "SCANNER_DISABLE_FAILED" -> callback(Result.failure(Error(cmd.result)))
                    else -> callback(Result.success(cmd.result))
                }
            }
        }
    }


    override fun getPackageIdentifer(): String {
        return context.applicationInfo.packageName
    }

    override fun registerForNotifications(): Unit {

        val params = Bundle()

        Log.d("ApplicationName", DWKeys.ApplicationName.value)
        Log.d("NotificationType", DWKeys.NotificationType.value)
        Log.d("ScannerStatus", DWKeys.ScannerStatus.value)

        params.putString(DWKeys.ApplicationName.value, getPackageIdentifer())
        params.putString(DWKeys.NotificationType.value, DWKeys.ScannerStatus.value)

        sendCommandBundle(DWCommand.RegisterForNotification, params) { res ->
            // This command never returns
        }
    }

    override fun softScanTrigger(on: Boolean, callback: (Result<String>) -> Unit) {

        var cmdValue = DWKeys.SoftScanTriggerStart.value

        if (!on)
            cmdValue = DWKeys.SoftScanTriggerStop.value



        sendCommandString(DWCommand.SoftScanTrigger, cmdValue) { result ->
            if (result.isFailure) {
                callback(Result.failure(result.exceptionOrNull()!!))

            } else {
                val cmd = result.getOrThrow()
                when (cmd.result) {
                    "SCANNER_ALREADY_DISABLED" -> callback(Result.failure(Error(cmd.result)))
                    "SCANNER_DISABLE_FAILED" -> callback(Result.failure(Error(cmd.result)))
                    else -> callback(Result.success(cmd.result))
                }
            }
        }


    }

    override fun unregisterForNotifications(): Unit {
        TODO("Not yet implemented")
    }


    // Tries to call set config with each of the known decoders set to false.
    override fun setDecoder(
        decoder: Decoder,
        enabled: Boolean,
        profileName: String,
        callback: (Result<Unit>) -> Unit
    ): Unit {


        val configBundle = Bundle()

        // Base config
        configBundle.putString("PROFILE_NAME", profileName)
        configBundle.putString("CONFIG_MODE", "UPDATE")

        val bConfig = Bundle()
        val bParams = Bundle()

        intentBool(bParams, decoderToString[decoder]!!, enabled)

        bParams.putString("scanner_selection_by_identifier","AUTO")

        bConfig.putString("PLUGIN_NAME", "BARCODE")
        bConfig.putBundle("PARAM_LIST", bParams)

        val plugins = arrayListOf<Bundle>(
            bConfig
        )

        configBundle.putParcelableArrayList("PLUGIN_CONFIG", plugins)

        sendCommand(DWCommand.SetConfig, configBundle) { res ->
            if (res.isFailure) {
                callback(Result.failure(res.exceptionOrNull()!!))

            } else {
                val cmd = res.getOrThrow()

                when (cmd.result) {
                    "SUCCESS" -> callback(Result.success(Unit))
                    else -> callback(Result.failure(Error(cmd.result)))
                }


            }
        }


    }


    override fun setProfileConfig(config: ProfileConfig, callback: (kotlin.Result<Unit>) -> Unit) {
        // We somehow need to convert the profile config to a bundle...

        val configBundle = Bundle()

        // Base config
        configBundle.putString("PROFILE_NAME", config.profileName)
        configBundle.putString("PROFILE_ENABLED", config.profileEnabled.toString())
        configBundle.putString(
            "CONFIG_MODE", when (config.configMode) {
                ConfigMode.CREATE_IF_NOT_EXISTS -> "CREATE_IF_NOT_EXIST"
                ConfigMode.UPDATE -> "UPDATE"
                ConfigMode.OVERWRITE -> "OVERWRITE"
                else -> "UNKNOWN"
            }
        )

        // Apps that this profile is active for
        if (config.appList != null) {

            configBundle.putParcelableArray("APP_LIST", config.appList.map { appEntry ->
                val app = Bundle()
                if (appEntry != null) {
                    app.putString("PACKAGE_NAME", appEntry.packageName)
                    app.putStringArray(
                        "ACTIVITY_LIST",
                        appEntry.activityList.map { e -> e!! }.toTypedArray()
                    )
                }
                app
            }.toTypedArray())
        }

        // Prepare a list of plugins that we will process. Maybe we will need to split this into
        // multiple calls in the future, because older version of DW don't support multiple plugins
        // at once
        val plugins = arrayListOf<Bundle>()

        // Intent parameters
        if (config.intentParamters != null) {
            plugins.add(buildIntentConfig(config.intentParamters))
        }

        // Barcode parameters
        if (config.barcodeParamters != null) {
            plugins.add(buildBarcodeConfig(config.barcodeParamters))
        }

        // Add all plugins
        configBundle.putParcelableArrayList("PLUGIN_CONFIG", plugins)


        sendCommand(DWCommand.SetConfig, configBundle) { res ->
            if (res.isFailure) {
                callback(Result.failure(res.exceptionOrNull()!!))

            } else {
                val cmd = res.getOrThrow()

                when (cmd.result) {
                    "SUCCESS" -> callback(Result.success(Unit))
                    else -> callback(Result.failure(Error(cmd.result)))
                }


            }
        }


    }


}

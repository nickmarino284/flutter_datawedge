package com.circuskitchens.flutter_datawedge

import android.util.Log
import androidx.annotation.NonNull
import com.circuskitchens.flutter_datawedge.pigeon.DataWedgeFlutterApi
import com.circuskitchens.flutter_datawedge.pigeon.DataWedgeHostApi
import io.flutter.embedding.engine.plugins.FlutterPlugin

class FlutterDatawedgePlugin : FlutterPlugin {

    private var dwInterface: DWInterface? = null
    private var flutterApi: DataWedgeFlutterApi? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d("FlutterDataWedgePlugin", "Attaching to engine...")

        try {
            // Initialize API and DWInterface
            flutterApi = DataWedgeFlutterApi(flutterPluginBinding.binaryMessenger)
            dwInterface = DWInterface(flutterPluginBinding.applicationContext, flutterApi!!)

            // Set up host API
            DataWedgeHostApi.setUp(flutterPluginBinding.binaryMessenger, dwInterface)

            // Register broadcast receiver
            dwInterface?.setupBroadcastReceiver()
            Log.d("FlutterDataWedgePlugin", "Broadcast receiver setup successfully.")
        } catch (e: Exception) {
            Log.e("FlutterDataWedgePlugin", "Error during engine attachment: ${e.message}", e)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d("FlutterDataWedgePlugin", "Detaching from engine...")

        try {
            // Dispose resources
            dwInterface?.dispose()
            Log.d("FlutterDataWedgePlugin", "Broadcast receiver disposed.")

            // Unset API and DWInterface
            DataWedgeHostApi.setUp(binding.binaryMessenger, null)
        } catch (e: Exception) {
            Log.e("FlutterDataWedgePlugin", "Error during cleanup: ${e.message}", e)
        } finally {
            dwInterface = null
            flutterApi = null
        }
    }
}

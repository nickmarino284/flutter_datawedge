package com.circuskitchens.flutter_datawedge_example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed. BootReceiver triggered.")
            // Add logic to initialize DataWedge or other services if needed
        }
    }
}

package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.os.Handler
import android.util.Log
import com.google.ar.core.ArCoreApk
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MethodCallHandlerImpl(private val activity: Activity, private val messenger: BinaryMessenger) : MethodChannel.MethodCallHandler {
    private var methodChannel: MethodChannel? = null
    private val UTILS_CHANNEL_NAME = "arcore_flutter_plugin/utils"

    init {
        methodChannel = MethodChannel(messenger, UTILS_CHANNEL_NAME)
        methodChannel?.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.i(ArcoreFlutterPlugin.TAG, "Method called: " + call.method)
        when (call.method) {
            "checkArCoreApkAvailability" -> {
                maybeEnableAr { available ->
                    Log.i(ArcoreFlutterPlugin.TAG, "checkArCoreApkAvailability handler")
                    result.success(available)
                }
            }
            "checkIfARCoreServicesInstalled" -> {
                isARServicesInstalled { isInstalled ->
                    Log.i(ArcoreFlutterPlugin.TAG, "checkIfARCoreServicesInstalled handler")
                    result.success(isInstalled)
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun maybeEnableAr(handler: (Boolean) -> Unit) {
        Log.i(ArcoreFlutterPlugin.TAG, "maybeEnableAr")
        val availability: ArCoreApk.Availability = ArCoreApk.getInstance().checkAvailability(activity)
        if (availability.isTransient) {
            // Re-query at 5Hz while compatibility is checked in the background.
            Handler().postDelayed({
                maybeEnableAr {
                    Log.i(ArcoreFlutterPlugin.TAG, "handler")
                    handler(it)
                }
            }, 200)
        } else handler(availability.isSupported)
    }

    private fun isARServicesInstalled(handler: (Boolean) -> Unit) {
        Log.i(ArcoreFlutterPlugin.TAG, "isARServicesInstalled")
        var isInstalled = false;
        try {
            val status = ArCoreApk.getInstance().requestInstall(activity, false)
            if (status == ArCoreApk.InstallStatus.INSTALLED) {
                isInstalled = true;
            }
        } catch (e: Exception) {
            Log.e(ArcoreFlutterPlugin.TAG, e.toString());
        } finally {
            handler(isInstalled)
        }
        
    }

    fun stopListening() {
        methodChannel?.setMethodCallHandler(null)
    }

}
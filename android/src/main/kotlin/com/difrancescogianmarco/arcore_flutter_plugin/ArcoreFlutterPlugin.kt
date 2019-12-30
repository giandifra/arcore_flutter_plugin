package com.difrancescogianmarco.arcore_flutter_plugin

import android.util.Log
import androidx.annotation.NonNull;
import androidx.annotation.Nullable
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class ArcoreFlutterPlugin : FlutterPlugin, ActivityAware {

    @Nullable
    private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null

    companion object {
        const val TAG = "ArCoreFlutterPlugin"

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            Log.i(TAG, "registerWith")
            registrar
                    .platformViewRegistry()
                    .registerViewFactory("arcore_flutter_plugin", ArCoreViewFactory(registrar.activity(), registrar.messenger()))
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onAttachedToEngine")
        this.flutterPluginBinding = flutterPluginBinding
    }

    override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {
        Log.i(TAG, "onDetachedFromEngine")
        this.flutterPluginBinding = null
    }


    override fun onDetachedFromActivity() {
        Log.i(TAG,"onDetachedFromActivity")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.i(TAG, "onReattachedToActivityForConfigChanges")
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.i(TAG, "onAttachedToActivity")
        flutterPluginBinding?.let {
            it.platformViewRegistry.registerViewFactory("arcore_flutter_plugin", ArCoreViewFactory(binding.activity,it.binaryMessenger))
        }

    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.i(TAG, "onDetachedFromActivityForConfigChanges")
        onDetachedFromActivity()
    }

}
package com.difrancescogianmarco.arcore_flutter_plugin

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

class ArcoreFlutterPlugin : FlutterPlugin, ActivityAware {

    @Nullable
    private var flutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    
    private var methodCallHandler: MethodCallHandlerImpl? = null

    companion object {
        const val TAG = "ArCoreFlutterPlugin"
        private const val CHANNEL_NAME = "arcore_flutter_plugin"
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = flutterPluginBinding
    }

    override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {
        this.flutterPluginBinding = null
    }


    override fun onDetachedFromActivity() {
        //TODO remove othen channel
        methodCallHandler?.stopListening()
        methodCallHandler = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        flutterPluginBinding?.platformViewRegistry?.registerViewFactory(CHANNEL_NAME, ArCoreViewFactory(binding.activity, flutterPluginBinding?.binaryMessenger!!))
        methodCallHandler = MethodCallHandlerImpl(
                binding.activity, flutterPluginBinding?.binaryMessenger!!)

    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }


}
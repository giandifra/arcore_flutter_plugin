package com.difrancescogianmarco.arcore_flutter_plugin

import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class ArcoreFlutterPlugin: FlutterPlugin{

    companion object {

        val TAG = "ArCoreFlutterPlugin"
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            registrar
                    .platformViewRegistry()
                    .registerViewFactory("arcore_flutter_plugin", ArCoreViewFactory(registrar.messenger()))
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "arcore_flutter_plugin")
        flutterPluginBinding.getPlatformViewRegistry().registerViewFactory("arcore_flutter_plugin", ArCoreViewFactory(flutterPluginBinding.getBinaryMessenger()))
    }
      override fun onDetachedFromEngine (p0: FlutterPlugin.FlutterPluginBinding) {}
}
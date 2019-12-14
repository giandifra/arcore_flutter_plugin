package com.difrancescogianmarco.arcore_flutter_plugin

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.PluginRegistry

class ArcoreFlutterPlugin: FlutterPlugin{

    companion object {

        val TAG = "ArCoreFlutterPlugin"
        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            registrar
                    .platformViewRegistry()
                    .registerViewFactory("arcore_flutter_plugin", ArCoreViewFactory(registrar.messenger()))
        }
    }

    override fun onAttachedToEngine (p0: FlutterPlugin.FlutterPluginBinding) {}
    override fun onDetachedFromEngine (p0: FlutterPlugin.FlutterPluginBinding) {}
}
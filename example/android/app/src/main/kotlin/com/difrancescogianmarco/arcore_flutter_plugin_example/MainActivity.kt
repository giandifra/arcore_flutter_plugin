package com.difrancescogianmarco.arcore_flutter_plugin_example

import android.os.Bundle

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import com.difrancescogianmarco.arcore_flutter_plugin.ArcoreFlutterPlugin;

class MainActivity: FlutterActivity() {
  
  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    flutterEngine.getPlugins().add(ArcoreFlutterPlugin());
  }
}

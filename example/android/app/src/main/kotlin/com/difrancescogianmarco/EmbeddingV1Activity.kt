package com.difrancescogianmarco.arcore_flutter_plugin_example

import android.os.Bundle;
import io.flutter.app.FlutterActivity;
import io.flutter.plugins.GeneratedPluginRegistrant;

class EmbeddingV1Activity : FlutterActivity() {

 override fun onCreate(savedInstanceState: Bundle?) {
   super.onCreate(savedInstanceState);
   GeneratedPluginRegistrant.registerWith(this);
 }
}

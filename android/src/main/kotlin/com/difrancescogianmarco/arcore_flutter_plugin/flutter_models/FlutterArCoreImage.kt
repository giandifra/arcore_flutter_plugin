package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

class FlutterArCoreImage(map: HashMap<String, *>) {

    val width: Int = map["width"] as Int
    val height: Int = map["height"] as Int
    val bytes: ByteArray = map["bytes"] as ByteArray

}
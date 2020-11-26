package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

class FlutterArCoreSceneSetup(map: Map<String, *>)  {


    val enableTapRecognizer: Boolean? = map["enableTapRecognizer"] as? Boolean
    val enableUpdateListener: Boolean? = map["enableUpdateListener"] as? Boolean
    val enablePlaneRenderer: Boolean? = map["enablePlaneRenderer"] as? Boolean

}
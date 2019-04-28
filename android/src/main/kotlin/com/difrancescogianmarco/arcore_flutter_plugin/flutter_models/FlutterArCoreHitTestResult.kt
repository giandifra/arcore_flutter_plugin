package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Quaternion

class FlutterArCoreHitTestResult(val distance: Float, val translation: FloatArray, val rotation: FloatArray) {

    fun toHashMap(): HashMap<String, Any> {
        val map: HashMap<String, Any> = HashMap<String, Any>()
        map["distance"] = distance.toDouble()
        map["pose"] = FlutterArCorePose(translation,rotation).toHashMap()
        return map
    }
}
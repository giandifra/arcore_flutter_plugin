package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

import com.difrancescogianmarco.arcore_flutter_plugin.utils.DecodableUtils
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

class FlutterArCoreNodeConfiguration(map: Map<String, *>) {
    
    val name: String = map["name"] as String
    
    //ScaleController
    val scaleEnabled = map["scaleGestureEnabled"] as? Boolean ?: true
    val minScale = map["minScale"] as? Float ?: 0.25F
    val maxScale = map["maxScale"] as? Float ?: 5.0F
    val currentScale: Vector3 = DecodableUtils.parseVector3(map["scale"] as? HashMap<String, *>)
            ?: Vector3(1.0F, 1.0F, 1.0F)
    
    //TranslationController
    val translationEnabled = map["translationGestureEnabled"] as? Boolean ?: true
    val currentPosition: Vector3 = DecodableUtils.parseVector3(map["position"] as? HashMap<String, *>) ?: Vector3()
    
    //RotationController
    val rotationEnabled = map["rotationGestureEnabled"] as? Boolean ?: true
    val currentRotation: Quaternion = DecodableUtils.parseQuaternion(map["rotation"] as? HashMap<String, Double>)
            ?: Quaternion()

}
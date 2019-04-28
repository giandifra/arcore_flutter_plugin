package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

import com.google.ar.sceneform.math.Quaternion

class FlutterArCoreHitTestResult(val distance: Float, val translation: FloatArray, val rotation: FloatArray) {


    fun toHashMap(): HashMap<String, Any> {
        val map: HashMap<String, Any> = HashMap<String, Any>()
        map["distance"] = distance.toDouble()
        map["translation"] = convertFloatArray(translation)
        map["rotation"] = convertFloatArray(rotation)
        return map
    }

    private fun convertFloatArray(array: FloatArray): DoubleArray {
        val doubleArray = DoubleArray(array.size)
        for ((i, a) in array.withIndex()) {
            doubleArray[i] = a.toDouble()
        }
        return doubleArray
    }

}
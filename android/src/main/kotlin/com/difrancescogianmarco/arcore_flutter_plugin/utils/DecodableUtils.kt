package com.difrancescogianmarco.arcore_flutter_plugin.utils

import android.util.Log
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import java.util.*

class DecodableUtils {

    companion object {
        fun parseVector3(vector: HashMap<String, *>?): Vector3? {
            if (vector != null) {
                val x: Float = (vector["x"] as Double).toFloat()
                val y: Float = (vector["y"] as Double).toFloat()
                val z: Float = (vector["z"] as Double).toFloat()
                return Vector3(x, y, z)
            }
            return null
        }

        fun parseQuaternion(vector: HashMap<String, Double>?): Quaternion? {
            if (vector != null) {
                val x: Float = (vector["x"] as Double).toFloat()
                val y: Float = (vector["y"] as Double).toFloat()
                val z: Float = (vector["z"] as Double).toFloat()
                val w: Float = (vector["w"] as Double).toFloat()
                return Quaternion(x, y, z, w)
            }
            return null
        }
    }
}
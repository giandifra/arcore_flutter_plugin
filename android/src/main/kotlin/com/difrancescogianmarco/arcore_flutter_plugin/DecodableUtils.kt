package com.difrancescogianmarco.arcore_flutter_plugin

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import java.util.*

class DecodableUtils {


    companion object {
        fun parseVector3(vector: HashMap<String, Any>): Vector3 {
            val x: Float = (vector["x"] as Double).toFloat()
            val y: Float = (vector["y"] as Double).toFloat()
            val z: Float = (vector["z"] as Double).toFloat()
            return Vector3(x,y,z)
        }

        fun parseVector4(vector: HashMap<String, Any>): Quaternion {
            val x: Float = (vector["x"] as Double).toFloat()
            val y: Float = (vector["y"] as Double).toFloat()
            val z: Float = (vector["z"] as Double).toFloat()
            val w: Float = (vector["w"] as Double).toFloat()

            return Quaternion(x,y,z,w)
        }
    }
}
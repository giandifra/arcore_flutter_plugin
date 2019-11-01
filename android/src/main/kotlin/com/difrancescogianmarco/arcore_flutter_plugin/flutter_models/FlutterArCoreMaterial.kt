package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

import android.graphics.Color

class FlutterArCoreMaterial(map: HashMap<String, *>) {

    val argb: ArrayList<Int>? = map["color"] as? ArrayList<Int>
    val color: Int? = getIntColor(argb)
    //    val texture: String? = map["texture"] as? String
    val textureBytes: ByteArray? = map["textureBytes"] as? ByteArray
    val metallic: Float? = (map["metallic"] as? Double)?.toFloat()
    val roughness: Float? = (map["roughness"] as? Double)?.toFloat()
    val reflectance: Float? = (map["reflectance"] as? Double)?.toFloat()

    private fun getIntColor(argb: ArrayList<Int>?): Int? {
        if (argb != null) {
            return Color.argb(argb[0], argb[1], argb[2], argb[3])
        }
        return null
    }

    override fun toString(): String {
        return "color: $color\nargb: $argb\n" +
                "textureBytesLength: ${textureBytes?.size}\n" +
                "metallic: $metallic\n" +
                "roughness: $roughness\n" +
                "reflectance: $reflectance"
    }
}
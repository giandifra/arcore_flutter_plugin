package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.graphics.Color
import android.util.Log
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import java.util.concurrent.CompletableFuture

class GeometryBuilder {

    companion object {

        fun createModelRenderable(activity: Activity, map: HashMap<String, Any>): ModelRenderable {
            val type = map["dartType"] as String

            val materials = map["materials"] as ArrayList<HashMap<String, Any>>
            val materialType: Int = (materials[0] as HashMap<String, Any>)["materialType"] as Int
            val rgb = materials[0]["color"] as ArrayList<Int>
            Log.i("GeometryBuilder", rgb.toString())
            val color = com.google.ar.sceneform.rendering.Color(Color.argb(255,rgb[0],rgb[1],rgb[2]))
            Log.i("GeometryBuilder",  rgb[0].toFloat().toString())
            Log.i("GeometryBuilder",  rgb[1].toFloat().toString())
            Log.i("GeometryBuilder",  rgb[2].toFloat().toString())
            val materialFactory = getMaterialFactory(activity, materialType, color)
            val material = materialFactory?.get()!!
            val renderable = getShapeType(type, map, material)!!
            return renderable

        }


        fun getMaterialFactory(activity: Activity, materialType: Int, color: com.google.ar.sceneform.rendering.Color): CompletableFuture<Material>? {
            Log.i("GeometryBuilder", "materialType : " + materialType)

            when (materialType) {
                0 -> {
                    return MaterialFactory.makeOpaqueWithColor(activity.applicationContext, color);
                }
                1 -> {
                    return MaterialFactory.makeTransparentWithColor(activity.applicationContext, color);
                }
                2 -> {
                    return MaterialFactory.makeOpaqueWithTexture(activity.applicationContext, null);
                }
                3 -> {
                    return MaterialFactory.makeTransparentWithTexture(activity.applicationContext, null);
                }
                else -> {
                    Log.i("GeometryBuilder", "Error Material Factory")
                    return null
                }
            }
        }

        fun getShapeType(type: String, map: HashMap<String, Any>, material: Material): ModelRenderable? {
            var modelRenderable: ModelRenderable?
            when (type) {
                "ArCoreSphere" -> {
                    val radius: Float = (map["radius"] as Double).toFloat()
                    return ShapeFactory.makeSphere(radius, Vector3(0.0f, 0.15f, 0.0f), material);
                }
//            "ArCorePlane" -> {
//
//            }
//            "ArCoreText" -> {
//
//            }
//            "ArCoreLine" -> {
//
//            }
                else -> {
                    return null
                }
            }

        }
    }
}
package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreMaterial
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.R
import com.google.ar.sceneform.rendering.Texture
import java.util.concurrent.CompletableFuture


@RequiresApi(api = 24)
class MaterialCustomFactory {
    companion object {
        val MATERIAL_COLOR = "color"
        val MATERIAL_TEXTURE = "texture"
        val MATERIAL_METALLIC = "metallic"
        val MATERIAL_ROUGHNESS = "roughness"
        val MATERIAL_REFLECTANCE = "reflectance"
        val TAG: String = MaterialCustomFactory::class.java.name

        fun makeWithColor(context: Context, flutterArCoreMaterial: FlutterArCoreMaterial): CompletableFuture<Material>? {
            if (flutterArCoreMaterial.argb != null) {
                if (flutterArCoreMaterial.argb[0] < 255) {
                    return makeTransparentWithColor(context, flutterArCoreMaterial)
                }
                return makeOpaqueWithColor(context, flutterArCoreMaterial)
            }
            return null
        }

        fun makeWithTexture(context: Context, texture: Texture, isPng: Boolean, flutterArCoreMaterial: FlutterArCoreMaterial): CompletableFuture<Material>? {
            if (isPng) {
                return makeTransparentWithTexture(context, texture, flutterArCoreMaterial)
            }
            return makeOpaqueWithTexture(context, texture, flutterArCoreMaterial)
        }

        fun makeOpaqueWithColor(context: Context, flutterArCoreMaterial: FlutterArCoreMaterial): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_opaque_colored_material).build()
            return materialFuture.thenApply { material ->
                material.setFloat3(MATERIAL_COLOR, flutterArCoreMaterial.color.toArColor())
                applyCustomPbrParams2(material, flutterArCoreMaterial)
                material
            }
        }

        fun makeTransparentWithColor(context: Context, flutterArCoreMaterial: FlutterArCoreMaterial): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_transparent_colored_material).build()
            return materialFuture.thenApply { material ->
                material.setFloat4(MATERIAL_COLOR, flutterArCoreMaterial.color.toArColor())
                applyCustomPbrParams2(material, flutterArCoreMaterial)
                material
            }
        }

        fun makeOpaqueWithTexture(context: Context, texture: Texture, flutterArCoreMaterial: FlutterArCoreMaterial): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_opaque_textured_material).build()
            return materialFuture.thenApply { material ->
                material.setTexture(MATERIAL_TEXTURE, texture)
                applyCustomPbrParams2(material, flutterArCoreMaterial)
                material
            }
        }

        fun makeTransparentWithTexture(context: Context, texture: Texture, flutterArCoreMaterial: FlutterArCoreMaterial): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_transparent_textured_material).build()
            return materialFuture.thenApply { material ->
                material.setTexture(MATERIAL_TEXTURE, texture)
                applyCustomPbrParams2(material, flutterArCoreMaterial)
                material
            }
        }

        fun updateMaterial(material: Material, map: HashMap<String, *>): Material {
            val color = map[MATERIAL_COLOR] as? ArrayList<Int>
            val metallic = (map[MATERIAL_METALLIC] as? Double)?.toFloat()
            val roughness = (map[MATERIAL_ROUGHNESS] as? Double)?.toFloat()
            val reflectance = (map[MATERIAL_REFLECTANCE] as? Double)?.toFloat()

            if (metallic != null) {
                material.setFloat(MATERIAL_METALLIC, metallic)
            }
            if (roughness != null) {
                material.setFloat(MATERIAL_ROUGHNESS, roughness)
            }
            if (reflectance != null) {
                material.setFloat(MATERIAL_REFLECTANCE, reflectance)
            }
            if (color != null) {
                material.setFloat3(MATERIAL_COLOR, getColor(map[MATERIAL_COLOR] as ArrayList<Int>))
            }

            return material
        }

        private fun getColor(rgb: ArrayList<Int>): Color {
            return Color(android.graphics.Color.argb(rgb[0], rgb[1], rgb[2], rgb[3]))
        }

        private fun applyCustomPbrParams2(material: Material, flutterArCoreMaterial: FlutterArCoreMaterial) {

            material.setFloat(MATERIAL_METALLIC, flutterArCoreMaterial.metallic / 100F)
            material.setFloat(MATERIAL_ROUGHNESS, flutterArCoreMaterial.roughness / 100F)
            material.setFloat(MATERIAL_REFLECTANCE, flutterArCoreMaterial.reflectance / 100F)
        }
    }
}

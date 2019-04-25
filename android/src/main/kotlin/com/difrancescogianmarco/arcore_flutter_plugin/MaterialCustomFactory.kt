package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
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
        private val DEFAULT_METALLIC_PROPERTY = 0.0f
        private val DEFAULT_ROUGHNESS_PROPERTY = 0.4f
        private val DEFAULT_REFLECTANCE_PROPERTY = 0.5f
        val TAG: String = MaterialCustomFactory::class.java.name


        fun makeWithColor(context: Context, map: HashMap<String, *>): CompletableFuture<Material>? {
            if (map[MATERIAL_COLOR] != null) {
                val color = map[MATERIAL_COLOR] as ArrayList<Int>
                if (color[0] < 255) {
                    return makeTransparentWithColor(context, map)
                }
                return makeOpaqueWithColor(context, map)
            }
            return null
        }

        fun makeWithTexture(context: Context, texture: Texture, isPng: Boolean): CompletableFuture<Material>? {
            if (isPng) {
                return makeTransparentWithTexture(context, texture)
            }
            return makeOpaqueWithTexture(context, texture)
        }


        fun makeOpaqueWithColor(context: Context, map: HashMap<String, *>): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_opaque_colored_material).build()
            return materialFuture.thenApply { material ->
                material.setFloat3(MATERIAL_COLOR, getColor(map[MATERIAL_COLOR] as ArrayList<Int>))
                applyCustomPbrParams(material, map as HashMap<String, Double>)
                material
            }
        }

        fun makeTransparentWithColor(context: Context, map: HashMap<String, *>): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_transparent_colored_material).build()
            return materialFuture.thenApply { material ->
                material.setFloat4(MATERIAL_COLOR, getColor(map[MATERIAL_COLOR] as ArrayList<Int>))
                applyCustomPbrParams(material, map as HashMap<String, Double>)
                material
            }
        }

        fun makeOpaqueWithTexture(context: Context, texture: Texture): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_opaque_textured_material).build()
            return materialFuture.thenApply { material ->
                material.setTexture(MATERIAL_TEXTURE, texture)
                applyDefaultPbrParams(material)
                material
            }
        }

        fun makeTransparentWithTexture(context: Context, texture: Texture): CompletableFuture<Material> {
            val materialFuture = Material.builder().setSource(context, R.raw.sceneform_transparent_textured_material).build()
            return materialFuture.thenApply { material ->
                material.setTexture(MATERIAL_TEXTURE, texture)
                applyDefaultPbrParams(material)
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

        private fun applyCustomPbrParams(material: Material, map: HashMap<String, Double>) {

            Log.i(TAG, "MATERIAL_METALLIC: ${map[MATERIAL_METALLIC]}")
            val metallic = map[MATERIAL_METALLIC]?.toFloat()
            Log.i(TAG, "MATERIAL_ROUGHNESS: ${map[MATERIAL_ROUGHNESS]}")
            val roughness = map[MATERIAL_ROUGHNESS]?.toFloat()
            Log.i(TAG, "MATERIAL_REFLECTANCE: ${map[MATERIAL_REFLECTANCE]}")
            val reflectance = map[MATERIAL_REFLECTANCE]?.toFloat()

            material.setFloat(MATERIAL_METALLIC, metallic ?: DEFAULT_METALLIC_PROPERTY)
            material.setFloat(MATERIAL_ROUGHNESS, roughness ?: DEFAULT_ROUGHNESS_PROPERTY)
            material.setFloat(MATERIAL_REFLECTANCE, reflectance ?: DEFAULT_REFLECTANCE_PROPERTY)
        }

        private fun applyDefaultPbrParams(material: Material) {
            material.setFloat("metallic", 0.0f)
            material.setFloat("roughness", 0.4f)
            material.setFloat("reflectance", 0.5f)
        }
    }
}

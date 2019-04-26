package com.difrancescogianmarco.arcore_flutter_plugin

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ModelRenderable

typealias MaterialHandler = (Material?, Throwable?) -> Unit
typealias RenderableHandler = (ModelRenderable?, Exception?) -> Unit

class RenderableCustomFactory {

    companion object {

        val TAG = RenderableCustomFactory::class.java.name
        @SuppressLint("ShowToast")
        fun makeRenderable(context: Context, flutterArCoreNode: FlutterArCoreNode, handler: RenderableHandler) {
            makeMaterial(context, flutterArCoreNode) { material, throwable ->
                if (material != null) {
                    Log.i(TAG, "material not null")
                    try {
                        val renderable = flutterArCoreNode.shape?.buildShape(material)
                        handler(renderable, null)
                    } catch (ex: Exception) {
                        Log.i(TAG, "renderable error ${ex}")
                        handler(null, ex)
                        Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG)
                    }
                }
            }
        }

        private fun makeMaterial(context: Context, flutterArCoreNode: FlutterArCoreNode, handler: MaterialHandler) {
            val texture = flutterArCoreNode.shape?.materials?.first()?.texture
            val color = flutterArCoreNode.shape?.materials?.first()?.color
            if (texture != null) {
                val isPng = texture.endsWith("png")
                val builder = com.google.ar.sceneform.rendering.Texture.builder();
                builder.setSource(context, Uri.parse(texture))
                builder.build().thenAccept { texture ->
                    MaterialCustomFactory.makeWithTexture(context, texture, isPng, flutterArCoreNode.shape.materials[0])?.thenAccept { material ->
                        handler(material, null)
                    }?.exceptionally { throwable ->
                        Log.i(TAG, "texture error ${throwable}")
                        handler(null, throwable)
                        return@exceptionally null
                    }
                }
            } else if (color != null) {
                MaterialCustomFactory.makeWithColor(context, flutterArCoreNode.shape.materials[0])
                        ?.thenAccept { material: Material ->
                            handler(material, null)
                        }?.exceptionally { throwable ->
                            Log.i(TAG, "material error ${throwable}")
                            handler(null, throwable)
                            return@exceptionally null
                        }
            }
        }
    }
}
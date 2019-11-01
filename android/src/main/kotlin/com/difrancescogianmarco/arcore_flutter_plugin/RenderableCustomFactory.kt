package com.difrancescogianmarco.arcore_flutter_plugin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ModelRenderable

typealias MaterialHandler = (Material?, Throwable?) -> Unit
typealias RenderableHandler = (ModelRenderable?, Throwable?) -> Unit

class RenderableCustomFactory {

    companion object {

        val TAG = RenderableCustomFactory::class.java.name
        @SuppressLint("ShowToast")
        fun makeRenderable(context: Context, flutterArCoreNode: FlutterArCoreNode, handler: RenderableHandler) {


            if (flutterArCoreNode.dartType == "ArCoreReferenceNode") {

                val url = flutterArCoreNode.objectUrl

                val localObject = flutterArCoreNode.obcject3DFileName

                if (localObject != null) {
                    val builder = ModelRenderable.builder()
                    builder.setSource(context, Uri.parse(localObject))
                    builder.build().thenAccept { renderable ->
                        handler(renderable, null)
                    }.exceptionally { throwable ->
                        Log.e(TAG, "Unable to load Renderable.", throwable);
                        handler(null, throwable)
                        return@exceptionally null
                    }
                } else if (url != null) {
                    val modelRenderableBuilder = ModelRenderable.builder()
                    val renderableSourceBuilder = RenderableSource.builder()

                    renderableSourceBuilder
                            .setSource(context, Uri.parse(url), RenderableSource.SourceType.GLTF2)
                            .setScale(0.5f)
                            .setRecenterMode(RenderableSource.RecenterMode.ROOT)

                    modelRenderableBuilder
                            .setSource(context, renderableSourceBuilder.build())
                            .setRegistryId(url)
                            .build()
                            .thenAccept { renderable ->
                                handler(renderable, null)
                            }
                            .exceptionally { throwable ->
                                handler(null, throwable)
                                Log.i(TAG, "renderable error ${throwable.localizedMessage}")
                                null
                            }
                }

            } else {
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
        }

        private fun makeMaterial(context: Context, flutterArCoreNode: FlutterArCoreNode, handler: MaterialHandler) {
//            val texture = flutterArCoreNode.shape?.materials?.first()?.texture
            val textureBytes = flutterArCoreNode.shape?.materials?.first()?.textureBytes
            val color = flutterArCoreNode.shape?.materials?.first()?.color
            if (textureBytes != null) {
//                val isPng = texture.endsWith("png")
                val isPng = true

                val builder = com.google.ar.sceneform.rendering.Texture.builder();
//                builder.setSource(context, Uri.parse(texture))
                builder.setSource(BitmapFactory.decodeByteArray(textureBytes, 0, textureBytes.size))
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
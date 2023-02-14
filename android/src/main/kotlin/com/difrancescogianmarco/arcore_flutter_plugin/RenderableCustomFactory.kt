package com.difrancescogianmarco.arcore_flutter_plugin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
//import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import java.util.function.Consumer

import android.widget.RelativeLayout.LayoutParams;

typealias MaterialHandler = (Material?, Throwable?) -> Unit
typealias RenderableHandler = (Renderable?, Throwable?) -> Unit

class RenderableCustomFactory {

    companion object {

        val TAG = "RenderableCustomFactory"

        @SuppressLint("ShowToast")
        fun makeRenderable(
            context: Context,
            flutterArCoreNode: FlutterArCoreNode,
            handler: RenderableHandler
        ) {

            if (flutterArCoreNode.dartType == "ArCoreReferenceNode") {

                val url = flutterArCoreNode.objectUrl

                val localObject = flutterArCoreNode.object3DFileName
                if (localObject != null) {
                    val builder = ModelRenderable.builder()
                    builder.setSource(context, Uri.parse(localObject))
                        .setIsFilamentGltf(true)
                    builder.build().thenAccept { renderable ->
                        handler(renderable, null)
                    }.exceptionally { throwable ->
                        Log.e(TAG, "Unable to load Renderable.", throwable);
                        handler(null, throwable)
                        return@exceptionally null
                    }
                } else if (url != null) {
                    ModelRenderable.builder()
                        .setSource(context, Uri.parse(url))
                        .setIsFilamentGltf(true)
                        .setAsyncLoadEnabled(false)
                        //.setRegistryId(url)
                        .build()
                        .thenAccept { renderable ->
                            handler(renderable, null)
                        }
                        .exceptionally { throwable ->
                            handler(null, throwable)
                            Log.e(TAG, "renderable error ${throwable.localizedMessage}")
                            null
                        }
                }

            } else {

                if (flutterArCoreNode.image != null) {
                    val image = ImageView(context);
                    image.layoutParams =
                        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                    val bmp = BitmapFactory.decodeByteArray(
                        flutterArCoreNode.image.bytes,
                        0,
                        flutterArCoreNode.image.bytes.size
                    )

                    image.setImageBitmap(
                        Bitmap.createScaledBitmap(
                            bmp, flutterArCoreNode.image.width,
                            flutterArCoreNode.image.height, false
                        )
                    )

                    ViewRenderable.builder().setView(context, image)
                        .build()
                        .thenAccept(Consumer { renderable: ViewRenderable ->
                            handler(
                                renderable,
                                null
                            )
                        })
                        .exceptionally { throwable ->
                            Log.e(TAG, "Unable to load image renderable.", throwable);
                            handler(null, throwable)
                            return@exceptionally null
                        }
                } else if (flutterArCoreNode.video != null) {

                } else {
                    makeMaterial(context, flutterArCoreNode) { material, throwable ->
                        if (throwable != null) {
                            handler(null, throwable)
                            return@makeMaterial
                        }
                        if (material == null) {
                            handler(null, null)
                            return@makeMaterial
                        }
                        try {
                            val renderable = flutterArCoreNode.shape?.buildShape(material)
                            handler(renderable, null)
                        } catch (ex: Exception) {
                            Log.e(TAG, "renderable error ${ex}")
                            handler(null, ex)
                            Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG)
                        }
                    }


                }

            }
        }

        private fun makeMaterial(
            context: Context,
            flutterArCoreNode: FlutterArCoreNode,
            handler: MaterialHandler
        ) {
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
                    MaterialCustomFactory.makeWithTexture(
                        context,
                        texture,
                        isPng,
                        flutterArCoreNode.shape.materials[0]
                    )?.thenAccept { material ->
                        handler(material, null)
                    }?.exceptionally { throwable ->
                        Log.e(TAG, "texture error ${throwable}")
                        handler(null, throwable)
                        return@exceptionally null
                    }
                }
            } else if (color != null) {
                MaterialCustomFactory.makeWithColor(context, flutterArCoreNode.shape.materials[0])
                    ?.thenAccept { material: Material ->
                        handler(material, null)
                    }?.exceptionally { throwable ->
                        Log.e(TAG, "material error ${throwable}")
                        handler(null, throwable)
                        return@exceptionally null
                    }
            } else {
                handler(null, null)
            }
        }
    }
}
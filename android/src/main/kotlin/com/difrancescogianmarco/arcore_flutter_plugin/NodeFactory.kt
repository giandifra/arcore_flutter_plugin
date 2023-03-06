package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.google.ar.sceneform.ux.VideoNode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

typealias NodeHandler = (Node?, Throwable?) -> Unit

class NodeFactory {

    companion object {
        val TAG: String = NodeFactory::class.java.name

        fun makeNode(
            context: Context,
            flutterNode: FlutterArCoreNode,
            debug: Boolean,
            handler: NodeHandler
        ) {
            if (debug) {
                Log.i(TAG, flutterNode.toString())
            }
            val node = flutterNode.buildNode()
            RenderableCustomFactory.makeRenderable(context, flutterNode) { renderable, t ->
                if (renderable != null) {
                    node.renderable = renderable
                    handler(node, null)
                } else {
                    handler(null, t)
                }
            }
        }

        fun createVideoNode2(
            activity: Context,
            arSceneView: ArSceneView,
            anchorNode: AnchorNode,
            player: MediaPlayer,
            transformationSystem: TransformationSystem,
            byteArray: ByteArray?,
            extentX: Float,
            extentZ: Float,
            localPosition: Vector3,
            localRotation: Quaternion,
            localScale: Vector3,
            plainVideoModel: Renderable,
            plainVideoMaterial: Material
        ) {
            // AnchorNode placed to the detected tag and set it to the real size of the tag
            // This will cause deformation if your AR tag has different aspect ratio than your video
            // AnchorNode placed to the detected tag and set it to the real size of the tag
            // This will cause deformation if your AR tag has different aspect ratio than your video
            anchorNode.worldScale = Vector3(
                extentX,
                1f,
                extentZ,
            )
            anchorNode.parent = arSceneView.scene

            val videoNode = TransformableNode(transformationSystem)
            // For some reason it is shown upside down so this will rotate it correctly
            // For some reason it is shown upside down so this will rotate it correctly
            videoNode.localRotation =
                Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f)
            anchorNode.addChild(videoNode)

            // Setting texture

            // Setting texture
            val externalTexture = ExternalTexture()
            val renderableInstance = videoNode.setRenderable(plainVideoModel)
            renderableInstance.material = plainVideoMaterial

            // Setting MediaPLayer
            renderableInstance.material.setExternalTexture("videoTexture", externalTexture)

            player.isLooping = true
            player.setSurface(externalTexture.surface)
            player.prepare()
            player.start()
        }

        fun createVideoNode(
            activity: Context,
            arSceneView: ArSceneView,
            anchorNode: AnchorNode,
            player: MediaPlayer,
            transformationSystem: TransformationSystem,
            byteArray: ByteArray?,
            extentX: Float,
            extentZ: Float,
            localPosition: Vector3,
            localRotation: Quaternion,
            localScale: Vector3,
        ) {

            //anchorNode.name = flutterArCoreNode.name
            anchorNode.parent = arSceneView.scene
            //anchorNode.localScale = Vector3(
            //    extentX,
            //    1f,
            //    extentZ
            //)

            // From bytes
            val filename = "video_" + anchorNode.name
            Log.i(TAG, "temp file: $filename")
            val temp = File.createTempFile(filename, "mp4", activity.cacheDir)
            temp.deleteOnExit()
            val fos = FileOutputStream(temp)
            fos.write(byteArray)
            fos.close()

            val file = FileInputStream(temp)
            player.setDataSource(file.fd)
            player.prepare()

            val modelNode = TransformableNode(transformationSystem)
            //modelNode.localPosition = Vector3(0f,0f,player.videoHeight.toFloat()/2)
            modelNode.localRotation = localRotation
            modelNode.parent = anchorNode

            /*scaleNode(
                modelNode,
                player.videoWidth.toFloat(),
                player.videoHeight.toFloat(),
                extentX,
                extentZ,
                VideoScaleType.CenterInside
            )*/
            //rotateNode(modelNode, -90f)


            // from android assets
            /*val afd: AssetFileDescriptor = activity.assets.openFd("sintel.mp4")
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            player.isLooping = true
            player.prepare();
            player.start()
*/
            // From url
            /*player.setDataSource(
                activity,
                Uri.parse("https://github.com/SceneView/sceneform-android/blob/master/samples/video-texture/src/main/res/raw/sintel.mp4?raw=true")
            )
            player.isLooping = true
            player.setOnPreparedListener {
                Log.i(TAG, "video player prepared")
                player.start()
            };
            player.setOnErrorListener { player, v1, v2 ->
                Log.e("on create video node","error")
                true
            }
            player.prepareAsync();
             */


            player.start()

            val videoNode = CustomVideoNode(
                activity, player,
                object : VideoNode.Listener {
                    override fun onCreated(videoNode: VideoNode) {
                        Log.i(TAG, "video node created")
                    }

                    override fun onError(throwable: Throwable) {
                        Log.d(TAG, throwable.toString())
                        Toast.makeText(activity, "Unable to load material", Toast.LENGTH_LONG)
                            .show()
                    }
                },
                extentX,
                extentZ
            )
            //old code
            //videoNode.localPosition = Vector3(0f, 0f, 0.25f)
            //videoNode.localRotation = Quaternion.axisAngle(Vector3(0f, 1f, -1f), 180f)


            //videoNode.localRotation = localRotation
            //videoNode.localScale = localScale

            scaleNode2(
                videoNode,
                player.videoWidth.toFloat(),
                player.videoHeight.toFloat(),
                extentX,
                extentZ,
                VideoScaleType.CenterInside
            )
            modelNode.localPosition = Vector3(0f,0f,videoNode.localScale.y/2)

            Log.i("videoNode.localScale", videoNode.localScale.toString());
            Log.i("Video", "width: " + player.videoWidth + " extentX: " + extentX);
            Log.i("Video", "height: " + player.videoHeight + " extentZ: " + extentZ);
            videoNode.parent = modelNode

            // If you want that the VideoNode is always looking to the
            // Camera (You) comment the next line out. Use it mainly
            // if you want to display a Video. The use with activated
            // ChromaKey might look odd.
            //videoNode.setRotateAlwaysToCamera(true);

            //modelNode.select()
            //return anchorNode
        }

        private fun rotateNode(videoNode: Node, videoRotation: Float) {
            videoNode.localRotation =
                Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), videoRotation)
        }

        private fun scaleNode(
            videoNode: Node,
            videoWidth: Float, videoHeight: Float,
            imageWidth: Float, imageHeight: Float,
            videoScaleType: VideoScaleType,
        ) {
            Log.i(
                "ScaleNode",
                "videoWidth: $videoWidth videoHeight: $videoHeight imageWidth $imageWidth imageHeight $imageHeight"
            )
            videoNode.localScale = when (videoScaleType) {
                VideoScaleType.FitXY -> scaleFitXY(imageWidth, imageHeight)
                VideoScaleType.CenterCrop -> scaleCenterCrop(
                    videoWidth,
                    videoHeight,
                    imageWidth,
                    imageHeight
                )
                VideoScaleType.CenterInside -> scaleCenterInside(
                    videoWidth,
                    videoHeight,
                    imageWidth,
                    imageHeight
                )
            }
            Log.i(
                "ScaleNode",
                videoNode.localScale.toString()
            )
        }

        private fun scaleNode2(
            videoNode: Node,
            videoWidth: Float, videoHeight: Float,
            imageWidth: Float, imageHeight: Float,
            videoScaleType: VideoScaleType,
        ) {
            Log.i(
                "ScaleNode",
                "videoWidth: $videoWidth videoHeight: $videoHeight imageWidth $imageWidth imageHeight $imageHeight"
            )
            var v3 = when (videoScaleType) {
                VideoScaleType.FitXY -> scaleFitXY(imageWidth, imageHeight)
                VideoScaleType.CenterCrop -> scaleCenterCrop(
                    videoWidth,
                    videoHeight,
                    imageWidth,
                    imageHeight
                )
                VideoScaleType.CenterInside -> scaleCenterInside2(
                    videoWidth,
                    videoHeight,
                    imageWidth,
                    imageHeight
                )
            }
            Log.i(
                "scaleNode2",
                v3.toString()
            )

            videoNode.localScale = v3
            Log.i(
                "ScaleNode",
                videoNode.localScale.toString()
            )
        }

        private fun scaleFitXY(imageWidth: Float, imageHeight: Float): Vector3 {
            return Vector3(imageWidth, 1.0f, imageHeight)
        }

        private fun scaleCenterCrop(
            videoWidth: Float,
            videoHeight: Float,
            imageWidth: Float,
            imageHeight: Float
        ): Vector3 {
            val isVideoVertical = videoHeight > videoWidth
            val videoAspectRatio =
                if (isVideoVertical) videoHeight / videoWidth else videoWidth / videoHeight
            val imageAspectRatio =
                if (isVideoVertical) imageHeight / imageWidth else imageWidth / imageHeight

            return if (isVideoVertical) {
                if (videoAspectRatio > imageAspectRatio) {
                    Vector3(imageWidth, 1.0f, imageWidth * videoAspectRatio)
                } else {
                    Vector3(imageHeight / videoAspectRatio, 1.0f, imageHeight)
                }
            } else {
                if (videoAspectRatio > imageAspectRatio) {
                    Vector3(imageHeight * videoAspectRatio, 1.0f, imageHeight)
                } else {
                    Vector3(imageWidth, 1.0f, imageWidth / videoAspectRatio)
                }
            }
        }

        private fun scaleCenterInside(
            videoWidth: Float,
            videoHeight: Float,
            imageWidth: Float,
            imageHeight: Float
        ): Vector3 {
            val isVideoVertical = videoHeight > videoWidth
            val videoAspectRatio =
                if (isVideoVertical) videoHeight / videoWidth else videoWidth / videoHeight
            val imageAspectRatio =
                if (isVideoVertical) imageHeight / imageWidth else imageWidth / imageHeight

            return if (isVideoVertical) {
                if (videoAspectRatio < imageAspectRatio) {
                    Vector3(imageWidth, 1.0f, imageWidth * videoAspectRatio)
                } else {
                    Vector3(imageHeight / videoAspectRatio, 1.0f, imageHeight)
                }
            } else {
                if (videoAspectRatio < imageAspectRatio) {
                    Vector3(imageHeight * videoAspectRatio, 1.0f, imageHeight)
                } else {
                    Vector3(imageWidth, 1.0f, imageWidth / videoAspectRatio)
                }
            }
        }

        private fun scaleCenterInside2(
            videoWidth: Float,
            videoHeight: Float,
            imageWidth: Float,
            imageHeight: Float
        ): Vector3 {
            val isVideoVertical = videoHeight > videoWidth
            val videoAspectRatio =
                if (isVideoVertical) videoHeight / videoWidth else videoWidth / videoHeight
            val imageAspectRatio =
                if (isVideoVertical) imageHeight / imageWidth else imageWidth / imageHeight

            return if (isVideoVertical) {
                Log.i("scaleCenterInside2", "video is vertical");

                if (videoAspectRatio < imageAspectRatio) {
                    Log.i(
                        "scaleCenterInside2",
                        "videoAspectRatio < imageAspectRatio $videoAspectRatio < $imageAspectRatio"
                    );
                    //Vector3(imageWidth, imageWidth * videoAspectRatio, 1.0f)
                } else {
                    Log.i(
                        "scaleCenterInside2",
                        "videoAspectRatio >= imageAspectRatio $videoAspectRatio >= $imageAspectRatio"
                    );
                    //Vector3(imageHeight / videoAspectRatio, imageHeight, 1.0f)
                }
                Vector3(imageWidth * videoAspectRatio, imageHeight, 1.0f)
            } else {
                Log.i("scaleCenterInside2", "video is horizontal");

                if (videoAspectRatio < imageAspectRatio) {
                    Log.i(
                        "scaleCenterInside2",
                        "videoAspectRatio < imageAspectRatio $videoAspectRatio < $imageAspectRatio"
                    );
                    //Vector3(imageHeight * videoAspectRatio, imageHeight, 1.0f)
                } else {
                    Log.i(
                        "scaleCenterInside2",
                        "videoAspectRatio >= imageAspectRatio $videoAspectRatio > $imageAspectRatio"
                    );
                    //Vector3(imageWidth, imageWidth / videoAspectRatio, 1.0f)
                }
                Vector3(imageWidth, imageHeight * videoAspectRatio, 1.0f)
            }
        }
    }

    enum class VideoScaleType {
        FitXY, CenterCrop, CenterInside
    }

}

/*
class StreamMediaDataSource(var data: Stream) : MediaDataSource() {
    override fun close() {
        if (data != null) {
            data.Dispose();
            data = null;
        }
    }

    override fun readAt(p0: Long, p1: ByteArray?, p2: Int, p3: Int): Int {
        data.Seek(p0, System.IO.SeekOrigin.Begin);
        return data.Read(buffer, offset, size);
    }

    override fun getSize(): Long {
        return data.Length;
    }
}*/


class CustomVideoNode(
    context: Context?,
    player: MediaPlayer,
    listener: Listener?,
    val extentX: Float,
    val extentZ: Float
) :
    VideoNode(context, player, listener) {

    override fun createModel(player: MediaPlayer, material: Material?): Renderable {
        val width = player.videoWidth
        val height = player.videoHeight
        val x: Float
        val y: Float
        if (width >= height) {
            x = 1.0f
            y = (height.toFloat() / width.toFloat())
        } else {
            x = (width.toFloat() / height.toFloat()) //* extentX
            y = 1.0f
        }
        Log.i("CustomVideoNode", "createModel => x : $x y: $y")
        return makePlane(x, y, material)
    }

    override fun makePlane(width: Float, height: Float, material: Material?): Renderable {
        Log.i("CustomVideoNode", "makePlane => width : $width height: $height")

        return super.makePlane(width, height, material)
    }
}


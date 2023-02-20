package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.google.ar.sceneform.ux.VideoNode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.stream.Stream


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

        fun createVideoNode(
            activity: Context,
            arSceneView: ArSceneView,
            anchorNode: AnchorNode,
            player: MediaPlayer,
            transformationSystem: TransformationSystem,
            byteArray: ByteArray?
        ) {

            //anchorNode.name = flutterArCoreNode.name
            anchorNode.parent = arSceneView.scene

            val modelNode = TransformableNode(transformationSystem)
            modelNode.parent = anchorNode
            //modelNode.localScale = Vector3(augmentedImage.extentX, 1f, augmentedImage.extentZ)

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
            player.start()

            val videoNode = VideoNode(activity, player, null, object : VideoNode.Listener {
                override fun onCreated(videoNode: VideoNode) {
                    Log.i(TAG, "video node created")
                }

                override fun onError(throwable: Throwable) {
                    Log.d(TAG, throwable.toString())
                    Toast.makeText(activity, "Unable to load material", Toast.LENGTH_LONG)
                        .show()
                }
            })
            videoNode.localPosition = Vector3(0f, 0f, 0.25f)
            videoNode.localRotation =
                Quaternion.axisAngle(Vector3(0f, 1f, -1f), 180f)
            videoNode.parent = modelNode

            // If you want that the VideoNode is always looking to the
            // Camera (You) comment the next line out. Use it mainly
            // if you want to display a Video. The use with activated
            // ChromaKey might look odd.
            //videoNode.setRotateAlwaysToCamera(true);

            //modelNode.select()
            //return anchorNode
        }
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
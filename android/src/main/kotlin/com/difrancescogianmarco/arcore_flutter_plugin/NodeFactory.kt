package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.google.ar.sceneform.ux.VideoNode


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
            augmentedImage:AugmentedImage
        ) {

            //anchorNode.name = flutterArCoreNode.name
            anchorNode.parent = arSceneView.scene

            val modelNode = TransformableNode(transformationSystem)
            modelNode.parent = anchorNode
            //modelNode.localScale = Vector3(augmentedImage.extentX, 1f, augmentedImage.extentZ)


            //debugLog("addNodeWithAnchor inserted ${anchorNode.name}")
            //val rawId = R.raw.sintel
            //val player: MediaPlayer = MediaPlayer.create(activity, rawId)
            //val player = MediaPlayer()

            val afd: AssetFileDescriptor = activity.assets.openFd("sintel.mp4")
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            player.isLooping = true
            player.prepare();
            player.start()
            //mediaPlayers.add(player)
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
            videoNode.localPosition = Vector3(0f,0f,0.25f)
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
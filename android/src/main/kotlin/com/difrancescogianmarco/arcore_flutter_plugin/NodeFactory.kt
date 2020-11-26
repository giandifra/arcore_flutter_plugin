package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.util.Log
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem

typealias NodeHandler = (Node?, Throwable?) -> Unit
typealias TransformableNodeHandler = (BaseNode?, Throwable?) -> Unit

class NodeFactory {

    companion object {
        val TAG: String = NodeFactory::class.java.name

        fun makeNode(context: Context, flutterNode: FlutterArCoreNode, debug: Boolean,  handler: NodeHandler) {
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

        fun makeTransformableNode(context: Context, flutterNode: FlutterArCoreNode, debug: Boolean, transformationSystem: Coordinator, handler: TransformableNodeHandler) {
            if (debug) {
                Log.i(TAG, flutterNode.toString())
            }
            val node = flutterNode.buildTransformableNode(transformationSystem)
            RenderableCustomFactory.makeRenderable(context, flutterNode) { renderable, t ->
                if (renderable != null) {
                    node.renderable = renderable
                    handler(node, null)
                } else {
                    handler(null, t)
                }
            }
        }
    }
}
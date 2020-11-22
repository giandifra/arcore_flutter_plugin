package com.difrancescogianmarco.arcore_flutter_plugin

import android.content.Context
import android.util.Log
import com.difrancescogianmarco.arcore_flutter_plugin.flutter_models.FlutterArCoreNode
import com.google.ar.sceneform.Node

typealias NodeHandler = (Node?, Throwable?) -> Unit

class NodeFactory {

    companion object {
        val TAG: String = NodeFactory::class.java.name

        fun makeNode(context: Context, flutterNode: FlutterArCoreNode, debug: Boolean, handler: NodeHandler) {
            if (debug) {
                Log.i(TAG, flutterNode.toString())
            }
            val node = flutterNode.buildNode()
            RenderableCustomFactory.makeRenderable(context, flutterNode) { renderable, t ->
                if (renderable != null) {
                    node.renderable = renderable
                    handler(node, null)
                }else{
                    handler(null,t)
                }
            }
        }
    }
}
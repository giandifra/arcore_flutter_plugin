package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

import com.difrancescogianmarco.arcore_flutter_plugin.BaseNode
import com.difrancescogianmarco.arcore_flutter_plugin.Coordinator
import com.difrancescogianmarco.arcore_flutter_plugin.models.RotatingNode
import com.google.ar.core.Pose
import com.google.ar.sceneform.Node

class FlutterArCoreNode(map: HashMap<String, *>) {

    val dartType: String = map["dartType"] as String
    val image: FlutterArCoreImage? = createArCoreImage(map["image"] as? HashMap<String, *>)
    val objectUrl: String? = map["objectUrl"] as? String
    val object3DFileName: String? = map["object3DFileName"] as? String
    val shape: FlutterArCoreShape? = getShape(map["shape"] as? HashMap<String, *>)
    private val degreesPerSecond: Float? = getDegreesPerSecond((map["degreesPerSecond"] as? Double))
    var parentNodeName: String? = map["parentNodeName"] as? String
    private val configuration: FlutterArCoreNodeConfiguration = FlutterArCoreNodeConfiguration(map)
    val children: ArrayList<FlutterArCoreNode> = getChildrenFromMap(map["children"] as ArrayList<HashMap<String, *>>)

    private fun getChildrenFromMap(list: ArrayList<HashMap<String, *>>): ArrayList<FlutterArCoreNode> {
        return ArrayList(list.map { map -> FlutterArCoreNode(map) })
    }

    fun buildNode(): Node {
        val node: Node = if (degreesPerSecond != null) {
            RotatingNode(degreesPerSecond, true, 0.0f)
        } else {
            Node()
        }

        node.name = configuration.name
        node.localPosition = configuration.currentPosition
        node.localScale = configuration.currentScale
        node.localRotation = configuration.currentRotation

        return node
    }

    fun buildTransformableNode(transformationSystem: Coordinator): BaseNode {
        return BaseNode(transformationSystem, configuration)
    }

    fun getPosition(): FloatArray {
        return floatArrayOf(configuration.currentPosition.x, configuration.currentPosition.y, configuration.currentPosition.z)
    }

    fun getRotation(): FloatArray {
        return floatArrayOf(configuration.currentRotation.x, configuration.currentRotation.y, configuration.currentRotation.z, configuration.currentRotation.w)
    }

    fun getPose(): Pose {
        return Pose(getPosition(), getRotation())
    }

    private fun getDegreesPerSecond(degreesPerSecond: Double?): Float? {
        if (dartType == "ArCoreRotatingNode" && degreesPerSecond != null) {
            return degreesPerSecond.toFloat()
        }
        return null
    }

    private fun createArCoreImage(map: HashMap<String, *>?): FlutterArCoreImage? {
        if (map != null)
            return FlutterArCoreImage(map);

        return null;
    }

    private fun getShape(map: HashMap<String, *>?): FlutterArCoreShape? {
        if (map != null) {
            return FlutterArCoreShape(map)
        }
        return null
    }

    override fun toString(): String {
        return "dartType: $dartType\n" +
                "name: ${configuration.name}\n" +
                "shape: ${shape.toString()}\n" +
                "object3DFileName: $object3DFileName \n" +
                "objectUrl: $objectUrl \n" +
                "position: ${configuration.currentPosition}\n" +
                "scale: ${configuration.currentScale}\n" +
                "rotation: ${configuration.currentRotation}\n" +
                "parentNodeName: $parentNodeName"
    }

}
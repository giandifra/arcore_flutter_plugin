package com.difrancescogianmarco.arcore_flutter_plugin.flutter_models

import com.difrancescogianmarco.arcore_flutter_plugin.models.RotatingNode
import com.difrancescogianmarco.arcore_flutter_plugin.utils.DecodableUtils.Companion.parseQuaternion
import com.difrancescogianmarco.arcore_flutter_plugin.utils.DecodableUtils.Companion.parseVector3
import com.google.ar.core.Pose
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

class FlutterArCoreNode(map: HashMap<String, *>) {

    val dartType: String = map["dartType"] as String
    val name: String = map["name"] as String
    val image: FlutterArCoreImage? = createArCoreImage(map["image"] as? HashMap<String, *>)
    val objectUrl: String? = map["objectUrl"] as? String
    val object3DFileName: String? = map["object3DFileName"] as? String
    val shape: FlutterArCoreShape? = getShape(map["shape"] as? HashMap<String, *>)
    val position: Vector3 = parseVector3(map["position"] as? HashMap<String, *>) ?: Vector3()
    val scale: Vector3 = parseVector3(map["scale"] as? HashMap<String, *>)
            ?: Vector3(1.0F, 1.0F, 1.0F)
    val rotation: Quaternion = parseQuaternion(map["rotation"] as? HashMap<String, Double>)
            ?: Quaternion()
    val degreesPerSecond: Float? = getDegreesPerSecond((map["degreesPerSecond"] as? Double))
    var parentNodeName: String? = map["parentNodeName"] as? String

    val children: ArrayList<FlutterArCoreNode> = getChildrenFromMap(map["children"] as ArrayList<HashMap<String, *>>)

    private fun getChildrenFromMap(list: ArrayList<HashMap<String, *>>): ArrayList<FlutterArCoreNode> {
        return ArrayList(list.map { map -> FlutterArCoreNode(map) })
    }

    fun buildNode(): Node {
        lateinit var node: Node
        if (degreesPerSecond != null) {
            node = RotatingNode(degreesPerSecond, true, 0.0f)
        } else {
            node = Node()
        }

        node.name = name
        node.localPosition = position
        node.localScale = scale
        node.localRotation = rotation

        return node
    }

    fun getPosition(): FloatArray {
        return floatArrayOf(position.x, position.y, position.z)
    }

    fun getRotation(): FloatArray {
        return floatArrayOf(rotation.x, rotation.y, rotation.z, rotation.w)
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
                "name: $name\n" +
                "shape: ${shape.toString()}\n" +
                "object3DFileName: $object3DFileName \n" +
                "objectUrl: $objectUrl \n" +
                "position: $position\n" +
                "scale: $scale\n" +
                "rotation: $rotation\n" +
                "parentNodeName: $parentNodeName"
    }

}
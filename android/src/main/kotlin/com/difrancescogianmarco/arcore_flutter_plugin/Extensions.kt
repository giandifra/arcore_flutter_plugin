
package com.difrancescogianmarco.arcore_flutter_plugin

import androidx.annotation.ColorInt
import com.google.ar.core.Pose
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import kotlin.math.roundToInt

fun @receiver:ColorInt Int.toArColor(): Color = Color(this)

fun Pose.translation() = Vector3(tx(), ty(), tz())

fun Pose.rotation() = Quaternion(qx(), qy(), qz(), qw())

inline fun <reified T> ArSceneView.findNode(): T? = scene.findInHierarchy { it is T } as T?

fun formatDistance(pose: Pose?, vector3: Vector3): Int? {
    if (pose == null) return null
    val x = pose.tx() - vector3.x
    val y = pose.ty() - vector3.y
    val z = pose.tz() - vector3.z
    val distanceInMeters = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
    val distanceInCentimeters = (distanceInMeters * 100).roundToInt()
    return distanceInCentimeters
}

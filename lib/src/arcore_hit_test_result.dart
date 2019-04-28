import 'package:vector_math/vector_math_64.dart';
import 'arcore_pose.dart';

class ArCoreHitTestResult {
  double distance;

  Vector3 translation;

  Vector4 rotation;

  String nodeName;

  ArCorePose pose;

  ArCoreHitTestResult.fromMap(Map<dynamic, dynamic> map) {
    this.distance = map['distance'];
    this.pose = ArCorePose.fromMap(map['pose']);
  }
}

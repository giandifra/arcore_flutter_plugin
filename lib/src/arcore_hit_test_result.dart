import 'package:vector_math/vector_math_64.dart';

import 'arcore_pose.dart';

class ArCoreHitTestResult {
  late double distance;

  late Vector3 translation;

  late Vector4 rotation;

  late String nodeName;

  late ArCorePose pose;

  ArCoreHitTestResult.fromMap(Map<dynamic, dynamic> map) {
    this.distance = map['distance'];
    this.pose = ArCorePose.fromMap(map['pose']);
  }
}

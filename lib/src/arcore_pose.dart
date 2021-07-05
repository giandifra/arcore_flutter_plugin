import 'package:vector_math/vector_math_64.dart';

class ArCorePose {
  late Vector3 translation;
  late Vector4 rotation;

  ArCorePose.fromMap(Map<dynamic, dynamic> map) {
    this.translation = Vector3.array(map["translation"]);
    this.rotation = Vector4.array(map["rotation"]);
  }
}

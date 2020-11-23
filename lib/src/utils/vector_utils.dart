import 'package:vector_math/vector_math_64.dart';

Map<String, double> convertVector3ToMap(Vector3 vector) =>
    vector == null ? null : {'x': vector.x, 'y': vector.y, 'z': vector.z};

Map<String, double> convertVector4ToMap(Vector4 vector) => vector == null
    ? null
    : {'x': vector.x, 'y': vector.y, 'z': vector.z, 'w': vector.w};

extension Vector3Ext on Vector3 {
  String get text =>
      'x: ${this.x.toStringAsFixed(2)} y: ${this.y.toStringAsFixed(2)} z: ${this.z.toStringAsFixed(2)}';
}

extension Vector4Ext on Vector4 {
  String get text =>
      'x: ${this.x.toStringAsFixed(2)} y: ${this.y.toStringAsFixed(2)} z: ${this.z.toStringAsFixed(2)} t: ${this.t.toStringAsFixed(2)}';
}

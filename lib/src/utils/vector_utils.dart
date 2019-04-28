import 'package:vector_math/vector_math_64.dart';

Map<String, double> convertVector3ToMap(Vector3 vector) =>
    vector == null ? null : {'x': vector.x, 'y': vector.y, 'z': vector.z};

Map<String, double> convertVector4ToMap(Vector4 vector) => vector == null
    ? null
    : {'x': vector.x, 'y': vector.y, 'z': vector.z, 'w': vector.w};

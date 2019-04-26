
import 'package:arcore_flutter_plugin/src/arcore_material.dart';
import 'package:flutter/widgets.dart';

/// ARKitGeometry is an abstract class that represents the geometry that can be attached to a SCNNode.
abstract class ArCoreShape {
  ArCoreShape({@required List<ArCoreMaterial> materials})
      : materials = ValueNotifier(materials);

  /// Specifies the receiver's materials array.
  /// Each geometry element can be rendered using a different material.
  /// The index of the material used for a geometry element is equal to the index of that element modulo the number of materials.
  final ValueNotifier<List<ArCoreMaterial>> materials;

  Map<String, dynamic> toMap() => <String, dynamic>{
    'dartType': runtimeType.toString(),
    'materials': materials != null
        ? materials?.value?.map((m) => m.toMap())?.toList()
        : null,
  }..removeWhere((String k, dynamic v) => v == null);
}

import 'package:arcore_flutter_plugin/src/arcore_material.dart';
import 'package:arcore_flutter_plugin/src/shape/arcore_shape.dart';

class ArCoreSphere extends ArCoreShape {
  ArCoreSphere({
    this.radius = 0.5,
    List<ArCoreMaterial> materials,
  }) : super(
          materials: materials,
        );

  final double radius;

  @override
  Map<String, dynamic> toMap() => <String, dynamic>{
        'radius': radius,
      }..addAll(super.toMap());
}

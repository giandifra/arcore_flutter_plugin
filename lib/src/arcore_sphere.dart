import 'arcore_geometry.dart';
import 'arcore_material.dart';

/// Represents a sphere with controllable radius
class ArCoreSphere extends ArCoreGeometry {
  ArCoreSphere({
    this.radius = 0.5,
    List<ArCoreMaterial> materials,
  }) : super(
          materials: materials,
        );

  /// The sphere radius.
  /// If the value is less than or equal to 0, the geometry is empty.
  /// The default value is 0.5.
  final double radius;

  @override
  Map<String, dynamic> toMap() => <String, dynamic>{
        'radius': radius,
      }..addAll(super.toMap());
}

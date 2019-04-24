
import 'package:arcore_flutter_plugin/src/vector_utils.dart';
import 'package:flutter/widgets.dart';
import 'package:vector_math/vector_math_64.dart';

import 'matrix4_utils.dart';

/// Object representing a physical location and orientation in 3D space.
class ArCoreAnchor {
  ArCoreAnchor(
      this.nodeName,
      this.identifier,
      this.transform,
      );

  /// Represents the name of the node anchor attached to.
  final String nodeName;

  /// Unique identifier of the anchor.
  final String identifier;

  /// The transformation matrix that defines the anchor’s rotation, translation and scale in world coordinates.
  final Matrix4 transform;

  static ArCoreAnchor fromMap(Map<String, String> map) => ArCoreAnchor(
    map['node_name'],
    map['identifier'],
    getMatrixFromString(map['transform']),
  );
}

/// An anchor representing a planar surface in the world.
/// Planes are defined in the X and Z direction, where Y is the surface’s normal.
class ArCorePlaneAnchor extends ArCoreAnchor {
  ArCorePlaneAnchor(
      this.center,
      this.extent,
      String nodeName,
      String identifier,
      Matrix4 transorm,
      ) : super(
    nodeName,
    identifier,
    transorm,
  );

  /// The center of the plane in the anchor’s coordinate space.
  final Vector3 center;

  /// The extent of the plane in the anchor’s coordinate space.
  final Vector3 extent;

  static ArCorePlaneAnchor fromMap(Map<String, String> map) => ArCorePlaneAnchor(
    createVector3FromString(map['center']),
    createVector3FromString(map['extent']),
    map['node_name'],
    map['identifier'],
    getMatrixFromString(map['transform']),
  );
}

/// An anchor representing an image in the world.
class ArCoreImageAnchor extends ArCoreAnchor {
  ArCoreImageAnchor(
      this.referenceImageName,
      String nodeName,
      String identifier,
      Matrix4 transorm,
      ) : super(
    nodeName,
    identifier,
    transorm,
  );

  /// Name of the detected image.
  final String referenceImageName;

  static ArCoreImageAnchor fromMap(Map<String, String> map) => ArCoreImageAnchor(
    map['referenceImageName'],
    map['node_name'],
    map['identifier'],
    getMatrixFromString(map['transform']),
  );
}

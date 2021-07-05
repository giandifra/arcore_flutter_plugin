import 'package:vector_math/vector_math_64.dart';

import 'arcore_node.dart';

class ArCoreReferenceNode extends ArCoreNode {
  /// Filename of sfb object in assets folder (generated with Import Sceneform Asset)
  /// https://developers.google.com/ar/develop/java/sceneform/import-assets
  final String? object3DFileName;

  /// Url of gltf object for remote rendering
  final String? objectUrl;

  ArCoreReferenceNode({
    String? name,
    this.object3DFileName,
    this.objectUrl,
    List<ArCoreNode> children = const [],
    Vector3? position,
    Vector3? scale,
    Vector4? rotation,
  }) : super(
            name: name,
            children: children,
            position: position,
            scale: scale,
            rotation: rotation);

  @override
  Map<String, dynamic> toMap() => <String, dynamic>{
        'object3DFileName': this.object3DFileName,
        'objectUrl': this.objectUrl,
      }..addAll(super.toMap());
}

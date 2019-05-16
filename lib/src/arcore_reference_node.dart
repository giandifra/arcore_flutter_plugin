import 'package:vector_math/vector_math_64.dart';

import 'arcore_node.dart';

class ArCoreReferenceNode extends ArCoreNode {
  /// Filenaeme of sfb object in assets folder (generated with Import Sceneform Asset)
  /// https://developers.google.com/ar/develop/java/sceneform/import-assets
  final String obcject3DFileName;

  /// Url of gltf object for remote rendering
  final String objectUrl;

  ArCoreReferenceNode({
    String name,
    this.obcject3DFileName,
    this.objectUrl,
    List<ArCoreNode> children = const [],
    Vector3 position,
    Vector3 scale,
    Vector4 rotation,
  }) : super(
            name: name,
            children: children,
            position: position,
            scale: scale,
            rotation: rotation);

  @override
  Map<String, dynamic> toMap() => <String, dynamic>{
        'obcject3DFileName': this.obcject3DFileName,
        'objectUrl': this.objectUrl,
      }..addAll(super.toMap());
}

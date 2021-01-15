import 'package:vector_math/vector_math_64.dart';

import 'arcore_node.dart';

class ArCoreReferenceNode extends ArCoreNode {
  /// Filename of sfb object in assets folder (generated with Import Sceneform Asset)
  /// https://developers.google.com/ar/develop/java/sceneform/import-assets
  final String object3DFileName;

  /// Url of gltf object for remote rendering
  final String objectUrl;

  /// Index of the animation to execute
  final int animationIndex;

  /// Name of the animation to execute (not handled if animationIndex is not null)
  final String animationName;

  /// Number of repetition for the animation (-1 for infinite repetition)
  final int animationRepeatNb;

  ArCoreReferenceNode({
    String name,
    this.object3DFileName,
    this.objectUrl,
    List<ArCoreNode> children = const [],
    Vector3 position,
    Vector3 scale,
    Vector4 rotation,
    this.animationIndex,
    this.animationName,
    this.animationRepeatNb,
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
        'animationIndex': this.animationIndex,
        'animationName': this.animationName,
        'animationRepeatNb': this.animationRepeatNb,
      }..addAll(super.toMap());
}

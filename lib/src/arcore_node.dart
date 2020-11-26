import 'package:arcore_flutter_plugin/src/arcore_image.dart';
import 'package:arcore_flutter_plugin/src/utils/vector_utils.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/widgets.dart';
import 'package:vector_math/vector_math_64.dart';
import 'package:arcore_flutter_plugin/src/utils/random_string.dart'
    as random_string;
import 'package:arcore_flutter_plugin/src/shape/arcore_shape.dart';

class ArCoreNode {
  ArCoreNode({
    ScaleControllerNode scaleControllerNode,
    TranslationControllerNode translationControllerNode,
    RotationControllerNode rotationControllerNode,
    String name,
    Vector3 position,
    Vector3 scale,
    Vector4 rotation,
    this.shape,
    this.image,
    this.children = const [],
  })  : name = name ?? 'node_${random_string.randomString(length: 6)}',
        scaleControllerNode = ValueNotifier(
            scaleControllerNode ?? ScaleControllerNode(scale: scale)),
        translationControllerNode = ValueNotifier(translationControllerNode ??
            TranslationControllerNode(position: position)),
        rotationControllerNode = ValueNotifier(rotationControllerNode ??
            RotationControllerNode(rotation: rotation)),
        assert(!(shape != null && image != null)),
        assert(!(scaleControllerNode != null && scale != null)),
        assert(!(translationControllerNode != null && position != null)),
        assert(!(rotationControllerNode != null && rotation != null));

  final List<ArCoreNode> children;

  final ArCoreShape shape;

  final String name;

  final ArCoreImage image;

  final ValueNotifier<ScaleControllerNode> scaleControllerNode;

  final ValueNotifier<TranslationControllerNode> translationControllerNode;

  final ValueNotifier<RotationControllerNode> rotationControllerNode;

  Vector3 get position => translationControllerNode.value.position;
  bool get positionGestureEnabled => translationControllerNode.value.enabled;
  set positionGestureEnabled(bool value) {
    translationControllerNode.value =
        translationControllerNode.value.copyWith(enabled: value);
  }

  Vector4 get rotation => rotationControllerNode.value.rotation;
  bool get rotationGestureEnabled => rotationControllerNode.value.enabled;
  set rotationGestureEnabled(bool value) {
    rotationControllerNode.value =
        rotationControllerNode.value.copyWith(enabled: value);
  }

  Vector3 get scale => scaleControllerNode.value.scale;
  bool get scaleGestureEnabled => scaleControllerNode.value.enabled;
  set scaleGestureEnabled(bool value) {
    scaleControllerNode.value =
        scaleControllerNode.value.copyWith(enabled: value);
  }

  void changeRotation(Vector4 newRotation) {
    rotationControllerNode.value =
        rotationControllerNode.value.copyWith(rotation: newRotation);
  }

  void changePosition(Vector3 newPosition) {
    translationControllerNode.value =
        translationControllerNode.value.copyWith(position: newPosition);
  }

  void changeScale(Vector3 newScale) {
    scaleControllerNode.value =
        scaleControllerNode.value.copyWith(scale: newScale);
  }

  // factory ArCoreNode.fromMap(Map<String, dynamic> map) {
  //   return ArCoreNode(
  //     name: map['name'],
  //     position: map['position'],
  //     scale: map['scale'],
  //     rotation: map['rotation'],
  //   );
  // }

  Map<String, dynamic> toMap() => <String, dynamic>{
        'dartType': runtimeType.toString(),
        'shape': shape?.toMap(),
        'scaleControllerNode': scaleControllerNode?.value?.toMap(),
        'translationControllerNode': translationControllerNode?.value?.toMap(),
        'rotationControllerNode': rotationControllerNode?.value?.toMap(),
        'name': name,
        'image': image?.toMap(),
        'children':
            this.children.map((arCoreNode) => arCoreNode.toMap()).toList(),
      }..removeWhere((String k, dynamic v) => v == null);
}

class ScaleControllerNode extends Equatable {
  final Vector3 scale;
  final double minScale;
  final bool enabled;
  final double maxScale;

  ScaleControllerNode({
    @required this.scale,
    this.minScale = 0.25,
    this.maxScale = 5.0,
    this.enabled = true,
  });

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'scale': convertVector3ToMap(this.scale),
      'minScale': this.minScale,
      'enabled': this.enabled,
      'maxScale': this.maxScale,
    };
  }

  ScaleControllerNode copyWith({
    Vector3 scale,
    double minScale,
    bool enabled,
    double maxScale,
  }) {
    if ((scale == null || identical(scale, this.scale)) &&
        (minScale == null || identical(minScale, this.minScale)) &&
        (enabled == null || identical(enabled, this.enabled)) &&
        (maxScale == null || identical(maxScale, this.maxScale))) {
      return this;
    }

    return new ScaleControllerNode(
      scale: scale ?? this.scale,
      minScale: minScale ?? this.minScale,
      enabled: enabled ?? this.enabled,
      maxScale: maxScale ?? this.maxScale,
    );
  }

  @override
  List<Object> get props => [scale, minScale, maxScale, enabled];
}

class TranslationControllerNode extends Equatable {
  final Vector3 position;
  final bool enabled;

  TranslationControllerNode({
    @required this.position,
    this.enabled = true,
  });

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'position': convertVector3ToMap(this.position),
      'enabled': this.enabled,
    };
  }

  TranslationControllerNode copyWith({
    Vector3 position,
    bool enabled,
  }) {
    if ((position == null || identical(position, this.position)) &&
        (enabled == null || identical(enabled, this.enabled))) {
      return this;
    }

    return new TranslationControllerNode(
      position: position ?? this.position,
      enabled: enabled ?? this.enabled,
    );
  }

  @override
  List<Object> get props => [position, enabled];
}

class RotationControllerNode extends Equatable {
  final Vector4 rotation;
  final bool enabled;

  RotationControllerNode({
    @required this.rotation,
    this.enabled = true,
  });

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'rotation': convertVector4ToMap(this.rotation),
      'enabled': this.enabled,
    };
  }

  RotationControllerNode copyWith({
    Vector4 rotation,
    bool enabled,
  }) {
    if ((rotation == null || identical(rotation, this.rotation)) &&
        (enabled == null || identical(enabled, this.enabled))) {
      return this;
    }

    return new RotationControllerNode(
      rotation: rotation ?? this.rotation,
      enabled: enabled ?? this.enabled,
    );
  }

  @override
  List<Object> get props => [rotation, enabled];
}

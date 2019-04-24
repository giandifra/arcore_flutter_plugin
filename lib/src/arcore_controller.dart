import 'package:arcore_flutter_plugin/src/arcore_anchor.dart';
import 'package:arcore_flutter_plugin/src/arcore_plane.dart';
import 'package:arcore_flutter_plugin/src/vector_utils.dart';
import 'package:flutter/services.dart';

import 'arcore_node.dart';

typedef StringResultHandler = void Function(String text);
//typedef AnchorEventHandler = void Function(ARKitAnchor anchor);

class ArCoreController {
  ArCoreController(
    int id,
//      bool showStatistics,
//      bool autoenablesDefaultLighting,
      bool enableTapRecognizer,
//      bool showFeaturePoints,
//      bool showWorldOrigin,
//      ARPlaneDetection planeDetection,
//      String detectionImagesGroupName,
  ) {
    _channel = MethodChannel('arcore_flutter_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
    _channel.invokeMethod<void>('init', {
//      'showStatistics': showStatistics,
//      'autoenablesDefaultLighting': autoenablesDefaultLighting,
      'enableTapRecognizer': enableTapRecognizer,
//      'planeDetection': planeDetection.index,
//      'showFeaturePoints': showFeaturePoints,
//      'showWorldOrigin': showWorldOrigin,
//      'detectionImagesGroupName': detectionImagesGroupName,
    });
  }

  MethodChannel _channel;
  StringResultHandler onError;
  StringResultHandler onTap;

//  Matrix4ResultHandler onPlaneTap;

  Future<dynamic> _handleMethodCalls(MethodCall call) async {
    print('_platformCallHandler call ${call.method} ${call.arguments}');
    switch (call.method) {
      case 'onError':
        if (onError != null) {
          onError(call.arguments);
        }
        break;
      case 'onTap':
        if (onTap != null) {
          onTap(call.arguments);
        }
        break;
//      case 'onPlaneTap':
//        if (onPlaneTap != null) {
//          onPlaneTap(getMatrixFromString(call.arguments));
//        }
//        break;
//      case 'didAddNodeForAnchor':
//        if (onAddNodeForAnchor != null) {
//          final anchor = _buildAnchor(call.arguments);
//          onAddNodeForAnchor(anchor);
//        }
//        break;
//      case 'didUpdateNodeForAnchor':
//        if (onUpdateNodeForAnchor != null) {
//          final anchor = _buildAnchor(call.arguments);
//          onUpdateNodeForAnchor(anchor);
//        }
//        break;
      default:
        print('Unknowm method ${call.method} ');
    }
    return Future.value();
  }

  Future<void> add(ArCoreNode node, {String parentNodeName}) {
    assert(node != null);
    final params = _addParentNodeNameToParams(node.toMap(), parentNodeName);
//    _subsribeToChanges(node);
    return _channel.invokeMethod('addArCoreNode', params);
  }

  Map<String, dynamic> _addParentNodeNameToParams(
      Map geometryMap, String parentNodeName) {
    if (parentNodeName?.isNotEmpty ?? false)
      geometryMap['parentNodeName'] = parentNodeName;
    return geometryMap;
  }

  void _subsribeToChanges(ArCoreNode node) {
    node.position.addListener(() => _handlePositionChanged(node));
    node.rotation.addListener(() => _handleRotationChanged(node));

    node.geometry.materials.addListener(() => _updateMaterials(node));
    if (node.geometry is ArCorePlane) {
      final ArCorePlane plane = node.geometry;
      plane.width.addListener(() =>
          _updateSingleGeometryProperty(node, 'width', plane.width.value));
      plane.height.addListener(() =>
          _updateSingleGeometryProperty(node, 'height', plane.height.value));
    }
  }

  void _handlePositionChanged(ArCoreNode node) {
    _channel.invokeMethod<void>('positionChanged',
        _getHandlerParams(node, convertVector3ToMap(node.position.value)));
  }

  void _handleRotationChanged(ArCoreNode node) {
    _channel.invokeMethod<void>('rotationChanged',
        _getHandlerParams(node, convertVector4ToMap(node.rotation.value)));
  }

  void _updateMaterials(ArCoreNode node) {
    _channel.invokeMethod<void>(
        'updateMaterials', _getHandlerParams(node, node.geometry.toMap()));
  }

  void _updateSingleGeometryProperty(
      ArCoreNode node, String propertyName, dynamic value) {
    _channel.invokeMethod<void>(
        'updateSingleGeometryProperty',
        _getHandlerParams(node, <String, dynamic>{
          'propertyName': propertyName,
          'propertyValue': value,
        }));
  }

  Map<String, dynamic> _getHandlerParams(
      ArCoreNode node, Map<String, dynamic> params) {
    final Map<String, dynamic> values = <String, dynamic>{'name': node.name}
      ..addAll(params);
    return values;
  }

  ArCoreAnchor _buildAnchor(Map arguments) {
    final type = arguments['anchorType'].toString();
    final map = arguments.cast<String, String>();
    switch (type) {
      case 'planeAnchor':
        return ArCorePlaneAnchor.fromMap(map);
      case 'imageAnchor':
        return ArCoreImageAnchor.fromMap(map);
    }
    return ArCoreAnchor.fromMap(map);
  }

  void dispose() {
    _channel?.invokeMethod<void>('dispose');
  }
}

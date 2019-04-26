import 'package:arcore_flutter_plugin/src/arcore_rotating_node.dart';
import 'package:arcore_flutter_plugin/src/utils/vector_utils.dart';
import 'package:flutter/services.dart';

import 'arcore_node.dart';

typedef StringResultHandler = void Function(String text);

class ArCoreController {
  ArCoreController(
    int id,
    bool enableTapRecognizer,
  ) {
    _channel = MethodChannel('arcore_flutter_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
    _channel.invokeMethod<void>('init', {
      'enableTapRecognizer': enableTapRecognizer,
    });
  }

  MethodChannel _channel;
  StringResultHandler onError;
  StringResultHandler onTap;

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
      default:
        print('Unknowm method ${call.method} ');
    }
    return Future.value();
  }

  Future<void> add(ArCoreNode node, {String parentNodeName}) {
    assert(node != null);
    final params = _addParentNodeNameToParams(node.toMap(), parentNodeName);
    _subsribeToChanges(node);
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

    if (node is ArCoreRotatingNode) {
      node.degreesPerSecond.addListener(() => _handleRotationChanged(node));
    }

    node.shape.materials.addListener(() => _updateMaterials(node));

  }

  void _handlePositionChanged(ArCoreNode node) {
    _channel.invokeMethod<void>('positionChanged',
        _getHandlerParams(node, convertVector3ToMap(node.position.value)));
  }

  void _handleRotationChanged(ArCoreRotatingNode node) {
    _channel.invokeMethod<void>('rotationChanged',
        {'name': node.name, 'degreesPerSecond': node.degreesPerSecond.value});
  }

  void _updateMaterials(ArCoreNode node) {
    _channel.invokeMethod<void>(
        'updateMaterials', _getHandlerParams(node, node.shape.toMap()));
  }

  void _updateSingleGeometryProperty(
      ArCoreNode node, String propertyName, dynamic value) {
    _channel.invokeMethod<void>(
      'updateSingleGeometryProperty',
      _getHandlerParams(
        node,
        <String, dynamic>{
          'propertyName': propertyName,
          'propertyValue': value,
        },
      ),
    );
  }

  Map<String, dynamic> _getHandlerParams(
      ArCoreNode node, Map<String, dynamic> params) {
    final Map<String, dynamic> values = <String, dynamic>{'name': node.name}
      ..addAll(params);
    return values;
  }

  void dispose() {
    _channel?.invokeMethod<void>('dispose');
  }
}

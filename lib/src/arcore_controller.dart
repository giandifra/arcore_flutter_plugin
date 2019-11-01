import 'package:arcore_flutter_plugin/src/arcore_rotating_node.dart';
import 'package:arcore_flutter_plugin/src/utils/vector_utils.dart';
import 'package:flutter/services.dart';
import 'package:meta/meta.dart';
import 'arcore_hit_test_result.dart';

import 'arcore_node.dart';
import 'arcore_plane.dart';

typedef StringResultHandler = void Function(String text);
typedef UnsupportedHandler = void Function(String text);
typedef ArCoreHitResultHandler = void Function(List<ArCoreHitTestResult> hits);
typedef ArCorePlaneHandler = void Function(ArCorePlane plane);

class ArCoreController {
  ArCoreController({
    int id,
    this.enableTapRecognizer,
    this.enableUpdateListener,
//    @required this.onUnsupported,
  }) {
    _channel = MethodChannel('arcore_flutter_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
    init();
  }

  final bool enableUpdateListener;
  final bool enableTapRecognizer;
  MethodChannel _channel;
  StringResultHandler onError;
  StringResultHandler onNodeTap;

//  UnsupportedHandler onUnsupported;
  ArCoreHitResultHandler onPlaneTap;
  ArCorePlaneHandler onPlaneDetected;

  init() async {
    try {
      await _channel.invokeMethod<void>('init', {
        'enableTapRecognizer': enableTapRecognizer,
        'enableUpdateListener': enableUpdateListener,
      });
    } on PlatformException catch (ex) {
      print(ex.message);
    }
  }

  Future<dynamic> _handleMethodCalls(MethodCall call) async {
    print('_platformCallHandler call ${call.method} ${call.arguments}');
    switch (call.method) {
      case 'onError':
        if (onError != null) {
          onError(call.arguments);
        }
        break;
      case 'onNodeTap':
        if (onNodeTap != null) {
          onNodeTap(call.arguments);
        }
        break;
      case 'onPlaneTap':
        if (onPlaneTap != null) {
          final List<dynamic> input = call.arguments;
          final objects = input
              .cast<Map<dynamic, dynamic>>()
              .map<ArCoreHitTestResult>(
                  (Map<dynamic, dynamic> h) => ArCoreHitTestResult.fromMap(h))
              .toList();
          onPlaneTap(objects);
        }
        break;
      case 'onPlaneDetected':
        if (enableUpdateListener && onPlaneDetected != null) {
          final plane = ArCorePlane.fromMap(call.arguments);
          onPlaneDetected(plane);
        }
        break;
      default:
        print('Unknowm method ${call.method} ');
    }
    return Future.value();
  }

  Future<void> addArCoreNode(ArCoreNode node, {String parentNodeName}) {
    assert(node != null);
    final params = _addParentNodeNameToParams(node.toMap(), parentNodeName);
    print(params.toString());
    _addListeners(node);
    return _channel.invokeMethod('addArCoreNode', params);
  }

  Future<void> addArCoreNodeWithAnchor(ArCoreNode node,
      {String parentNodeName}) {
    assert(node != null);
    final params = _addParentNodeNameToParams(node.toMap(), parentNodeName);
    print(params.toString());
    _addListeners(node);
    return _channel.invokeMethod('addArCoreNodeWithAnchor', params);
  }

  Future<void> removeNode({@required String nodeName}) {
    assert(nodeName != null);
    return _channel.invokeMethod('removeARCoreNode', {'nodeName': nodeName});
  }

  Map<String, dynamic> _addParentNodeNameToParams(
      Map geometryMap, String parentNodeName) {
    if (parentNodeName?.isNotEmpty ?? false)
      geometryMap['parentNodeName'] = parentNodeName;
    return geometryMap;
  }

  void _addListeners(ArCoreNode node) {
    node.position.addListener(() => _handlePositionChanged(node));
    node?.shape?.materials?.addListener(() => _updateMaterials(node));

    if (node is ArCoreRotatingNode) {
      node.degreesPerSecond.addListener(() => _handleRotationChanged(node));
    }
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

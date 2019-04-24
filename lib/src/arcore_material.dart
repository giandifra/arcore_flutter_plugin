import 'dart:ui';

class ArCoreMaterial {
  final MaterialFactory materialFactory;
  final Color color;

  ArCoreMaterial(
      {this.color, this.materialFactory = MaterialFactory.OPAQUE_WITH_COLOR});

  Map<String, dynamic> toMap() => <String, dynamic>{
        'color': [color.red,color.green,color.blue],
        'materialType': materialFactory.index
      }..removeWhere((String k, dynamic v) => v == null);
}

enum MaterialFactory {
  OPAQUE_WITH_COLOR,
  TRANSPARENT_WITH_COLOR,
  OPAQUE_WITH_TEXTURE,
  TRANSPARENT_WITH_TEXTURE
}

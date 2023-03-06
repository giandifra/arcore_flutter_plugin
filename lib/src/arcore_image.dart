import 'dart:typed_data';

class ArCoreImage {
  ArCoreImage({
    required this.bytes,
    this.width,
    this.height,
  })  : assert(bytes != null),
        assert(width != null && width > 0),
        assert(height != null && height > 0);

  final Uint8List? bytes;
  final int? width;
  final int? height;

  Map<String, dynamic> toMap() => <String, dynamic>{
        'bytes': bytes,
        'width': width,
        'height': height
      }..removeWhere((String k, dynamic v) => v == null);
}

class ArCoreVideo {
  ArCoreVideo({
    this.bytes,
    this.loop = true,
  }) : assert(bytes != null);

  final bool loop;
  final Uint8List? bytes;

  Map<String, dynamic> toMap() => <String, dynamic>{
        'bytes': bytes,
        'loop': loop,
      }..removeWhere((String k, dynamic v) => v == null);
}

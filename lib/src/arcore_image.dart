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
  }) : assert(bytes != null);

  final Uint8List? bytes;

  Map<String, dynamic> toMap() => <String, dynamic>{
        'bytes': bytes,
      }..removeWhere((String k, dynamic v) => v == null);
}

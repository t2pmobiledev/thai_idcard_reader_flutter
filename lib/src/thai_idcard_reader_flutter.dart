import 'dart:convert';

import 'package:flutter/services.dart';
import 'types/common.dart';
import 'dart:core';

class ThaiIdcardReaderFlutter with ThaiIDType {
  static const MethodChannel _channel =
      MethodChannel('thai_idcard_reader_flutter_channel');

  static const EventChannel _usbStreamChannel =
      EventChannel('usb_stream_channel');
  static const EventChannel _readerStreamChannel =
      EventChannel('reader_stream_channel');

  static Stream<UsbDevice> get deviceHandlerStream {
    return _usbStreamChannel
        .receiveBroadcastStream()
        .distinct()
        .map((dynamic event) => UsbDevice.fromMap(event));
  }

  static Stream<IDCardReader> get cardHandlerStream {
    return _readerStreamChannel
        .receiveBroadcastStream()
        .distinct()
        .map((dynamic event) => IDCardReader.fromMap(event));
  }

  static int _permissionWatchers = 0;

  /// Android: shows the USB permission dialog right away if a reader is attached
  /// and keeps asking whenever a reader is plugged in, until [stopPermissionWatch].
  /// Call it when a card-reading screen opens so the user grants access before
  /// inserting the card. Calls are reference-counted, so overlapping screens are safe.
  ///
  /// Returns "000" already granted, "009" dialog shown, "004" no reader attached,
  /// or null when unsupported on this platform.
  static Future<String?> startPermissionWatch() async {
    _permissionWatchers++;
    try {
      return await _channel.invokeMethod<String>('startPermissionWatch');
    } on MissingPluginException {
      return null;
    } on PlatformException {
      return null;
    }
  }

  /// Stops the auto-request started by [startPermissionWatch].
  static Future<void> stopPermissionWatch() async {
    if (_permissionWatchers > 0) _permissionWatchers--;
    if (_permissionWatchers > 0) return;
    try {
      await _channel.invokeMethod('stopPermissionWatch');
    } on MissingPluginException {
      // unsupported platform
    } on PlatformException {
      // ignore
    }
  }

  static Future<String> readRaw() async {
    return await _channel.invokeMethod('getInfo').timeout(const Duration(seconds: 20));
  }

  static Future<ThaiIDCard> read({List<String> only = const []}) async {
    // final String res = only.isNotEmpty
    //     ? await _channel.invokeMethod('read', {'selected': only})
    //     : await _channel.invokeMethod('readAll');
    final String res = await _channel.invokeMethod('getInfo').timeout(const Duration(seconds: 20));
    return ThaiIDCard.fromJson(res);
  }
}

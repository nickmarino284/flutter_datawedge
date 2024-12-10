import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter_datawedge/flutter_datawedge.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const channel = MethodChannel('flutter_datawedge');
  const dataWedgeHostChannel = MethodChannel('dev.flutter.pigeon.flutter_datawedge.DataWedgeHostApi');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'initialize':
          return 'Initialized';
        case 'onScanResult':
          return {
            'dataString': 'Sample Scan Result',
            'decodeMode': 'single',
            'labelType': 'code128',
            'source': 'scanner',
          };
        default:
          return null;
      }
    });

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(dataWedgeHostChannel, (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'registerForNotifications':
          return null;
        case 'unregisterForNotifications':
          return null;
        case 'suspendPlugin':
          return 'Plugin suspended';
        case 'resumePlugin':
          return 'Plugin resumed';
        case 'enablePlugin':
          return 'Plugin enabled';
        case 'disablePlugin':
          return 'Plugin disabled';
        case 'softScanTrigger':
          return 'Soft scan triggered';
        case 'getPackageIdentifer':
          return 'com.example.datawedge';
        default:
          return null;
      }
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(dataWedgeHostChannel, null);
  });

  test('onScanResult stream test', () async {
    final dataWedge = FlutterDataWedge.instance;

    // Simulate a scan result
    Future.delayed(const Duration(milliseconds: 100), () {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage(
        channel.name,
        const StandardMethodCodec().encodeMethodCall(
          const MethodCall('onScanResult', {
            'dataString': 'Sample Scan Result',
            'decodeMode': 'single',
            'labelType': 'code128',
            'source': 'scanner',
          }),
        ),
            (_) {},
      );
    });

    await expectLater(
      dataWedge.scans,
      emits(isA<ScanEvent>().having((event) => event.dataString, 'dataString', 'Sample Scan Result')),
    );
  });

  test('initialize method test', () async {
    final result = await channel.invokeMethod('initialize');
    expect(result, 'Initialized');
  });

  test('registerForNotifications method test', () async {
    final dataWedge = FlutterDataWedge.instance;
    await dataWedge.registerForNotifications();
    expect(true, isTrue); // Verifying the method completes successfully
  });

  test('unregisterForNotifications method test', () async {
    final dataWedge = FlutterDataWedge.instance;
    await dataWedge.unregisterForNotifications();
    expect(true, isTrue); // Verifying the method completes successfully
  });

  test('suspendPlugin method test', () async {
    final result = await dataWedgeHostChannel.invokeMethod('suspendPlugin');
    expect(result, 'Plugin suspended');
  });

  test('resumePlugin method test', () async {
    final result = await dataWedgeHostChannel.invokeMethod('resumePlugin');
    expect(result, 'Plugin resumed');
  });

  test('enablePlugin method test', () async {
    final result = await dataWedgeHostChannel.invokeMethod('enablePlugin');
    expect(result, 'Plugin enabled');
  });

  test('disablePlugin method test', () async {
    final result = await dataWedgeHostChannel.invokeMethod('disablePlugin');
    expect(result, 'Plugin disabled');
  });

  test('softScanTrigger method test', () async {
    final result = await dataWedgeHostChannel.invokeMethod('softScanTrigger', {'on': true});
    expect(result, 'Soft scan triggered');
  });

  test('getPackageIdentifer method test', () async {
    final result = await dataWedgeHostChannel.invokeMethod('getPackageIdentifer');
    expect(result, 'com.example.datawedge');
  });
}

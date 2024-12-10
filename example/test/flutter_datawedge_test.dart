import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_datawedge/flutter_datawedge.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  // Initialize the test environment
  TestWidgetsFlutterBinding.ensureInitialized();

  final dataWedge = FlutterDataWedge.instance;

  const channel = MethodChannel('flutter_datawedge');
  const dataWedgeHostChannel = MethodChannel('dev.flutter.pigeon.flutter_datawedge.DataWedgeHostApi');

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
      if (methodCall.method == 'registerForNotifications') {
        return null;
      }
      return null;
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(dataWedgeHostChannel, null);
  });

  test('initialize method test', () async {
    final result = await channel.invokeMethod('initialize');
    expect(result, 'Initialized');
  });

  test('registerForNotifications method test', () async {
    await dataWedge.registerForNotifications();
    expect(true, isTrue); // Verifying the method completes successfully
  });

  test('onScanResult stream test', () async {
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
}

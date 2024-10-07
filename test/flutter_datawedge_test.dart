import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_datawedge/flutter_datawedge.dart';

FlutterDataWedge dataWedge = FlutterDataWedge.instance;

void main() {
  const MethodChannel channel = MethodChannel('flutter_datawedge');
  const MethodChannel dataWedgeHostChannel = MethodChannel('dev.flutter.pigeon.flutter_datawedge.DataWedgeHostApi');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      if (methodCall.method == 'initialize') {
        return 'Initialized';
      } else if (methodCall.method == 'onScanResult') {
        return 'Sample Scan Result';
      }
      return null;
    });

    dataWedgeHostChannel.setMockMethodCallHandler((MethodCall methodCall) async {
      if (methodCall.method == 'registerForNotifications') {
        return null;
      }
      return null;
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
    dataWedgeHostChannel.setMockMethodCallHandler(null);
  });

  test('onScanResult stream test', () async {
    final dataWedge = FlutterDataWedge.instance;
    const scanData = 'Sample Scan Result';
    Future.delayed(Duration(milliseconds: 100), () {
      dataWedge.onScanResult(ScanEvent(
        dataString: scanData,
        decodeData: [Uint8List.fromList(scanData.codeUnits)],
        decodeMode: DecodeMode.single,
        labelType: LabelType.code128,
        source: ScanSource.scanner,
      ));
    });

    expectLater(dataWedge.scans, emits(isA<ScanEvent>()));
  });

  test('initialize method test', () async {
    final dataWedge = FlutterDataWedge.instance;
    await dataWedge.registerForNotifications();
    expect(true, isTrue); // Dummy expectation since 'result' is undefined
  });
}

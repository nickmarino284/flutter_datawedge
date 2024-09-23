import 'package:flutter_datawedge/flutter_datawedge.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late FlutterDataWedge sut;

  setUp(() {
    sut = FlutterDataWedge();
  });

  test('Test enableScanner requires initialization', () async {
    final result = await sut.enablePlugin();
    expect(result.isFailure, true);
    expect(result.maybeError, true);
  });

  test('Test activateScanner requires initialization', () async {
    final result = await sut.resumePlugin();
    expect(result.isFailure, true);
    expect(result.maybeError, true);
  });

  test('Test scannerControl requires initialization', () async {
    final result = await sut.enableAllDecoders('FlutterDataWedge');
    expect(result.isFailure, true);
    expect(result.maybeError, true);
  });
}

// ignore_for_file: lines_longer_than_80_chars, require_trailing_commas

class FlutterDatawedgeException implements Exception {
  FlutterDatawedgeException(this.message);
  
  final String message;
}

class NotInitializedException extends FlutterDatawedgeException {
  NotInitializedException()
      : super(
            'FlutterDataWedgePlus is not initialized. Call FlutterDataWedgePlus.initialize() first.');
}

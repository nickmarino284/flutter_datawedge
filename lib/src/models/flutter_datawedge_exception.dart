class FlutterDatawedgeException implements Exception {
  FlutterDatawedgeException(this.message);
  
  final String message;
}

class NotInitializedException extends FlutterDatawedgeException {
  NotInitializedException()
      : super(
            'FlutterDataWedgePlus is not initialized. Call FlutterDataWedgePlus.initialize() first.');
}

class NotInitializedException implements Exception {
  NotInitializedException([this.message = 'Plugin not initialized']);

  final String message;

  @override
  String toString() => 'NotInitializedException: $message';
}

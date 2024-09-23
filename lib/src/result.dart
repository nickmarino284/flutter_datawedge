import 'package:flutter_datawedge/src/exceptions.dart';

class CmdResult<T> {
  CmdResult.success(this.data) : error = null;
  CmdResult.failure(this.error) : data = null;

  final T? data;
  final String? error;

  bool get isSuccess => error == null;
  bool get isFailure => error != null;

  NotInitializedException get maybeError {
    if (error != null) {
      return NotInitializedException(error!);
    }
    throw Exception('No error');
  }
}

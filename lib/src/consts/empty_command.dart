// Define your enum
enum EmptyCommand {
  empty,
}

// Add the 'value' functionality using an extension
extension EmptyCommandExtension on EmptyCommand {
  String get value {
    switch (this) {
      case EmptyCommand.empty:
        return '';
    }
  }
}

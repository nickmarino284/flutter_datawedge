
import 'package:flutter_datawedge/flutter_datawedge.dart';


extension DatawedgeApiTargetsExtension on String {
  DatawedgeApiTargets toDatawedgeApiTarget() {
    switch (this) {
      case 'softScanTrigger':
        return DatawedgeApiTargets.softScanTrigger;
      case 'scannerPlugin':
        return DatawedgeApiTargets.scannerPlugin;
      case 'getProfiles':
        return DatawedgeApiTargets.getProfiles;
      case 'getActiveProfile':
        return DatawedgeApiTargets.getActiveProfile;
    // Add other cases as needed
      default:
        throw Exception('Unknown command: $this');
    }
  }
}

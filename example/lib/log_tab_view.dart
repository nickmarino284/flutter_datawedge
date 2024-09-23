import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_datawedge/flutter_datawedge.dart';

class LogTabView extends StatefulWidget {
  const LogTabView(this.fdw, {super.key});

  final FlutterDataWedge fdw;

  @override
  State<LogTabView> createState() => LogTabViewState();
}

class LogTabViewState extends State<LogTabView>
    with AutomaticKeepAliveClientMixin<LogTabView> {
  late final StreamSubscription<ActionResult> scannerEventSubscription;
  late final StreamSubscription<ScanResult> scanResultSubscription;
  late final StreamSubscription<ScannerStatus> scannerStatusSubscription;

  List<Widget> log = [];

  // TODO make display of logs more pretty

  void onScannerEvent(ActionResult event) {
    setState(() {
      log.add(_ActionResultLogTile(event));
    });
  }

  void onScanResult(ScanEvent event) {
    setState(() {
      log.add(_ScanResultLogTile(event));
    });
  }

  void onScannerStatus(StatusChangeEvent event) {
    setState(() {
      log.add(_ScannerStatusLogTile(event));
    });
  }

  @override
  void initState() {
    super.initState();
    // Subscribe to the necessary streams
    scannerEventSubscription = widget.fdw.scans.listen(onScannerEvent);
    scanResultSubscription = widget.fdw.scans.listen(onScanResult);
    scannerStatusSubscription = widget.fdw.status.listen(onScannerStatus);
  }

  @override
  void dispose() {
    // Cancel the subscriptions to avoid memory leaks
    scannerEventSubscription.cancel();
    scanResultSubscription.cancel();
    scannerStatusSubscription.cancel();
    super.dispose();
  }


  @override
  Widget build(BuildContext context) {
    super.build(context);
    return ListView.builder(
      itemCount: log.length,
      itemBuilder: (context, index) => log.reversed.elementAt(index),
    );
  }

  @override
  bool get wantKeepAlive => true;
}

class _ActionResultLogTile extends StatelessWidget {
  _ActionResultLogTile(this.actionResult);

  final ActionResult actionResult;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(actionResult.command.split('.').last),
      subtitle: Text(actionResult.logContent),
    );
  }
}

class _ScanResultLogTile extends StatelessWidget {
  _ScanResultLogTile(this.scanResult);

  final ScanEvent scanResult;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(scanResult.labelType.toString()),
      subtitle: Text(scanResult.dataString),
    );
  }
}

class _ScannerStatusLogTile extends StatelessWidget {
  const _ScannerStatusLogTile(this.scannerStatus);

  final ScannerStatus scannerStatus;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: const Text('Scanner Status'),
      subtitle: Text(scannerStatus.status.toString()),
    );
  }
}


extension ActionResultLog on ActionResult {
  String get logContent {
    return switch (DatawedgeApiTargets.fromString(this.command)) {
      DatawedgeApiTargets.softScanTrigger => '${result}',
      DatawedgeApiTargets.scannerPlugin =>
        result == "SUCCESS" ? '$result' : '${resultInfo!['RESULT_CODE']}',
      DatawedgeApiTargets.getProfiles => '${resultInfo!['profiles']}',
      DatawedgeApiTargets.getActiveProfile => '${resultInfo!['activeProfile']}',
    };
  }
}

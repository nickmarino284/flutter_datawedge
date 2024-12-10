import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_datawedge/flutter_datawedge.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(
    MaterialApp(
      title: 'Flutter DataWedge Example',
      theme: ThemeData(
        canvasColor: Colors.blue.shade200,
        cardColor: Colors.blue.shade100,
        appBarTheme: AppBarTheme(
          backgroundColor: Colors.blueGrey[100],
        ),
        cardTheme: CardTheme(
          color: Colors.blueGrey[200],
        ),
        colorScheme: ColorScheme.fromSwatch(primarySwatch: Colors.blueGrey),
        dialogTheme: DialogTheme(
          backgroundColor: Colors.blueGrey[800],
        ),
      ),
      home: const MyApp(),
    ),
  );
}

Future<void> dwTest(BuildContext context) async {
  final dataWedge = FlutterDataWedge.instance;

  if (kDebugMode) {
    print('Creating profile...');
  }
  try {
    await dataWedge.createProfile('TestFlutter');
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Profile created successfully')),
      );
    }
  } catch (e) {
    if (kDebugMode) {
      print('Creating profile failed: $e');
    }
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error creating profile: $e')),
      );
    }
  }

  final config = ProfileConfig(
    profileName: 'TestFlutter',
    profileEnabled: true,
    configMode: ConfigMode.update,
    barcodeParamters: PluginBarcodeParamters(
      scannerSelection: ScannerIdentifer.auto,
      enableHardwareTrigger: true,
      enableAimMode: true,
      upcEeanLinearDecode: true,
      dataBarToUpcEan: true,
    ),
  );

  await dataWedge.setConfig(config);

  try {
    await dataWedge.enableAllDecoders('TestFlutter');
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('All decoders enabled')),
      );
    }
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error enabling decoders: $e')),
      );
    }
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late StreamSubscription<ScanEvent> scanSub;
  final List<ScanEvent> _scans = [];
  StatusChangeEvent? _status;
  late StreamSubscription<StatusChangeEvent> statusSub;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await dwTest(context);
    });
    setupListeners();
  }

  Future<void> setupListeners() async {
    scanSub = FlutterDataWedge.instance.scans.listen((event) {
      setState(() {
        _scans.add(event);
      });
    });
    statusSub = FlutterDataWedge.instance.status.listen((event) {
      setState(() {
        _status = event;
      });
    });
  }

  @override
  void dispose() {
    statusSub.cancel();
    scanSub.cancel();
    super.dispose();
  }

  Widget _buildScan(BuildContext context, int index) {
    final scan = _scans[index];

    return ListTile(
      title: Text(
        scan.dataString,
        maxLines: 2,
      ),
      subtitle: Wrap(
        spacing: 4,
        children: [
          Chip(
            visualDensity: VisualDensity.compact,
            label: Text(scan.labelType.name),
          ),
          Chip(
            visualDensity: VisualDensity.compact,
            label: Text(scan.decodeMode.name),
          ),
          Chip(
            visualDensity: VisualDensity.compact,
            label: Text(scan.source.name),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🦓 Flutter DataWedge Example'),
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(color: Colors.grey.shade200, boxShadow: [
          BoxShadow(color: Colors.grey.shade400, blurRadius: 10)
        ]),
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              GestureDetector(
                onTapDown: (details) {
                  FlutterDataWedge.instance.softScanTrigger(on: true);
                },
                onTapUp: (details) {
                  FlutterDataWedge.instance.softScanTrigger(on: false);
                },
                onTapCancel: () {
                  FlutterDataWedge.instance.softScanTrigger(on: false);
                },
                child: ElevatedButton(
                  onPressed:
                  _status?.newState == ScannerState.waiting ? () {} : null,
                  child: const Text('Trigger'),
                ),
              ),
              Row(
                children: [
                  Text(
                    _status?.newState.toString() ?? 'Unknown',
                    style: Theme.of(context).textTheme.labelLarge,
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _status?.newState != ScannerState.disabled
                          ? null
                          : () async =>
                          FlutterDataWedge.instance.enablePlugin(),
                      child: const Text('Enable Scanner'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _status?.newState == ScannerState.disabled
                          ? null
                          : () async =>
                          FlutterDataWedge.instance.disablePlugin(),
                      child: const Text('Disable Scanner'),
                    ),
                  ),
                ],
              ),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: (_status?.newState == ScannerState.idle)
                          ? () => FlutterDataWedge.instance.resumePlugin()
                          : null,
                      child: const Text('Activate Scanner'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: (_status?.newState == ScannerState.waiting)
                          ? () => FlutterDataWedge.instance.suspendPlugin()
                          : null,
                      child: const Text('Deactivate Scanner'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
      body: ListView.separated(
        padding: const EdgeInsets.only(top: 8),
        separatorBuilder: (context, n) => const Divider(),
        itemBuilder: _buildScan,
        itemCount: _scans.length,
      ),
    );
  }
}

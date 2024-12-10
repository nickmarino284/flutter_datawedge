import 'package:flutter/material.dart';
import 'package:flutter_datawedge/flutter_datawedge.dart';

class ButtonTabView extends StatelessWidget {
  const ButtonTabView(this.fdw, { super.key });

  final FlutterDataWedge fdw;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: () async => fdw.enablePlugin(),
                  child: const Text('Enable Scanner'),
                ),
              ),
              Expanded(
                child: ElevatedButton(
                  onPressed: () async => fdw.disablePlugin(),
                  child: const Text('Disable Scanner'),
                ),
              ),
            ],
          ),
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: fdw.resumePlugin,
                  child: const Text('Activate Scanner'),
                ),
              ),
              Expanded(
                child: ElevatedButton(
                  onPressed: fdw.suspendPlugin,
                  child: const Text('Deactivate Scanner'),
                ),
              ),
            ],
          ),
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: () => fdw.enableAllDecoders('FlutterDataWedge'),
                  child: const Text('Scanner Control Activate'),
                ),
              ),
              Expanded(
                child: ElevatedButton(
                  onPressed: () => fdw.disableAllDecoders('FlutterDataWedge'),
                  child: const Text('Scanner Control DeActivate'),
                ),
              ),
            ],
          ),
          Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: fdw.registerForNotifications,
                  child: const Text('Request Profiles'),
                ),
              ),
              Expanded(
                child: ElevatedButton(
                  onPressed: fdw.getPackageIdentifer,
                  child: const Text('Request active Profile'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

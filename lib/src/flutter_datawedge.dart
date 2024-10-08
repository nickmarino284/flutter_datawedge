// ignore_for_file: lines_longer_than_80_chars
import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_datawedge/flutter_datawedge.dart';
import 'package:flutter_datawedge/logger.dart';
import 'package:flutter_datawedge/src/pigeon.dart';
import 'package:flutter_datawedge/src/result.dart';

import '../flutter_datawedge.dart';
import '../logger.dart';

/// Thrown if the profile we try to create already exists
class ProfileExistsError extends Error {}

/// Thrown if the profile name passed during creation is empty
class ProfileNameEmptyError extends Error {}

/// Main class used to interact with DataWedge. Use [FlutterDataWedge.instance]
/// to access a singleton instance
class FlutterDataWedge extends DataWedgeFlutterApi {
  /// Used to create a new [FlutterDataWedge]
  FlutterDataWedge()
      : assert(
          _instCount == 0,
          'Dont construct this class. Use .instance instead',
        ) {
    _instCount++;
    DataWedgeFlutterApi.setUp(this);
  }
  final DataWedgeHostApi _hostApi = DataWedgeHostApi();

  static int _instCount = 0;
  static final _flutterDataWedge = FlutterDataWedge();

  /// Get the singleton instance to this class
  static FlutterDataWedge get instance => _flutterDataWedge;

  /// create a new profile in data wedge with the name [profileName]
  /// if [autoActivate] is true, current app is added as an activation app
  /// to the profile.
  Future<void> createProfile(
    String profileName, {
    bool autoActivate = true,
  }) async {
    assert(profileName.isNotEmpty, 'Profile name cannot be empty');

    await _hostApi.createProfile(profileName);

    if (autoActivate) {
      final packageName = await _hostApi.getPackageIdentifer();

      final config = ProfileConfig(
        profileEnabled: true,
        profileName: profileName,
        configMode: ConfigMode.update,
        appList: [
          AppEntry(
            packageName: packageName,
            activityList: ['*'],
          ),
        ],
        intentParamters: PluginIntentParamters(
          intentOutputEnabled: true,
          intentAction: '$packageName.SCAN_EVENT',
          intentDelivery: IntentDelivery.broadcast,
        ),
      );

      await setConfig(config);
    }
  }

  /// Disables all decoders
  Future<CmdResult<List<Decoder>>> disableAllDecoders(String profileName) async {
    return _setAllDecoders(false, profileName);
  }

  /// Get the apps package identifier
  Future<String> getPackageIdentifer() {
    return _hostApi.getPackageIdentifer();
  }

  /// Enables all decoders
  Future<CmdResult<List<Decoder>>> enableAllDecoders(String profileName) async {
    return _setAllDecoders(true, profileName);
  }

  Future<CmdResult<List<Decoder>>> _setAllDecoders(
      bool enabled,
      String profileName,
  ) async {
    final availableDecoders = <Decoder>[];
    var anyFailures = false;
    var error = '';

    for (final decoder in Decoder.values) {
      try {
        await _hostApi.setDecoder(decoder, enabled, profileName);
        availableDecoders.add(decoder);
        logger.info('Decoder $decoder enabled');
      } catch (e) {
        logger.info('Decoder $decoder not available');
        anyFailures = true;
        error = e.toString();
      }
    }

    // If any decoders failed, return a failure CmdResult with the available decoders.
    if (anyFailures) {
      return CmdResult.failure( 'Could not be enabled due to an error: $error');
    }

    // If all decoders were successfully enabled, return success.
    return CmdResult.success(availableDecoders);
  }


  /// Update a profile config
  Future<void> setConfig(
    ProfileConfig config,
  ) async {
    await _hostApi.setProfileConfig(config);
  }

  final _scanEvents = StreamController<ScanEvent>.broadcast();
  final _actionResults = StreamController<ActionResult>.broadcast();
  final _statusChangeEvents = StreamController<StatusChangeEvent>.broadcast();


  /// The stream of [ScanEvent]s that are produced by DataWedge
  Stream<ScanEvent> get scans => _scanEvents.stream;

  /// The stream of [ActionResult]s that are produced by DataWedge
  Stream<ActionResult> get actions => _actionResults.stream;

  /// The stream of [StatusChangeEvent]s that are produced by DataWedge
  Stream<StatusChangeEvent> get status => _statusChangeEvents.stream;

  @override
  // @protected
  void onProfileChange() {
    logger.debug('Profile has changed');
  }

  @override
  // @protected
  void onConfigUpdate() {
    logger.debug('Data wedge notified of configuration change');
  }

  @override
  // @protected
  void onScanResult(ScanEvent scanEvent) {
    logger.debug('Scan result: $scanEvent');
    _scanEvents.add(scanEvent);
  }

  @override
  // @protected
  void onScannerStatusChanged(StatusChangeEvent statusEvent) {
    logger.debug('Scanner status changed: ${statusEvent.newState}');
    _statusChangeEvents.add(statusEvent);
  }

  /// Register for notifications from DataWedge. This is required to receive
  /// scan events and status change events
  Future<void> registerForNotifications() async {
    await _hostApi.registerForNotifications();
  }

  /// Set the soft scan trigger
  Future<void> softScanTrigger({required bool on}) async {
    await _hostApi.softScanTrigger(on);
  }

  // Plugin controls

  /// Resumes the scanning from suspended state

  Future<CmdResult<String>> resumePlugin() async {
    try {
      logger.debug('Resuming plugin');
      final resCode = await _hostApi.resumePlugin();
      return CmdResult.success(resCode);
    } catch (e) {
      logger.error('Error resuming plugin: $e', StackTrace.current);
      return CmdResult.failure('Failed to resume plugin: $e');
    }
  }

  /// Suspends scanning temporarily
  Future<CmdResult<String>> suspendPlugin() async {
    try {
      logger.debug('Suspending plugin');
      final resCode = await _hostApi.suspendPlugin();
      return CmdResult.success(resCode);
    } catch (e) {
      logger.error('Error suspending plugin: $e', StackTrace.current);
      return CmdResult.failure('Failed to suspend plugin: $e');
    }
  }

  /// Disables scanning
  Future<CmdResult<String>> disablePlugin() async {
    try {
      logger.debug('Disabling plugin');
      final resCode = await _hostApi.disablePlugin();
      return CmdResult.success(resCode);
    } catch (e) {
      logger.error('Error disabling plugin: $e', StackTrace.current);
      return CmdResult.failure('Failed to disable plugin: $e');
    }
  }

  /// Enables scanning
  Future<CmdResult<String>> enablePlugin() async {
    try {
      logger.debug('Enabling plugin');
      final resCode = await _hostApi.enablePlugin();
      return CmdResult.success(resCode);
    } catch (e) {
      logger.error('Error enabling plugin: $e', StackTrace.current);
      return CmdResult.failure('Failed to enable plugin: $e');
    }
  }


}

# Android receiver controls

Add Snapcast using the Quick Settings editor (API24+). Tap starts/stops the native
receiver using the existing server configuration; long press opens MainActivity.
The tile reflects the actual native process, including reconnecting or muted state.

A service-owned MediaSession selects this client's group and stream through the
configured Snapserver control port. Source capabilities gate play/pause/next/previous
commands (Stream.Control). Raw sources instead mute/unmute only this client via
Client.SetVolume, preserving volume; other receivers continue playing. Stop shuts
down the receiver. Idle, disconnection and native exit clear the media card.
Snapshots refresh on events/commands and after reconnect, without periodic polling.

On Android13+ ordinary POST_NOTIFICATIONS is intentionally not requested: the idle
foreground-service notice stays out of the drawer while Active apps still lists
the service. MediaStyle notifications with a valid MediaSession token are exempt.
Older Android versions retain the idle notice unless its channel is manually
blocked (API26+). No controls are injected into third-party screensavers.

Six unit tests cover room selection, missing/disconnected clients, mute and source
pause/stopped states. Release-based device checks on Android13 used an isolated
package and localhost JSON-RPC fixture: tile lifecycle, native exit/restart,
long-press UI, notification exemption, media state and transport commands.
No real-source audio, synchronization, reboot or API34/35 runtime test is claimed.

For Java-only verification with unchanged matching native libraries, set JAVA_HOME,
ANDROID_HOME and SNAPCAST_NATIVE_DIR (ABI/libsnapclient.so directory), then run
`./tools/build-local.sh assembleRelease testReleaseUnitTest lintRelease`.
The default native-source build remains available. An isolated integration package
can be built with `-PclientApplicationId=local.snapcast.preview assembleDebug`.
Keep signing keys and app data outside Git; preserve settings/client UUID when
replacing an APK with a different signing certificate.

References:
- https://developer.android.com/develop/ui/views/quicksettings-tiles
- https://developer.android.com/develop/ui/views/notifications/notification-permission
- https://developer.android.com/media/implement/surfaces/mobile
- https://github.com/snapcast/snapcast/blob/develop/doc/json_rpc_api/control.md

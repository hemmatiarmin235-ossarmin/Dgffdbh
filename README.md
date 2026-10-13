# ARMIN DECK v2

Android Java launcher prototype, package `com.armin.deck`, min SDK 26.

Features implemented:
- Lists launchable apps installed on the phone (Android package visibility permission is broad and used for the app list).
- Launches selected apps through Android's normal launch intent.
- Optional movable floating DECK bubble over other apps, with a collapsible panel.
- Functional media-volume slider.
- Do Not Disturb toggle after Android's notification-policy access is granted.
- Honest FPS limitation note: ordinary third-party apps cannot reliably read another app/game's true FPS on standard Android.

Build: GitHub Actions workflow `.github/workflows/build-apk.yml`, then download `ARMIN-DECK-APK` artifact and extract `app-debug.apk`.

Permissions: The user must explicitly grant Display over other apps. DND control requires separate notification policy access. This is not a GPU overclock or game-graphics modifier.

# aDevice Tracker

Native Android controller for Android devices connected to the existing Debian `ws-scrcpy` backend. The Android app is Java/native and does not use WebView. The existing browser UI remains available.

## Current status
- Native tracker connection and device list: implemented.
- Native screen streaming: initial `SurfaceView` + `MediaCodec` implementation; real-device playback validation pending.
- Native touch control: initial scrcpy binary-protocol implementation; real-device input validation pending.
- Debian backend tutorial: documented from a clean Debian setup through service verification.

A successful APK build/signature check does **not** prove that live video and remote touch work on a physical device. Those remain explicit acceptance tests.

## Documentation
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Android build and installation](docs/BUILD.md)
- [Native streaming and touch protocol](docs/NATIVE-STREAMING.md)
- [Debian backend installation tutorial](docs/DEBIAN-BACKEND-GUIDE.md)
- [Safety boundaries](docs/SAFETY.md)
- [Streaming protocol audit](docs/STREAMING-PROTOCOL.md)
- [Development changelog](docs/CHANGELOG.md)

## Source
Android Java source and build script are in [`android/`](android/).

## Operational boundaries
The existing Debian Web UI remains on port 8000. The Android client must not stop or reconfigure ws-scrcpy, ADB, scrcpy, existing port forwards, or LAN access as part of normal app development. No device secrets, private ADB keys, debug keystores, or credentials belong in this public repository.

Repository is intentionally public as requested.
# aDevice Tracker Android source

Native Android Java client. Project-level documentation is in the repository root under `docs/`.

## Build on Debian/Linux

Requirements: JDK, Android SDK Platform 35, Build Tools 35.0.0.

```bash
git clone https://github.com/kaktuti895-cpu/aDevice-Tracker.git
cd aDevice-Tracker/android
./scripts/build-debug.sh
```

APK output: `build/apk/aDevice-Tracker-debug.apk`.

Do not commit `build/`, debug keystores, signing credentials, or device-specific secrets.

Native streaming and touch are an initial implementation and still require real-device verification. See [native streaming notes](../docs/NATIVE-STREAMING.md).
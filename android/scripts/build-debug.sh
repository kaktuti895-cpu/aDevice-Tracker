#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/android-sdk}}"
VER="${BUILD_TOOLS_VERSION:-35.0.0}"
BT="$SDK/build-tools/$VER"
JAR="$SDK/platforms/android-35/android.jar"
KEYSTORE="${ANDROID_USER_HOME:-$HOME/.android}/debug.keystore"
OUT="$ROOT/build"
for f in "$BT/aapt2" "$BT/d8" "$BT/zipalign" "$BT/apksigner" "$BT/aapt" "$JAR"; do
  [[ -f "$f" ]] || { echo "Missing: $f" >&2; exit 1; }
done
for c in javac jar keytool; do command -v "$c" >/dev/null || { echo "Missing: $c" >&2; exit 1; }; done
mkdir -p "$OUT/resources" "$OUT/apk" "$(dirname "$KEYSTORE")"
rm -rf "$OUT/generated" "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/generated" "$OUT/classes" "$OUT/dex"
rm -f "$OUT/resources/compiled.zip" "$OUT/apk/"*.apk
"$BT/aapt2" compile --dir "$ROOT/res" -o "$OUT/resources/compiled.zip"
"$BT/aapt2" link -I "$JAR" --manifest "$ROOT/AndroidManifest.xml" --java "$OUT/generated" --min-sdk-version 23 --target-sdk-version 35 --version-code 5 --version-name 0.2.3 -R "$OUT/resources/compiled.zip" -o "$OUT/apk/app-unsigned.apk"
javac -source 8 -target 8 -classpath "$JAR" -d "$OUT/classes" $(find "$ROOT/src" -name '*.java' -print)
"$BT/d8" --min-api 23 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name "*.class" -print)
cp "$OUT/apk/app-unsigned.apk" "$OUT/apk/app-unsigned-with-dex.apk"
(cd "$OUT/dex" && jar uf "$OUT/apk/app-unsigned-with-dex.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/apk/app-unsigned-with-dex.apk" "$OUT/apk/aDevice-Tracker-aligned.apk"
if [[ ! -f "$KEYSTORE" ]]; then
  keytool -genkeypair -v -keystore "$KEYSTORE" -storepass android -keypass android -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -keysize 2048 -validity 10000
  chmod 600 "$KEYSTORE"
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$OUT/apk/aDevice-Tracker-debug.apk" "$OUT/apk/aDevice-Tracker-aligned.apk"
"$BT/apksigner" verify --verbose "$OUT/apk/aDevice-Tracker-debug.apk"
"$BT/aapt" dump badging "$OUT/apk/aDevice-Tracker-debug.apk" | sed -n '1,12p'
sha256sum "$OUT/apk/aDevice-Tracker-debug.apk"
echo "APK: $OUT/apk/aDevice-Tracker-debug.apk"
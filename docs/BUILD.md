# Tutorial build dan instalasi

Panduan membangun APK debug dari source Java pada Debian.

## 1. Persyaratan
- Debian/Linux.
- OpenJDK dengan `javac` dan `keytool`.
- Android SDK Platform 35.
- Android Build Tools 35.0.0.
- Lokasi SDK pada server proyek: `/opt/android-sdk`.

Periksa toolchain:

```bash
java -version
javac -version
test -f /opt/android-sdk/platforms/android-35/android.jar
test -x /opt/android-sdk/build-tools/35.0.0/aapt2
test -x /opt/android-sdk/build-tools/35.0.0/d8
test -x /opt/android-sdk/build-tools/35.0.0/zipalign
test -x /opt/android-sdk/build-tools/35.0.0/apksigner
```

Jika SDK berada di lokasi lain:

```bash
export ANDROID_SDK_ROOT=/lokasi/android-sdk
```

## 2. Ambil source

```bash
git clone https://github.com/kaktuti895-cpu/aDevice-Tracker.git
cd aDevice-Tracker/android
```

## 3. Build

```bash
chmod +x scripts/build-debug.sh
./scripts/build-debug.sh
```

Script menjalankan aapt2, javac untuk semua file Java, d8, zipalign, dan apksigner. Hasil:

```text
build/apk/aDevice-Tracker-debug.apk
```

## 4. Verifikasi

```bash
/opt/android-sdk/build-tools/35.0.0/apksigner verify --verbose build/apk/aDevice-Tracker-debug.apk
/opt/android-sdk/build-tools/35.0.0/aapt dump badging build/apk/aDevice-Tracker-debug.apk | head -20
sha256sum build/apk/aDevice-Tracker-debug.apk
```

Metadata:
- Package: `com.astuti.adevicetracker`
- Label: aDevice Tracker
- Minimum SDK: 23
- Target SDK: 35
- Launcher activity: `com.astuti.adevicetracker.MainActivity`

## 5. Instalasi opsional melalui ADB

```bash
adb devices
adb install -r build/apk/aDevice-Tracker-debug.apk
```

Tutorial ini tidak menjalankan `adb forward`, tidak menghentikan scrcpy, dan tidak me-restart ws-scrcpy. APK juga dapat disalin ke ponsel dan dipasang manual.

## 6. Jalankan
1. Buka aDevice Tracker dari launcher Android.
2. Masukkan IP Debian dan port 8000.
3. Tekan Hubungkan.
4. Pilih tombol **Lihat layar & kontrol** pada perangkat aktif.

Web UI browser tetap tersedia di `http://IP-DEBIAN:8000/`.

## 7. Troubleshooting
- Gagal terhubung ke tracker: periksa jaringan, IP, port 8000, dan akses Web UI dari browser.
- Daftar kosong: periksa apakah tracker Debian mendeteksi perangkat.
- Stream gagal: periksa status perangkat, interface IPv4, dan apakah scrcpy server pada perangkat tersedia di port 8886.
- Gambar tidak tampil: implementasi decoder masih dalam tahap uji perangkat nyata; catat model Android, versi OS, dan pesan status aplikasi.
- Jangan restart layanan atau mengubah port-forward hanya untuk mengatasi masalah aplikasi.

## Signing
Script memakai debug keystore lokal pada `~/.android/debug.keystore`. Jangan unggah keystore atau password ke repository. APK debug dengan key berbeda tidak selalu dapat dipasang sebagai update di atas APK yang ditandatangani key lain.
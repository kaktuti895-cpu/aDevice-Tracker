# Tutorial Build dan Instalasi

## Prasyarat
- Debian/Linux.
- JDK yang sesuai konfigurasi proyek.
- Android SDK, platform-tools, dan build-tools.
- Perangkat Android untuk pengujian.

## Periksa lingkungan
```bash
java -version
adb version
adb devices -l
```

Pastikan perangkat pengujian berstatus `device`, bukan `unauthorized`.

## Build debug
Dari root proyek:
```bash
./scripts/build-debug.sh
```

Periksa APK:
```bash
ls -lh build/apk/aDevice-Tracker-debug.apk
sha256sum build/apk/aDevice-Tracker-debug.apk
apksigner verify --verbose build/apk/aDevice-Tracker-debug.apk
```

## Instalasi
```bash
adb install -r build/apk/aDevice-Tracker-debug.apk
```

Jika Android menolak instalasi, periksa prompt keamanan pada perangkat. Jangan menonaktifkan perlindungan sistem secara global.

## Pengujian dasar
1. Buka aplikasi dari launcher.
2. Masukkan host dan port backend.
3. Hubungkan ke backend.
4. Pastikan daftar perangkat muncul.
5. Catat log sebelum menguji streaming.

Build APK harus terpisah dari pemeliharaan layanan server. Jangan restart/kill ws-scrcpy, ADB, scrcpy, atau port-forward sebagai bagian dari build aplikasi.

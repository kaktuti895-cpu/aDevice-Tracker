# aDevice Tracker

Aplikasi Android native untuk memantau perangkat Android yang terhubung ke server Debian melalui tracker ws-scrcpy yang sudah ada.

Aplikasi ini bukan WebView dan tidak menggantikan antarmuka browser. Satu backend Debian melayani dua frontend mandiri:
- Aplikasi Android native untuk pemantauan perangkat.
- Web UI yang tetap tersedia di http://192.168.2.233:8000/.

## Status 0.1.0

| Bagian | Status |
|---|---|
| Aplikasi Android native Java | Tersedia |
| Ikon launcher dan halaman koneksi | Tersedia |
| Koneksi WebSocket tracker | Tersedia |
| Daftar perangkat dan metadata dasar | Tersedia |
| Web UI browser | Tetap dipertahankan |
| Live screen streaming di aplikasi | Belum dibuat |
| Kontrol sentuh dari aplikasi | Belum dibuat |
| Perintah start/stop perangkat atau server | Tidak ada pada MVP |

MVP bersifat read-only.

## Dokumentasi
- [Arsitektur](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Tutorial build dan instalasi](docs/BUILD.md)
- [Batas keselamatan](docs/SAFETY.md)

## Endpoint
- Host Debian default: 192.168.2.233
- Port: 8000
- WebSocket: ws://IP-DEBIAN:8000/?action=goog-device-list
- Web UI: http://IP-DEBIAN:8000/

## Build
Pada Debian dengan Android SDK Platform 35, Build Tools 35.0.0, dan JDK:

~~~bash
cd /home/pattar/adevice-tracker-android
./scripts/build-debug.sh
~~~

APK dibuat di build/apk/aDevice-Tracker-debug.apk.

## Struktur
~~~text
aDevice-Tracker/
├── AndroidManifest.xml
├── README.md
├── .gitignore
├── docs/
├── res/drawable/ic_launcher.xml
├── scripts/build-debug.sh
└── src/com/astuti/adevicetracker/MainActivity.java
~~~

Folder build dan keystore lokal tidak diunggah ke GitHub.

## Prinsip
1. Aplikasi Android tetap native, bukan WebView.
2. Web UI browser tetap dapat diakses.
3. Backend ws-scrcpy yang ada digunakan bersama, bukan diganti.
4. ADB, scrcpy, port-forward, dan LAN tidak diubah oleh pekerjaan aplikasi.
5. Streaming dan input ditambahkan setelah audit protokol dan uji dampak.
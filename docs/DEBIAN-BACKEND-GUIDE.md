# Panduan Backend Debian: ws-scrcpy dari Nol

Panduan ini menjelaskan cara membangun backend Debian untuk aDevice Tracker dan Web UI browser menggunakan proyek NetrisTV/ws-scrcpy. Isinya mencakup instalasi, build, ADB, layanan systemd, verifikasi, dan pemecahan masalah.

> **Penting:** tutorial ini ditulis dari audit server Debian yang saat ini berjalan. Jalankan langkah instalasi pada mesin Debian baru atau lingkungan uji. Jangan menyalin langkah restart/stop ke server produksi yang sedang dipakai tanpa jadwal dan persetujuan.

## 1. Gambaran arsitektur

~~~text
Browser Web UI ───── HTTP/WebSocket :8000 ─────┐
                                               │
Aplikasi aDevice Tracker (native, rencana) ────┤
                                               ▼
                                  Debian: ws-scrcpy (Node.js)
                                               │
                                  ADB server (TCP 5037)
                                               │
                                USB / ADB-connected Android
                                               │
                             scrcpy WebSocket server di perangkat
~~~

- **ws-scrcpy** adalah backend Node.js dan juga menyajikan Web UI.
- **ADB** menemukan perangkat Android dan mengirim perintah ke perangkat.
- Proyek ini menggunakan scrcpy server fork dengan dukungan WebSocket. Jangan menggantinya begitu saja dengan scrcpy-server.jar upstream biasa.
- Web UI browser tetap tersedia. Aplikasi aDevice Tracker native nantinya menjadi klien tambahan, bukan pengganti Web UI.

## 2. Persyaratan

- Debian 12/13 atau Debian turunan yang kompatibel.
- Akses sudo.
- Koneksi internet untuk mengunduh paket dan source.
- Node.js 20 atau lebih baru yang kompatibel dengan dependency proyek.
- Git, npm, Python 3, make, compiler C/C++ untuk dependency native.
- Android Platform Tools (ADB).
- Perangkat Android dengan USB debugging aktif.

## 3. Instal paket sistem

~~~bash
sudo apt update
sudo apt install -y git curl ca-certificates build-essential python3 make g++ adb android-sdk-platform-tools-common
~~~

Periksa versi dan ketersediaan perintah:

~~~bash
git --version
node --version
npm --version
python3 --version
adb version
~~~

Jika Node.js belum terpasang atau versinya terlalu lama, pasang rilis Node.js LTS yang didukung dari sumber resmi Node.js, lalu buka terminal baru dan ulangi pemeriksaan. Server yang diaudit saat panduan ini dibuat menggunakan Node.js **v20.19.2** dan npm **9.2.0**.

## 4. Ambil source ws-scrcpy

Repositori upstream yang digunakan:

~~~bash
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/NetrisTV/ws-scrcpy.git
cd ws-scrcpy
~~~

Untuk penggunaan produksi, pilih tag/rilis yang sudah diuji, bukan mengambil perubahan branch secara sembarang. Catat commit yang digunakan:

~~~bash
git rev-parse --short HEAD
git describe --tags --always
~~~

## 5. Konfigurasi build Android

Build default proyek mengaktifkan Android tracker (INCLUDE_GOOG: true) dan menonaktifkan iOS (INCLUDE_APPL: false). Port HTTP default adalah **8000**.

Lihat konfigurasi default:

~~~bash
cat webpack/default.build.config.json
~~~

Bila perlu override, buat build.config.override.json dari konfigurasi yang ingin diubah. Jangan menaruh token, password, private key, atau data rahasia di file build config. Untuk konfigurasi standar Android, override tidak wajib.

## 6. Instal dependency dan build

Dari root repositori:

~~~bash
npm install
npm run dist
~~~

Periksa hasil build:

~~~bash
test -f dist/index.js && echo "Build backend tersedia"
~~~

Catatan:
- npm install memasang dependency proyek dan menjalankan postinstall yang disediakan proyek.
- npm run dist membangun bundle server ke folder dist.
- Jika dependency native gagal dibangun, periksa versi Node.js, Python 3, make, g++, dan pesan error npm sebelum mencoba ulang.

## 7. Sambungkan perangkat Android dengan ADB

Di perangkat Android:
1. Aktifkan Developer options.
2. Aktifkan USB debugging.
3. Sambungkan kabel USB data ke Debian.
4. Buka layar perangkat dan setujui dialog RSA debugging yang menampilkan fingerprint komputer.

Di Debian:

~~~bash
adb start-server
adb devices -l
~~~

Status perangkat harus device. Jika tertulis unauthorized, buka kunci Android dan setujui dialog RSA. Jika daftar kosong, periksa kabel data, port USB, mode USB, aturan udev, dan izin pengguna.

**Keamanan:** jangan menonaktifkan verifikasi RSA atau membagikan private key ADB. Jangan gunakan adb kill-server pada server yang sedang melayani sesi aktif.

## 8. Uji backend secara manual pada mesin uji

Jalankan dari root proyek setelah build:

~~~bash
cd ~/projects/ws-scrcpy
node ./dist/index.js
~~~

Dari browser pada jaringan yang sama, buka:

~~~text
http://ALAMAT_IP_DEBIAN:8000
~~~

Untuk mengetahui alamat IPv4 Debian:

~~~bash
hostname -I
~~~

Hentikan proses uji dengan Ctrl+C hanya pada terminal proses uji tersebut. Jangan menghentikan service produksi yang sudah berjalan.

## 9. Jalankan otomatis dengan systemd

Untuk server baru, buat unit systemd. Ganti YOUR_LINUX_USER dengan nama pengguna Linux yang memiliki source proyek dan akses ADB. Sesuaikan path bila proyek disimpan di tempat lain.

~~~bash
sudo nano /etc/systemd/system/ws-scrcpy.service
~~~

Isi:

~~~ini
[Unit]
Description=ws-scrcpy remote screen streaming backend
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=YOUR_LINUX_USER
WorkingDirectory=/home/YOUR_LINUX_USER/projects/ws-scrcpy/dist
Environment=HOME=/home/YOUR_LINUX_USER
ExecStartPre=/usr/bin/adb start-server
ExecStart=/usr/bin/node /home/YOUR_LINUX_USER/projects/ws-scrcpy/dist/index.js
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
~~~

Aktifkan pada instalasi baru:

~~~bash
sudo systemctl daemon-reload
sudo systemctl enable --now ws-scrcpy.service
sudo systemctl status ws-scrcpy.service --no-pager
~~~

Perintah manajemen dan diagnosis:

~~~bash
sudo systemctl status ws-scrcpy.service
sudo journalctl -u ws-scrcpy.service -n 100 --no-pager
sudo journalctl -u ws-scrcpy.service -f
~~~

Untuk memperbarui source pada server baru: lakukan backup dan catat commit, build ulang, kemudian jadwalkan restart service. Jangan melakukan restart otomatis pada mesin yang sedang digunakan tanpa persetujuan operator.

## 10. Verifikasi port dan ADB

~~~bash
ss -ltnp | grep -E ':(8000|8001|8002)\b'
adb devices -l
adb forward --list
~~~

Makna port pada deployment yang diaudit:
- **8000/TCP:** Web UI dan endpoint WebSocket ws-scrcpy.
- **8001/TCP dan 8002/TCP:** ADB local-forward yang dibuat untuk dua sesi/perangkat pada server saat audit. Port ini bukan pengganti port Web UI dan dapat berubah sesuai sesi/konfigurasi.
- **5037/TCP:** ADB server lokal. Jangan membukanya ke jaringan umum.

Jangan menambahkan forward secara manual ke port yang sudah dipakai. Periksa adb forward --list terlebih dahulu.

## 11. Konfigurasi keamanan jaringan

Penting: README proyek ws-scrcpy menyatakan bahwa instalasi default tidak menyediakan autentikasi dan koneksi HTTP/WebSocket tidak terenkripsi. Build server juga dapat mendengarkan pada semua interface jaringan.

Karena itu:
- Gunakan hanya pada LAN tepercaya atau jaringan privat/VPN.
- Jangan melakukan port-forward router dari internet langsung ke port 8000, 8001, 8002, atau 5037.
- Batasi akses dengan firewall ke subnet LAN yang memang dipercaya.
- Untuk akses lintas jaringan, gunakan VPN atau reverse proxy HTTPS dengan autentikasi yang dikonfigurasi dan diuji secara terpisah.
- Jangan mengunggah konfigurasi rahasia, private key, token, atau ADB private key ke repositori.

## 12. Pemecahan masalah

### Web UI tidak dapat dibuka

~~~bash
sudo systemctl status ws-scrcpy.service --no-pager
sudo journalctl -u ws-scrcpy.service -n 100 --no-pager
ss -ltnp | grep ':8000'
~~~

Periksa IP Debian, firewall, status service, dan apakah port 8000 sudah digunakan proses lain.

### ADB tidak menemukan perangkat

~~~bash
adb devices -l
lsusb
~~~

Periksa kabel data, USB debugging, dialog RSA, udev rules, dan izin pengguna. Jangan reset ADB server saat ada sesi aktif.

### Build gagal

~~~bash
node --version
npm --version
python3 --version
make --version
g++ --version
~~~

Periksa log npm dan dependency native. Jangan menghapus package-lock.json sebagai langkah pertama karena itu dapat mengubah versi dependency.

### Perangkat muncul tetapi streaming gagal

Periksa log service dan kompatibilitas perangkat. ws-scrcpy ini memakai scrcpy WebSocket fork dan server JAR yang dibundel dalam repositori. Mengganti JAR dengan versi scrcpy standar yang tidak kompatibel dapat merusak streaming.

## 13. Snapshot deployment Debian yang diaudit

Bagian ini adalah kondisi yang benar-benar ditemukan pada server proyek ketika dokumentasi dibuat, bukan nilai universal untuk semua instalasi:

| Komponen | Hasil audit |
|---|---|
| OS | Debian GNU/Linux 13 (trixie) |
| Kernel | Linux 6.12.117-amd64 |
| Node.js | v20.19.2 |
| npm | 9.2.0 |
| ws-scrcpy | 0.9.0-dev |
| Source | /home/pattar/ws-scrcpy |
| Build entry | /home/pattar/ws-scrcpy/dist/index.js |
| systemd unit | ws-scrcpy.service |
| Service user | pattar |
| Working directory | /home/pattar/ws-scrcpy/dist |
| Web UI | TCP 8000, listening |
| ADB forwards saat audit | TCP 8001 dan 8002 ke perangkat, remote tcp:8886 |
| ADB devices saat audit | 2 perangkat berstatus device |

Pemeriksaan dilakukan secara read-only. Tidak ada service yang dihentikan, konfigurasi ADB yang diubah, atau port-forward yang dibuat ulang selama audit.

## 14. Kaitan dengan aDevice Tracker

aDevice Tracker adalah proyek Android native terpisah yang akan menjadi klien tambahan backend ini. Web UI tetap dipertahankan. Implementasi streaming native harus mengikuti protokol tracker, stream metadata, frame video, dan pesan kontrol biner ws-scrcpy yang sebenarnya.

**Status integrasi:** backend berjalan; aplikasi native masih perlu menyelesaikan decoder video, kontrol sentuh, dan uji end-to-end. Keberhasilan backend/Web UI tidak berarti aplikasi native sudah memiliki streaming dan kontrol sentuh.

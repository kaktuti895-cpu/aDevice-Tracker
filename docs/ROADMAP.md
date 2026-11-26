# Roadmap pengembangan

Status dibedakan antara fitur yang sudah tersedia dan target yang belum diimplementasikan.

## 0.1.0 - MVP
- [x] Aplikasi Android native Java.
- [x] Ikon launcher.
- [x] Input host dan port Debian.
- [x] WebSocket ke tracker ws-scrcpy.
- [x] Parsing devicelist dan device.
- [x] Daftar perangkat dan metadata.
- [x] Web UI browser tetap dipertahankan.
- [x] Mode read-only.

## 0.2 - Stabilitas dan UX
- [x] Simpan endpoint terakhir secara lokal (host dan port tersimpan di SharedPreferences).
- [ ] Status koneksi dan reconnect yang lebih baik.
- [ ] Penanganan timeout, disconnect, dan JSON tidak valid.
- [ ] Pembaruan daftar tanpa duplikasi.
- [ ] Halaman detail perangkat.
- [ ] Uji beberapa ukuran layar dan versi Android.

## 0.3 - Live screen
- [x] Audit protokol streaming ws-scrcpy yang sudah ada.
- [x] Tentukan jalur streaming kompatibel dengan backend saat ini.
- [x] Buat implementasi awal layar native (SurfaceView + MediaCodec).
- [ ] Verifikasi decoder dan gambar bergerak pada perangkat nyata.
- [ ] Kontrol resolusi dan orientasi.
- [ ] Uji satu perangkat dahulu, lalu multi-device.
- [ ] Pastikan Web UI browser tidak terganggu.

## 0.4 - Remote input
- [x] Rancang pemetaan tap dan swipe berdasarkan TouchControlMessage biner.
- [x] Buat implementasi awal input sentuh melalui WebSocket scrcpy.
- [ ] Uji tap, swipe, dan pelepasan sentuh pada perangkat nyata.
- [ ] Tambahkan tombol dan keyboard.
- [ ] Tampilkan perangkat yang sedang dikendalikan.
- [ ] Konfirmasi tindakan berisiko.
- [ ] Uji agar input tidak tertukar antarperangkat.

## 1.0 - Rilis stabil
- [ ] Autentikasi dan keamanan koneksi.
- [ ] Logging tanpa data sensitif.
- [ ] Uji reconnect dan pemulihan sesi.
- [ ] Dokumentasi final.
- [ ] APK release dengan signing key yang disimpan aman.
- [ ] Uji regresi browser, ADB, scrcpy, port-forward, dan LAN.

## Kriteria penerimaan
- Browser Web UI tetap dapat dibuka.
- Aplikasi native berjalan tanpa WebView.
- Daftar perangkat cocok dengan tracker Debian.
- Tidak ada perubahan tidak disengaja pada ADB, scrcpy, port-forward, atau LAN.
- Streaming dan remote input hanya dinyatakan selesai setelah diuji pada perangkat nyata.
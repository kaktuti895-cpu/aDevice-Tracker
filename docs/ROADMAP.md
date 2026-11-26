# Roadmap

## Tahap 1: Fondasi native
- [x] Aplikasi Android native dan launcher.
- [x] Konfigurasi endpoint backend.
- [x] Simpan host dan port terakhir secara lokal.
- [x] Koneksi awal dan daftar perangkat.
- [ ] Penanganan reconnect dan error state lengkap.

## Tahap 2: Streaming layar
- [x] Audit protokol tracker dan jalur stream ws-scrcpy dari source backend.
- [ ] Implementasi handshake dan sesi streaming native.
- [ ] Decode video dan render ke SurfaceView/TextureView.
- [ ] Rotasi, resize, lifecycle, dan pemulihan koneksi.
- [ ] Uji pada perangkat target nyata.

## Tahap 3: Kontrol sentuh
- [x] Audit struktur pesan kontrol biner upstream.
- [ ] Pemetaan koordinat view ke resolusi perangkat.
- [ ] Tap, long-press, swipe, dan multi-touch jika didukung protokol.
- [ ] Tombol navigasi dan input teks sesuai kemampuan backend.
- [ ] Uji latensi dan kestabilan.

## Tahap 4: Integrasi dan rilis
- [ ] Uji aplikasi native dan Web UI berjalan bersamaan.
- [ ] Uji beberapa perangkat dan pergantian target.
- [ ] Build release, tanda tangan, checksum, dan panduan instalasi.
- [ ] Catat hasil uji dan batasan yang diketahui.

## Pekerjaan paralel
- [x] README dan arsitektur awal.
- [x] Roadmap.
- [x] Tutorial build dan instalasi.
- [x] Aturan keselamatan.
- [x] Audit protokol streaming dan input (STREAMING-PROTOCOL.md).
- [ ] Sinkronisasi source aplikasi lengkap dan riwayat commit.

## Validasi build
- Debug APK berhasil dibangun dan diverifikasi secara lokal.
- Versi aplikasi: 0.1.0.
- Package: com.astuti.adevicetracker.
- Streaming dan input native belum diimplementasikan atau diuji end-to-end.

# Roadmap

## Tahap 1: Fondasi native
- [x] Aplikasi Android native dan launcher.
- [x] Konfigurasi endpoint backend.
- [x] Simpan host dan port terakhir secara lokal.
- [x] Koneksi awal dan daftar perangkat.
- [ ] Penanganan reconnect dan error state lengkap.

## Tahap 2: Streaming layar
- [ ] Audit protokol ws-scrcpy sesuai versi backend.
- [ ] Implementasi handshake dan sesi streaming native.
- [ ] Decode video dan render ke SurfaceView/TextureView.
- [ ] Rotasi, resize, lifecycle, dan pemulihan koneksi.
- [ ] Uji pada perangkat target nyata.

## Tahap 3: Kontrol sentuh
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
- [ ] Sinkronisasi source aplikasi lengkap dan riwayat commit.

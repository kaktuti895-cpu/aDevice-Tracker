# Riwayat Pengembangan

## 2026-11-26 - Native streaming prototype
- Audit protokol streaming ws-scrcpy selesai pada sisi metadata awal, frame H.264, dan format kontrol sentuh.
- Menambahkan `StreamActivity` native berbasis `SurfaceView` dan `MediaCodec`.
- Menambahkan parser metadata `scrcpy_initial` dan pemisahan NAL H.264 Annex-B.
- Menambahkan pengiriman pesan biner `TYPE_TOUCH` untuk down, move, dan up.
- Menambahkan tombol pembuka stream pada kartu perangkat aktif.
- Memperbarui script build agar mengompilasi seluruh file Java.
- APK debug berhasil dibangun dan signature diverifikasi.
- Uji stream, gambar bergerak, dan kontrol sentuh pada perangkat fisik masih tertunda.

## 0.1.0 - Fondasi awal
- Aplikasi Android native dengan launcher.
- Konfigurasi host dan port backend.
- Koneksi awal WebSocket dan tampilan daftar perangkat.
- Endpoint terakhir disimpan lokal.

## Pekerjaan berikutnya
- Uji koneksi WebSocket streaming pada perangkat nyata.
- Verifikasi decoding H.264 dan sinkronisasi ukuran/orientasi.
- Uji tap, swipe, pelepasan sentuh, dan pemulihan koneksi.
- Uji berdampingan dengan Web UI, ADB, scrcpy, dan port-forward.

Nomor build dan status rilis dicatat berdasarkan commit serta hasil uji yang benar-benar dilakukan.
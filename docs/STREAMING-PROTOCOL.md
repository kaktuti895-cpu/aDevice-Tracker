# Streaming dan Input: Audit Protokol ws-scrcpy

Dokumen ini mencatat hasil pemeriksaan kode ws-scrcpy lokal. Ini adalah spesifikasi kerja untuk integrasi native, bukan klaim bahwa streaming sudah berfungsi di aplikasi Android.

## 1. Dua koneksi yang berbeda

### Daftar perangkat
- WebSocket endpoint: `/?action=goog-device-list`
- Server mengirim pesan teks JSON.
- Pesan awal menggunakan `type: "devicelist"` dengan `data.list`.
- Perubahan perangkat menggunakan `type: "device"` dengan `data.device`.

### Streaming perangkat
- Aksi stream: `stream`.
- Parameter wajib yang dibaca klien web mencakup `udid`, `ws`, dan `player`.
- Koneksi stream dibuka ke URL WebSocket yang diberikan melalui parameter `ws`.
- Koneksi ini membawa data biner, bukan daftar perangkat JSON.

## 2. Format data stream

Implementasi `StreamReceiver` pada ws-scrcpy mengenali:
- Paket metadata awal dengan magic bytes UTF-8 `scrcpy_initial`.
- Setelah magic bytes terdapat field nama perangkat sepanjang 64 byte.
- Metadata berikutnya berisi jumlah display, informasi display, jumlah koneksi, data screen info, pengaturan video, daftar encoder, dan client ID.
- Paket biner yang bukan metadata awal atau pesan perangkat diteruskan sebagai data video ke player.
- Pesan perangkat menggunakan magic bytes milik `DeviceMessage`.

Native client perlu mem-parsing metadata biner secara tepat sebelum menginisialisasi decoder video. Jangan menganggap setiap frame biner sebagai frame video.

## 3. Input sentuh dan tombol

Klien web mengirim objek kontrol melalui `ControlMessage.toBuffer()` pada WebSocket stream. Input tombol menggunakan `KeyCodeControlMessage`; interaksi layar ditangani oleh `FeaturedInteractionHandler` dan kelas-kelas pesan kontrol terkait.

Karena payload kontrol berbentuk biner dan memiliki format khusus, native app tidak boleh mengirim koordinat atau perintah JSON buatan sendiri. Implementasi Android harus mengikuti struktur pesan kontrol yang didefinisikan pada source ws-scrcpy.

## 4. Urutan implementasi native

1. Pertahankan koneksi tracker yang sudah ada.
2. Tambahkan pemilihan perangkat berdasarkan `udid`.
3. Ambil parameter stream sesuai mekanisme server yang sudah ada.
4. Implementasikan pembacaan metadata awal dan validasi panjang paket.
5. Tambahkan decoder video Android melalui `MediaCodec`.
6. Implementasikan parsing dan pengiriman pesan input biner sesuai kelas protokol upstream.
7. Uji stream dan input pada satu perangkat terlebih dahulu.
8. Pastikan halaman Web UI dan koneksi ADB/scrcpy yang sudah ada tetap tidak berubah.

## 5. Status

- Audit format tracker JSON: selesai.
- Audit jalur stream biner dan metadata awal: selesai pada tingkat source.
- Decoder video native: belum diimplementasikan.
- Input sentuh native: belum diimplementasikan.
- Uji end-to-end pada perangkat: belum dilakukan.

## 6. Batas keselamatan

Jangan mengubah konfigurasi ADB, scrcpy, port-forward, layanan Web UI, atau proses server untuk pekerjaan integrasi ini. Semua pengujian awal harus menggunakan koneksi tambahan yang tidak memutus sesi yang sudah berjalan.

# Native streaming dan kontrol sentuh

## Implementasi awal

Aplikasi menambahkan `StreamActivity`, sebuah Activity Android native yang memakai `SurfaceView` dan `MediaCodec`. Tidak memakai WebView, tidak mengubah konfigurasi ws-scrcpy, dan tidak menjalankan perintah shell pada perangkat target.

Alur saat ini:

1. Kartu perangkat aktif menampilkan tombol **Lihat layar & kontrol**.
2. Aplikasi memprioritaskan interface Wi-Fi perangkat (sesuai `wifi.interface`), lalu interface jaringan lain yang layak. Jika koneksi langsung gagal, aplikasi mencoba URL proxy ADB ke endpoint Debian yang sudah dikonfigurasi.
3. Client membuka WebSocket ke endpoint scrcpy pada port 8886.
4. Paket metadata `scrcpy_initial` dibaca untuk mendapatkan ukuran video.
5. NAL unit H.264 Annex-B dipisahkan. SPS/PPS dipakai untuk mengonfigurasi Android `MediaCodec`.
6. NAL unit video dikirim ke decoder dan dirender ke `SurfaceView`.
7. Event sentuh diterjemahkan ke pesan biner scrcpy `TYPE_TOUCH` (type 2), bukan JSON.

## Format kontrol sentuh

Pesan mengikuti urutan wire format yang diaudit dari `TouchControlMessage.toBuffer()`:

| Field | Ukuran |
|---|---:|
| type | 1 byte |
| action | 1 byte |
| pointer ID | 8 byte |
| x, y | 4 + 4 byte |
| screen width, height | 2 + 2 byte |
| pressure | 2 byte |
| buttons | 4 byte |

Action memakai nilai Android MotionEvent: down=0, up=1, move=2. Koordinat dipetakan dari area tampilan ke ukuran video, dengan kompensasi letterbox.

## Uji manual pada ponsel controller

1. Pasang APK debug versi 0.2.0.
2. Buka aplikasi dan hubungkan ke IP Debian pada port 8000.
3. Pastikan perangkat target muncul dengan status ONLINE.
4. Tekan **Lihat layar & kontrol** pada perangkat target.
5. Pastikan layar target tampil dan bergerak.
6. Uji tap dan swipe sederhana, lalu lepaskan sentuhan di area mana pun.
7. Jika gagal, catat teks status yang tampil. Jangan restart ws-scrcpy atau mengubah port-forward sebagai langkah pertama.

## Batas dan status verifikasi

- [x] Source Activity native dibuat.
- [x] Tombol streaming ditambahkan pada kartu perangkat aktif.
- [x] Jalur WebSocket, parsing metadata awal, pemisahan NAL, MediaCodec, dan format pesan sentuh dibuat.
- [x] APK debug berhasil dikompilasi dan tanda tangannya diverifikasi.
- [ ] Uji koneksi WebSocket stream pada perangkat nyata.
- [ ] Verifikasi decoder menampilkan gambar bergerak.
- [ ] Verifikasi tap, swipe, dan pelepasan sentuh pada perangkat target.
- [ ] Uji beberapa resolusi, orientasi, Android API, dan reconnect.
- [ ] Uji regresi Web UI browser, ADB, scrcpy, port-forward, dan LAN.

**Penting:** build yang berhasil bukan bukti stream dan remote touch sudah bekerja. Jangan menganggap fitur selesai sebelum seluruh pengujian perangkat nyata lolos.

## Catatan teknis

Backend yang diaudit memakai ws-scrcpy `1.19-ws8`, server Web UI pada port 8000, dan server scrcpy pada port 8886. Untuk interface langsung, client memakai alamat IPv4 interface dan port 8886. Jalur fallback memakai WebSocket proxy ADB pada host/port Debian yang dimasukkan pengguna.

Koneksi saat ini memakai `ws://` dan tidak memiliki autentikasi pada konfigurasi backend yang diaudit. Gunakan hanya pada LAN tepercaya. Jangan membuka port 8886 atau ADB ke internet.

## Perbaikan parser metadata Xiaomi (build 0.2.1)

Parser metadata `scrcpy_initial` membaca ukuran dasar dari record `DisplayInfo` (24 byte) dan memakai `ScreenInfo.videoSize` (25 byte) sebagai prioritas ketika tersedia. Sebelumnya parser hanya mengisi dimensi dari `ScreenInfo`; ketika blok tersebut kosong, aplikasi menghasilkan error `ukuran layar tidak valid` walaupun ukuran display tersedia. Parser juga memvalidasi jumlah display, panjang blok metadata, dan batas buffer sebelum membaca data.

APK debug 0.2.1 berhasil dikompilasi dan tanda tangannya diverifikasi. SHA-256: `816c8641568b03be387d511e0b0aaf7dd7f5b0fa23e3d991bd41fe8b859ad712`. Tampilan video dan kontrol sentuh tetap menunggu uji langsung pada ponsel.

## Diagnostik layar hitam (build 0.2.2)

Build 0.2.2 menambahkan log `aDeviceStream` untuk mendeteksi penerimaan SPS/PPS, paket IDR, error decoder, dan jumlah frame hasil decoding. Decoder hanya mengantrekan NAL unit gambar (type 1 dan 5), serta menandai IDR sebagai key frame. Saat frame pertama berhasil didekode, status di layar berubah menjadi `Gambar diterima` dan menampilkan jumlah frame. Gunakan `adb logcat -s aDeviceStream` untuk membaca diagnostik.

APK debug 0.2.2: SHA-256 `8bc3d33a5250ac650cb36b3d0155c742645c81fb40f5ea6fd41ca6c55104864d`. Hasil decode/stream tetap perlu dikonfirmasi pada perangkat fisik.

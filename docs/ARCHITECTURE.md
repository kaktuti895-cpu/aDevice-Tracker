# Arsitektur aDevice Tracker

## Tujuan
Aplikasi Android native menjadi frontend untuk memantau dan mengendalikan perangkat Android yang terhubung ke Debian. Web UI yang sudah ada tetap menjadi frontend alternatif.

## Diagram
```text
                 LAN
                  |
       +----------+-----------+
       |                      |
 Android Controller      Browser Client
 Native aDevice Tracker  Existing Web UI
       |                      |
       | WebSocket tracker    | HTTP/WebSocket
       +-----------+----------+
                   |
              Debian Server
               ws-scrcpy :8000
                   |
             Device tracker
                   |
        Android devices / ADB
             scrcpy server :8886
```

## Android client
- Package: `com.astuti.adevicetracker`
- Bahasa: Java.
- Minimum Android: API 23.
- Target Android: API 35.
- Permission jaringan: `android.permission.INTERNET`.
- UI native Android, tanpa WebView.
- Endpoint Debian disimpan di SharedPreferences.
- Tracker memakai WebSocket dan membaca pesan JSON `devicelist` serta `device`.
- `StreamActivity` memakai `SurfaceView`, WebSocket biner, dan Android `MediaCodec` untuk jalur H.264.
- Kontrol sentuh memakai struktur biner `TouchControlMessage` scrcpy, bukan JSON.

## Backend
- Memakai ws-scrcpy yang sudah terpasang di Debian.
- Web UI dan tracker: TCP 8000.
- Endpoint tracker: `/?action=goog-device-list`.
- Scrcpy server pada perangkat Android: TCP 8886.
- Aplikasi tidak membuat backend tracker baru dan tidak mengubah konfigurasi layanan.

## Jalur streaming
1. Pengguna memilih perangkat aktif.
2. Aplikasi mengambil alamat IPv4 interface dari metadata tracker.
3. Client membuka WebSocket scrcpy ke interface perangkat pada port 8886. Jika interface tidak tersedia, aplikasi memakai jalur proxy ADB melalui host/port Debian.
4. Metadata awal `scrcpy_initial` dibaca untuk ukuran video.
5. NAL H.264 Annex-B dipisah; SPS/PPS digunakan untuk menyiapkan decoder MediaCodec.
6. Video dirender ke SurfaceView.
7. Tap, swipe, dan move dikirim sebagai pesan `TYPE_TOUCH` biner.

## Status verifikasi
Source streaming dan touch sudah dibuat serta APK berhasil dikompilasi dan diverifikasi signing-nya. Koneksi stream, decoding gambar bergerak, dan input sentuh belum dinyatakan berhasil sebelum uji pada perangkat Android nyata.

## Browser
Web UI tetap tersedia pada `http://192.168.2.233:8000/` dan tidak diganti oleh aplikasi native.

## Keamanan
Koneksi yang diaudit menggunakan `ws://` tanpa autentikasi pada konfigurasi backend saat ini. Gunakan pada jaringan tepercaya. Sebelum akses lintas jaringan atau internet, rancang autentikasi dan TLS. Jangan membuka port ADB atau 8886 ke internet.
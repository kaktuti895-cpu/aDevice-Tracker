# Arsitektur aDevice Tracker

## Sasaran
aDevice Tracker adalah aplikasi controller Android native untuk mengakses perangkat Android melalui backend Debian yang sudah berjalan. Aplikasi bukan halaman web dan tidak menggunakan WebView sebagai antarmuka utama.

## Komponen
1. **Controller Android**: konfigurasi server, daftar perangkat, tampilan layar, dan kontrol.
2. **Debian backend**: layanan ws-scrcpy yang juga melayani Web UI.
3. **ADB**: transport perangkat Android yang terhubung ke Debian.
4. **Web UI**: jalur browser yang tetap dipertahankan.

## Jalur komunikasi
- Discovery: gunakan mekanisme daftar perangkat yang sudah tersedia pada backend.
- Streaming: ikuti protokol WebSocket/multiplexer ws-scrcpy sesuai versi backend terpasang, termasuk framing biner dan negosiasi sesi.
- Input: kirim perintah kontrol sesuai protokol backend yang terverifikasi.

## Prinsip integrasi
- Gunakan backend yang ada, bukan membuat server pengganti.
- Jangan mengubah port atau konfigurasi Web UI.
- Pisahkan lifecycle daftar perangkat dan sesi streaming.
- Tangani disconnect, pergantian perangkat, rotasi, dan ukuran layar.
- Validasi koordinat input terhadap resolusi perangkat.

## Batas saat ini
Koneksi backend dan daftar perangkat merupakan fondasi awal. Protokol streaming dan kontrol native masih harus diimplementasikan dan diuji terhadap versi ws-scrcpy di server.

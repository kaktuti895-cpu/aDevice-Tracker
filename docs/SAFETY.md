# Batas Keselamatan Operasional

Layanan yang wajib dipertahankan:
- Web UI port 8000.
- ws-scrcpy.
- ADB.
- scrcpy.
- Port-forward 8001 dan 8002.
- Koneksi LAN.

## Larangan tanpa persetujuan
- Jangan menghentikan atau me-restart layanan aktif.
- Jangan menghapus atau mengganti konfigurasi ADB/forward.
- Jangan mengubah port Web UI.
- Jangan menghapus atau memindahkan source proyek asli.
- Jangan mengunggah kredensial, token, private key, atau konfigurasi rahasia ke GitHub.

## Cara kerja aman
1. Audit read-only terlebih dahulu.
2. Kerjakan perubahan pada source aplikasi secara terpisah.
3. Build APK tanpa mengubah backend.
4. Uji fitur baru pada sesi terpisah.
5. Periksa layanan dan port sebelum/sesudah pengujian.
6. Hentikan pengujian jika ada indikasi gangguan pada browser atau ADB.

Jika terjadi masalah, tutup aplikasi controller dan kumpulkan log. Jangan restart layanan server secara otomatis.

# aDevice Tracker

Aplikasi Android native untuk mengakses perangkat Android melalui backend Debian/ws-scrcpy yang sudah ada.

**Status:** fondasi awal tersedia; streaming layar native dan kontrol sentuh masih dalam pengembangan.

## Tujuan
- Controller Android native, bukan WebView.
- Menggunakan backend Debian yang sudah berjalan.
- Menampilkan dan memilih perangkat Android.
- Mengembangkan streaming layar dan kontrol sentuh.
- Mempertahankan Web UI browser sebagai jalur akses alternatif.

## Arsitektur
Android Controller (native) → Debian backend/ws-scrcpy → perangkat Android melalui ADB.

Web UI tetap berjalan berdampingan dan tidak digantikan aplikasi native.

## Dokumentasi
- [Arsitektur](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Tutorial build dan instalasi](docs/BUILD.md)
- [Keselamatan operasional](docs/SAFETY.md)
- [Riwayat pengembangan](docs/CHANGELOG.md)

## Status fitur
| Fitur | Status |
|---|---|
| Aplikasi native dan launcher | Fondasi tersedia |
| Koneksi backend dan daftar perangkat | Fondasi tersedia |
| Simpan endpoint terakhir | Ditambahkan |
| Streaming layar native | Belum selesai |
| Kontrol sentuh native | Belum selesai |

## Layanan yang wajib dipertahankan
Web UI port 8000, ws-scrcpy, ADB, scrcpy, port-forward 8001/8002, dan koneksi LAN. Jangan mengubah atau menghentikan layanan tersebut saat mengembangkan aplikasi tanpa persetujuan dan rencana uji terpisah.

# LunaBeat

[简体中文](https://github.com/2755337087/LunaBeat/blob/main/README.md) | [繁體中文](https://github.com/2755337087/LunaBeat/blob/main/README_ZH_TW.md) | [English](https://github.com/2755337087/LunaBeat/blob/main/README_EN.md) | [日本語](https://github.com/2755337087/LunaBeat/blob/main/README_JA.md) | [Türkçe](https://github.com/2755337087/LunaBeat/blob/main/README_TR.md) | [한국어](https://github.com/2755337087/LunaBeat/blob/main/README_KO.md) | [Русский](https://github.com/2755337087/LunaBeat/blob/main/README_RU.md) | [Bahasa Indonesia](https://github.com/2755337087/LunaBeat/blob/main/README_ID.md) | [Tiếng Việt](https://github.com/2755337087/LunaBeat/blob/main/README_VI.md) | [ไทย](https://github.com/2755337087/LunaBeat/blob/main/README_TH.md) | [Español](https://github.com/2755337087/LunaBeat/blob/main/README_ES.md) | [हिन्दी](https://github.com/2755337087/LunaBeat/blob/main/README_HI.md) | [Português](https://github.com/2755337087/LunaBeat/blob/main/README_PT.md) | [Français](https://github.com/2755337087/LunaBeat/blob/main/README_FR.md) | [Deutsch](https://github.com/2755337087/LunaBeat/blob/main/README_DE.md) | [العربية](https://github.com/2755337087/LunaBeat/blob/main/README_AR.md)

- LunaBeat ingin menghadirkan pengalaman musik lokal yang lebih bersih dan imersif, seperti musik di malam hari, di antara melodi dan lirik.
---
- LunaBeat adalah pemutar mobile yang berfokus pada musik lokal dan pengalaman lirik, menggabungkan pemutaran musik, pengeditan lirik, dan timing lirik per kata.
- Mendukung pemutaran audio lokal, pembuatan lirik per kata termasuk duet dan suara latar, pratinjau dan pengeditan lirik, ekspor ke `.lrc` / `.elrc` / `.ttml`, serta pemrosesan batch untuk metadata audio dan lirik.
- Dari impor, pengeditan, pemutaran hingga ekspor, LunaBeat bertujuan menghadirkan pengalaman musik dan lirik lokal yang lebih lengkap dan imersif.

## Mengapa LunaBeat

- Bukan hanya editor lirik, tetapi juga dapat digunakan sebagai pemutar musik lokal jangka panjang.
- Membuat lirik bertiming per kata tingkat profesional langsung di perangkat mobile.
- Mendukung impor dan ekspor banyak format untuk alur kerja lirik umum.
- Mengintegrasikan pustaka musik dan alat metadata untuk mengurangi perpindahan aplikasi.
- Fitur batch bawaan cocok untuk merapikan koleksi lagu besar.

## Pratinjau Antarmuka

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/mainPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/searchPage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricEditPage.jpg" width="30%" />
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricPrePage.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/lyricPrePage2.jpg" width="30%" />
  <img src="https://raw.githubusercontent.com/2755337087/LunaBeat/main/Screenshots/musicEditPage.jpg" width="30%" />
</p>

## Ringkasan Fitur

### 1. Timing dan Pengeditan Lirik
- Alur timing tiga tombol: Mulai / Lanjut / Akhiri
- Mundur / maju cepat, putar / jeda, kecepatan 0.25x ~ 3x, dan lompat ke waktu tertentu
- Urungkan / ulangi untuk waktu, lirik, dan operasi baris
- Ketuk waktu pemutaran untuk masuk mode mengikuti dan mencari lirik yang dipilih
- Ketuk unit lirik untuk memilih, ketuk dua kali untuk lompat ke waktu mulai lirik saat ini dan memutar

### 2. Impor
- Lirik teks biasa
- LRC (per baris / per kata)
- Enhanced LRC (ELRC)
- TTML
- Pencarian lirik streaming
- Impor file audio

### 3. Ekspor
- LRC (per baris / per kata)
- TTML (mendukung duet dan suara latar)

### 4. Pratinjau dan Pemrosesan Batch
- Pratinjau efek pemutaran lirik per kata
- Segmentasi kata sekali klik
- Pergeseran timestamp
- Penggabungan lirik sekali klik
- Konversi Tionghoa Tradisional ke Sederhana
- Hapus baris kosong sekali klik

### 5. Operasi Struktur Lirik (Tekan Lama)
- Operasi lirik: edit, tambah, pisah, gabung, atur waktu, hapus
- Operasi baris: tambah, gabung, pisah, pindah, tambah/edit terjemahan, hapus

### 6. Pemutar
- Pemutaran musik lokal dengan putar / jeda, sebelumnya / berikutnya, dan geser progres
- Putar berurutan, acak, dan ulang satu lagu
- Manajemen antrean sekarang diputar, mendukung lompat, hapus, dan urutkan dengan drag
- Mini player pustaka musik terhubung dengan pemutar penuh
- Pemutaran latar belakang, notifikasi media sistem, dan kontrol tombol media
- Timer tidur, mendukung berhenti dengan hitung mundur atau setelah lagu saat ini selesai
- Terintegrasi dengan pratinjau lirik untuk melihat efek lirik per kata saat memutar
- Mendukung format audio lokal umum dan menangani kompatibilitas pemutaran ALAC

### 7. Pustaka Musik
- Memindai dan memfilter audio lokal
- Playlist, lagu favorit, dan lagu bintang lima
- Impor / ekspor playlist
- Tambahkan ke antrean / putar berikutnya
- Pencocokan tag batch (cover, judul, artis, dan lainnya)
- Pencocokan lirik batch (per kata / per baris / terjemahan)
- Ganti nama batch
- Edit tag batch

### 8. Pengaturan
- Mode gelap kustom
- Durasi maju / mundur kustom untuk timing lirik
- Pengaturan pustaka musik
- Gaya dan latar bilah pemutaran
- Tampilan lirik, lirik desktop, lirik status bar, dan lirik Bluetooth mobil
- Pengaturan ukuran cover metadata audio
- Pengaturan wilayah metadata Apple Music

## Mulai Cepat 5 Menit (Urutan Disarankan)

### Langkah 1: Impor Audio dan Lirik
1. Impor file audio terlebih dahulu.
2. Pilih metode impor sesuai sumber lirik: teks biasa / LRC / ELRC / TTML / pencarian.

### Langkah 2: Putar Musik Lokal
1. Masuk ke pustaka musik dan pindai audio lokal.
2. Ketuk lagu untuk mulai memutar; mini player di bawah menampilkan status pemutaran saat ini.
3. Buka pemutar penuh untuk mengganti mode pemutaran, mengelola antrean, membuka pratinjau lirik, atau mengatur timer tidur.

### Langkah 3: Praproses Lirik
1. Gunakan segmentasi kata sekali klik untuk membuat unit per kata.
2. Jika perlu, gunakan hapus baris kosong, gabung lirik, atau konversi Tradisional ke Sederhana.
3. Jika timestamp yang ada bergeser secara keseluruhan, gunakan pergeseran timestamp sebelum penyempurnaan.

### Langkah 4: Mulai Timing (Tiga Tombol Inti)
- Mulai: mengatur waktu mulai unit saat ini tanpa berpindah.
- Lanjut: mengatur waktu akhir unit saat ini, otomatis ke unit berikutnya, dan menulis waktu mulai unit berikutnya.
- Akhiri: mengatur waktu akhir unit saat ini dan berpindah ke unit berikutnya, tetapi tidak menulis waktu mulai unit berikutnya.

Ritme yang disarankan: `Mulai -> Lanjut -> Lanjut -> ... -> Akhiri`, diulang per kalimat.

### Langkah 5: Sempurnakan Waktu dan Teks
1. Aktifkan mode mengikuti agar pemutaran otomatis menemukan unit saat ini.
2. Ketuk dua kali lirik untuk cepat kembali ke waktu mulai dan mendengarkan ulang.
3. Gunakan urungkan / ulangi untuk membandingkan perubahan sebelum dan sesudah.
4. Tekan lama lirik atau baris untuk memisah, menggabung, memindah, menerjemahkan, atau memperbaiki struktur.

### Langkah 6: Atur Duet dan Terjemahan (Opsional)
- Jenis duet: duet kiri (vokal utama), duet kanan (vokal kedua), latar (suara latar).
- Terjemahan dapat diimpor atau diedit manual.
- Catatan: jenis duet hanya berlaku dalam alur kerja TTML.

### Langkah 7: Pratinjau dan Ekspor
1. Periksa efek pemutaran per kata di halaman pratinjau lirik.
2. Ekspor sesuai kebutuhan:
   - Gunakan `LRC` untuk pemutar umum
   - Gunakan `TTML` jika memerlukan informasi duet / suara latar

## Antarmuka Timing

- Kolom: waktu mulai / lirik / waktu akhir / terjemahan
- Lokasi: dapat menuju lirik yang dipilih
- Lompat waktu: ketuk waktu di area timing untuk lompat ke waktu kustom
- Ketuk judul: dapat memperoleh path audio

## Alur Kerja Pustaka Musik yang Disarankan

1. Pindai audio lokal, lalu cek dan filter lagu target.
2. Rapikan lagu yang sering didengar berdasarkan album, playlist, favorit, atau bintang lima.
3. Putar lagu dan kelola antrean saat ini; jika perlu, atur putar berikutnya atau timer tidur.
4. Cocokkan tag secara batch untuk melengkapi cover / judul / artis dan metadata lainnya.
5. Cocokkan lirik secara batch untuk melengkapi lirik per kata / per baris dan terjemahan.
6. Selesaikan dengan ganti nama batch dan edit tag batch.

## FAQ

### 1. Kapan mengekspor LRC, dan kapan TTML?
- Pilih LRC jika hanya perlu tampilan lirik biasa.
- Pilih TTML jika memerlukan duet, suara latar, atau struktur yang lebih lengkap.

### 2. Mengapa efek duet tidak terlihat setelah saya mengatur duet?
Jenis duet hanya didukung di TTML. Pastikan Anda memakai alur kerja TTML dan mengekspor sebagai TTML.

### 3. Lirik seluruhnya terlalu cepat atau terlambat. Bagaimana memperbaikinya dengan cepat?
Gunakan pergeseran timestamp untuk kalibrasi global terlebih dahulu, lalu kembali ke halaman timing untuk penyempurnaan lokal.

### 4. Kesalahan apa yang paling sering dilakukan pemula?
Biasanya melakukan timing sebelum segmentasi kata. Sebaiknya selesaikan segmentasi kata dan pembersihan teks sebelum mulai timing.

## Special Thanks

Inspired by:
- [amll-ttml-tool](https://github.com/amll-dev/amll-ttml-tool)
- [Music Tag](https://www.cnblogs.com/vinlxc/p/11932130.html)

Technical support:

- [TagLib](https://github.com/taglib/taglib)
- [Lyricon](https://github.com/tomakino/lyricon)
- [SongSync](https://github.com/Lambada10/SongSync)
- [LDDC](https://github.com/chenmozhijin/LDDC)
- [OpenCC](https://github.com/BYVoid/OpenCC)
- [any-listen-extension-online-metadata](https://github.com/any-listen/any-listen-extension-online-metadata)
- [163Music2Tag](https://gitee.com/Wangs-official/163Music2Tag)
- [TagLib (Kyant0)](https://github.com/Kyant0/taglib)
- [uCrop](https://github.com/Yalantis/uCrop)
- [accompanist-lyrics-ui](https://github.com/6xingyv/accompanist-lyrics-ui)
- [EdgeTranslucent](https://github.com/qinci/EdgeTranslucent)
- [lottie-android](https://github.com/airbnb/lottie-android)
- [ICU Transliterator](https://unicode-org.github.io/icu/userguide/transforms/general/)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)

## Komunitas dan Masukan

- [Gabung grup Telegram LunaBeat](https://t.me/+qXs6mjKqwhw3Zjll)
- [Gabung grup QQ LunaBeat](https://qm.qq.com/q/N0fBvuWKOY)  ID grup: 964680520

# ab — Tasarım belgesi

**ab** Farsça "su" demek. Offline çalışan, takipsiz, sade bir Android su takibi uygulaması. Bu belge kullanıcıyla yapılan konuşmanın özetidir. Yeni karar çıktıkça güncellenir.

## Amaç
Çoğu uygulama "saatte bir" gibi sabit ve bunaltıcı bildirim gönderir. ab, günün belirlenen saat aralığı ve hedef su miktarından kaç dakikada bir ne kadar içilmesi gerektiğini hesaplar. Su içildikçe hesap yenilenir. Kullanıcı bildirimden bıkıp uygulamayı bırakmamalıdır.

## Olmazsa olmazlar (öncelik sırasıyla)
1. **Akıllı bildirim/hatırlatma**
   - Aralık, kalan süre ve kalan suyun tam dakikaya hesabıdır (ör. 87 dk). Alt sınır (ör. 45 dk) ve yuvarlak saat yoktur.
   - Bildirimler yalnızca kullanıcının rutin penceresinde (ör. 06:00–18:00, her gün aynı, ayardan değişir) gelir.
   - Geride kalınca bildirim sıklaşmaz, plan aynı kalır (şimdilik).
   - Son içişten sonra sıradaki bildirim ertelenir; hedef dolunca o gün bildirim kesilir; dil sıcak ve suçlamasızdır (benim kararım).
2. **Hızlı kayıt:** yalnızca bildirimden ve ana ekran widget'ından, uygulama açılmadan.
   - Varsayılan miktar 200 ml (değiştirilebilir). Hazır değerler 250 / 500 / 750 / 1000 ml. Özel miktar anında girilebilir.
   - Widget ve bildirim yalnızca ilerlemeyi gösterir ("1.600 / 2.750 ml").

## Günlük hedef
`hedef = max(1500, 25 ml × kilo) + hareket eki + sıcaklık eki`

| Hareket (kullanıcı seçer) | Ek |
|---|---|
| Hareketsiz | 0 |
| Orta (~30 dk/gün) | +250 ml |
| Çok aktif (60+ dk/gün) | +500 ml |

| Günün tahmini en yüksek sıcaklığı | Ek |
|---|---|
| ≤ 20°C | 0 |
| 21–25°C | +250 ml |
| 26–30°C | +500 ml |
| 31–35°C | +750 ml |
| > 35°C | +1000 ml |

- Soğukta hedef düşürülmez. Hedef her zaman elle düzenlenebilir. Hareket ve sıcaklık ekleri ayardan kapatılabilir.
- Rakamlar kaynaklı bir tablodan değil, tahmindir (EFSA 2,0/2,5 L toplam su; IOM 2,7/3,7 L toplam su; 30–35 ml/kg resmî kılavuz değil). Uygulamada "tahminidir, tıbbi tavsiye değildir" notu bulunur. Hedef 5 L üstüne çıkarsa uyarı verilir.
- Sıcaklık günde yalnızca bir kez, sabah çekilir. Hedef değişirse ekran kapalıyken bildirimle haber verilir.
- Hava: yalnızca sıcaklık, internetten, hesapsız ve anahtarsız bir servisten (Open-Meteo önerisi, benim seçimim). Konum izni isteğe bağlıdır (yaklaşık konum, Play Services yok). İzin yoksa şehir elle seçilir. Servise şehir düzeyinde yuvarlanmış koordinat gider.

## Ana ekran
1. Üstte bugünkü plan kartı: bir sonraki bildirim saatine kadar içilecek miktar (ör. "10:45'e kadar 600 ml").
2. Altında ilerleme çubuğu.
3. Altında hızlı ekleme düğmeleri (görünüm ortaya çıktıkça değişebilir).
Plan ayrıntısı yalnızca uygulamanın içinde görünür.

## Yapılmayacaklar
Reklam, analytics, Firebase, Google Play Services, hesap/giriş, sabit saatli spam bildirim, hedef dolduktan sonra bildirim, suçlayıcı pop-up, hatırlatma dışı amaçla pil muafiyeti isteme.

## Teknik (benim seçimlerim)
Kotlin + Jetpack Compose + Material 3, minSdk 26, compile/target SDK 34 (sunucu SDK'sı), AGP 8.7, Gradle 8.11.1. Faz 1'de Room, DataStore, Hilt, Glance (widget). Lisans GPL-3.0-or-later. Repo: lyldz/ab (public). Paket: com.lyldz.ab. Diller: tr, en.

## Sonraya kalanlar
Health Connect, Wear OS, rozet/seri, içecek türleri, Ramazan modu, Hızlı Ayarlar kutucuğu. Bunlar bu konuşmada istenmedi.

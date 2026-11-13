# Kararlar

| Tarih | Karar | Neden |
|---|---|---|
| 2026-11-12 | Uygulama adı `ab`, paket `com.lyldz.ab` | Kullanıcı seçti (Farsça "su") |
| 2026-11-12 | compile/target SDK 34, AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1 | Sunucudaki SDK yalnızca android-34. targetSdk 36'ya Play öncesi geçilir |
| 2026-11-12 | Faz 0'da Hilt, Room, DataStore, ktlint, detekt yok | Faz 0 boş iskelet; 2 GB RAM'li sunucuda derleme maliyeti. Faz 1'de eklenecek |
| 2026-11-12 | Hatırlatma aralığında alt sınır yok | Kullanıcı sabit 45 dk istemiyor |
| 2026-11-12 | Hedef = max(1500, 25 ml/kg) + hareket + sıcaklık | 33 ml/kg yüksek bulundu; EFSA/IOM değerleri ve 30–35 ml/kg kaba kural incelendi |
| 2026-11-12 | Widget Faz 1'de | Kullanıcı için olmazsa olmaz |
| 2026-11-12 | Hava servisi Open-Meteo (öneri) | Hesapsız, anahtarsız. Kullanıcı yalnızca "sadece sıcaklık çekilsin" dedi |
| 2026-11-12 | Hilt kullanılmıyor; bağımlılıklar Application üzerinden elle | Uygulama küçük; KSP/Hilt derleme maliyeti ve karmaşıklık. Room için KSP yine gerekir |
| 2026-11-12 | Hatırlatmada her blokta içilecek miktar bardak boyutudur (varsayılan 200 ml), aralık buna göre hesaplanır | "Bir sonraki bildirime kadar olan miktar" + "aralığı dakikaya böl" birlikte bunu verir; SPEC'teki "10:45'e kadar 600 ml" örneği daha büyük blok ima eder, kullanıcıya soruldu |
| 2026-11-12 | Sürümler 0.1.x serisiyle ilerler (şimdiki 0.1.0, versionCode 100) | Kullanıcı önce 0.0.x dedi, sonra 0.1.x olsun dedi. fastlane changelog dosyası 100.txt |
| 2026-11-12 | Bildirimde 3 düğme: bardak, 500 (bardak 500 ise 1000), satır içi "Özel" | Android bir bildirimde en fazla 3 düğme gösterir; 250/750/1000 uygulamada ve widget'ta. Kullanıcıya soruldu |
| 2026-11-12 | Bildirimden/uygulamadan/widget'tan kayıt sonrası bildirim kapatılmaz, yerinde güncellenir | Kullanıcı telefonda denedi: eklenince kayboluyordu, kalması gerekiyordu |
| 2026-11-12 | Tema ikon paletine (lacivert/mavi) çevrildi; Room şeması v2 (kayıtlara kaynak sütunu, migration ile) | Kullanıcı krem/teal temayı beğenmedi; kayıt listesi kaynak ikonu istendi |
| 2026-11-12 | Hava: Open-Meteo (tahmin API + geocoding), günde bir kez; sabah alarmı ayrı (rutin başı), bildirim yalnızca hava eki >0 ve elle hedef yokken | Kullanıcı: günde bir kez, internetten yalnızca sıcaklık, konum isteğe bağlı. Koordinat 1 ondalığa yuvarlanır (~11 km) |
| 2026-11-12 | Alt gezinme 3 sekme (Ana sayfa, İstatistik, Ayarlar); Room v3: day_goal tablosu | Referanstaki "Hatırlatıcı" sekmesi ayrı değil, hatırlatma ayarları Ayarlar'da. İstatistikte geçmiş hedef için günlük anlık görüntü gerekir |
| 2026-11-12 | Tempo = rutin penceresinde düz çizgi (başta 0, sonda hedef); sürahi kapasitesi = günlük hedef | Kullanıcı: ne kadar geride olduğunu ve şu saate kadar ne kadar içmesi gerektiğini görmek istedi; doluluk kişiden kişiye değişsin |
| 2026-11-12 | Bildirim başlığı tempo durumudur (geride/ilerde/tempoda), gövde "bardak kadar iç · içilen / hedef" | Kullanıcı: bildirim de "geridesin" desin; bildirim sıklığı ve miktarı değişmedi |
| 2026-11-12 | Alarm inexact (setAndAllowWhileIdle); tek alarm tutulur | SPEC T-03: izin gerektirmez; Doze'da dakikalar kayabilir, tam zamanında seçeneği sonra |
| 2026-11-12 | Görmezden gelinen hatırlatmadan sonra bir sonraki aralık sonra yine gelir | Plan çıpası son içiş veya son gösterilen hatırlatma; bıkma durumunda sıklık azaltma henüz kararlaştırılmadı, kullanıcıya soruldu |
| 2026-11-12 | Marka görseli | Ãb yazı logosu (Cormorant Garamond SemiBold, OFL, yoldan vektöre çevrildi) + damla; palet Aqua/Deep Navy/Pearl; ana ekran dairesel su göstergesi (WaterOrb, sürahi kodu durur). Referans: docs/design/brand/branding-reference.jpg |
| 2026-11-13 | Hatırlatma aralığı sabit, miktar kalan toplama göre | Kullanıcı: geride kalınca bildirim sıklaşmasın; aralık elle müdahale olmadıkça değişmesin; her hatırlatmada 200/250/400 ml gibi, bardağa değil kalan toplama göre (örn. 1900 ml kaldı). Aralık = pencere × bardak ÷ plan hedefi (hava eki yok), miktar = kalan ml ÷ kalan hatırlatma sayısı, 50 ml adım, 100–500 ml. Eski formül (kalan süreye göre aralık) geride kalınca 46 dk'yı 19 dk'ya, akşam birkaç dakikaya indiriyordu |
| 2026-11-13 | Yedek: tek JSON dosyası, sunucu/giriş yok | Kullanıcı Google ile giriş ve sunucu sordu; çevrimdışı/hesapsız vaadi ve Play Services yokluğu nedeniyle dosya tabanlı yedek seçildi. Biçim `ab-backup` v1 (settings, intakes, goals); geçici alanlar yazılmaz; okurken alan alan doğrulanır. Geri yükleme birleştirir (aynı an+miktar atlanır), ayarlar değişir. Otomatik Drive yedeği ayrı, isteğe bağlı iş (Play Services gerekir) |
| 2026-11-13 | Pencere = kalkış → yatış − 2 saat | Kullanıcı: kimse akşam 6'da uyumaz, uykudan en az 2 saat önce su içmeyi bırakmak gerekir; kalkış 06:30, yatış 22:00 ise hesap 20:00'ye kadar otomatik çalışmalı, altına not düşülsün. Saklanan alanlar aynı (windowStartMin = kalkış, windowEndMin = su içmenin sonu = yatış − 120); yatış yalnızca arayüzde gösterilir (veri geçişi yok). Varsayılan 06:30 / 20:00 |
| 2026-11-13 | Sürüm numarası: yeni özellik gelene kadar yalnızca son rakam; su temalı seri adları | Kullanıcı: sürüm 0.3.1 olsun, yeni özellik gelene kadar son rakam değişsin; 0.3.0 "Testi" (su kabı) olsun, 0.4.0 gelince su ile ilgili yeni bir ad. Adlar: 0.2 Damla, 0.3 Testi, 0.4 Pınar (`core/Version.kt` `seriesName`). Ayarlar > Hakkında ve GitHub sürüm başlığında gösterilir |

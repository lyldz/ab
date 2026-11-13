# Değişiklik günlüğü

[Keep a Changelog](https://keepachangelog.com/tr/1.1.0/) biçimi, [SemVer](https://semver.org/lang/tr/) sürümleme.

## [Unreleased]
### Added
- Yedek: Ayarlar > Yedek ile kayıtlar, günlük hedefler ve ayarlar tek JSON dosyasına yazılır ya da dosyadan geri yüklenir (dosya seçici, izin gerekmez, hiçbir yere gönderilmez). Geri yükleme kayıtları ekler (aynı an+miktar yeniden eklenmez), ayarları yedektekilerle değiştirir.
### Changed
- Gösterge büyüdü (su dairesi daha geniş, dalgalar belirgin). Su telefonun ivmeölçeriyle eğime göre yatay kalır ve sallayınca dalgalanır (Ayarlar > Görünüm'den kapatılır; izin gerekmez, yalnızca ekran açıkken dinler).
- Kurulum sihirbazı yenilendi (5 adım): karşılama, kilo ve hareket (hazır kilo düğmeleri, 3 hareket kartı), gün düzeni (kalkış/yatış saat seçici + bardak boyutu), izinler (bildirim, konum + bugünün sıcaklığı sonucu, şehir arama, pil notu), sana özel hedef özeti (kırılım ve hatırlatma sıklığı). Adımlar arası geçiş animasyonu, ilerleme noktaları.
- Ayarlar gruplandı: Profil, Günlük hedef, Gün düzeni, Hava ve konum, Görünüm, İzinler ve bildirimler (izin durumu + sistem ayarına kısayol + pil rehberi), Yedek, Güncelleme, Hakkında.
- Gün düzeni: "Kalkış" ve "Yatış" saati girilir, su içme/hatırlatma penceresi otomatik kalkış → yatıştan 2 saat önce olur (örn. 06:30 / 22:00 → 06:30–20:00); altında not gösterilir. Yeni kurulum varsayılanı 06:30 / 22:00 (eskiden 06:00–18:00 su içme). Mevcut kullanıcıların hatırlatma pencerelerinin sonu değişmez, yatış saati ona göre (sonu + 2 saat) gösterilir.
### Changed
- Hatırlatma aralığı sabit: geride kalınca bildirim sıklaşmaz (eskiden 46 dk'dan 19 dk'ya, akşama doğru birkaç dakikaya inebiliyordu). Aralık yalnızca kilo, hareket, rutin, bardak ya da elle hedef değişince değişir; her hatırlatmada içilecek miktar kalan toplama göre hesaplanır (50 ml adım, 100–500 ml).

## [0.3.0] - 2026-11-12
### Added
- Uygulama içi güncelleyici: Ayarlar > Güncelleme. Yalnızca dokununca GitHub sürümlerine bakar, yeni sürüm notlarını gösterir, APK'yı indirip sistem yükleyicisini açar. "Bilinmeyen kaynaklardan yükleme" izni ayrı ekranda istenir.

## [0.2.0] - 2026-11-12
### Added
- Yeni tasarım sistemi (Aqua, Mint, Deep Navy, Pearl; Plus Jakarta Sans): dolu dairesel su göstergesi (çift katmanlı dalga), zaman çizelgesi kartı (son düğüm sıradaki hatırlatma), "+ Su Ekle" düğmesi ve Bardak/Kupa/Şişe kartları, 4 sekme (Bugün, Grafikler, Geçmiş, Ayarlar), yeni Geçmiş ekranı.
- Siyah-beyaz (OLED) koyu tema; Ayarlar'dan Sistem/Açık/Koyu seçilir.
- Büyük "Āb" logosu (üst çubuk ve karşılama), makronlu yeni uygulama ve bildirim ikonu.
- Kurulumda konum bulma ve hava: bulunan yer, bugünün sıcaklığı ve hedefe eklenen su gösterilir; konum yoksa şehir aranır.
- Durum bildirimi uygulama açılınca ve kurulum sonrası hemen görünür (sessiz, yerinde güncellenir).
- İsteğe bağlı isim (selamlama), seri bilgisi.
### Fixed
- Gösterge animasyonları sistem animasyon ölçeği kapalıyken de çalışır.

### Added (0.2.0 öncesi)
- Tempo bilgisi: "X ml geridesin / ilerdesin" ve sıradaki hatırlatma saatinde olman gereken toplam; bardak yerine sürahi (kapasite = kişinin günlük hedefi, ölçek çizgileri, "şu an olman gereken" kesik çizgisi). Bildirim başlığı da aynı tempo bilgisini söyler ("450 ml geridesin").
- Günlük hedef hesabı (25 ml/kg + hareket + sıcaklık) ve dakikaya hesaplanan hatırlatma planı, birim testleriyle.
- Veri katmanı: Room (içme kayıtları), DataStore (ayarlar) ve ortak AbRepository.
- Onboarding (kilo, hareket, rutin saatleri, bildirim izni) ve ana ekran (plan kartı, ilerleme çubuğu, hızlı ekleme, özel miktar, geri al).
- Hatırlatma alarmı ve bildirim: bardak, 500 ml ve satır içi özel miktar düğmeleri; yeniden başlatma ve saat değişiminde alarm yeniden kurulur.
- Logo ve ikon paketi (uygulama, temalı ve bildirim ikonu); onboarding'de bildirim izni ayrı adımda istenir.
- Ana ekran widget'ı (Glance): ilerleme, +bardak/+500/+1000 ve özel miktar için uygulamayı açan düğme.
- Mavi tema (ikon paletinden), "Bugünün kayıtları" listesi: kaynak (uygulama/bildirim/widget), düzenleme, silme ve silmeyi geri alma.
- Hava: günde bir kez (sabah rutin başında ve ilk açılışta) Open-Meteo'dan bugünün en yüksek sıcaklığı; şehir arama veya isteğe bağlı yaklaşık konum; hedef yükselirse bildirim. Onboarding'de ayrı konum izni adımı.
- Dolan bardak görseli (dalgalı su, yaylı animasyon), hedef/içilen/kalan kartları ve simgeli bardak seçme penceresi.
- Alt gezinme (Ana sayfa, İstatistik, Ayarlar) ve istatistik ekranı: 7/30 gün çubuk grafiği, ortalama, hedefe ulaşılan gün, en iyi gün. Room şeması v3 (günlük hedef anlık görüntüsü).
- Ayarlar ekranı: kilo, hareket, rutin saatleri, bardak boyutu, hareket/hava eklerini kapatma, elle hedef ve 5 L uyarısı.

## [0.1.0]
### Added
- Boş Compose iskeleti, marka paleti ve ikon, tr/en metinler.

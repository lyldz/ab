# Eksikler ve yapılacaklar

Son güncelleme: 2026-11-12. Durum: uygulama telefonda yalnızca kısmen denendi, çok şey eksik. Bu liste hiçbir şeyin unutulmaması için.

## 1. Telefonda doğrulanmadı (önce bunlar)
- [ ] Hatırlatma bildirimi hesaplanan dakikada geliyor mu (MIUI/Samsung pil ve otomatik başlatma kısıtları, Doze kayması)
- [ ] Bildirimden kayıt (bardak, 500, satır içi "Özel") ve kayıttan sonra bildirimin yerinde güncellenmesi
- [ ] Widget (Glance): görünüm, düğmeler, "Özel" ile uygulamanın açılması
- [ ] Hava: onboarding konum adımı, Ayarlar > Hava (şehir arama, konumumu kullan), sabah bildirimi
- [ ] Yeni ana ekran (dairesel gösterge, büyük "+" düğmesi, çipler, seri kartı), yeni ikonlar (ana ekran, temalı, bildirim 24 dp) ve tempo kartı görünümü (boyut, renk, kesik çizgi), bardak seçici, alt gezinme kenar boşlukları
- [ ] İstatistik ekranı
- [ ] Room v1 → v2 → v3 geçişi (eski kurulumun üstüne kurulum)
- [ ] Açık/koyu tema, büyük yazı tipi, TalkBack

## 2. Kullanıcıya sorulmuş, cevap bekleyen kararlar
- [ ] Bildirimde Android'in 3 düğme sınırı: şimdi bardak, 500, satır içi "Özel". Kabul mü?
- [ ] Görmezden gelinen hatırlatmalarda sıklığı azaltma (bıkma riski)
- [ ] Widget'ta 250/750 ml düğmeleri
- [ ] Ayarlardaki ±50 ml bardak düğmeleri kalsın mı (artık ana ekranda seçici var)
- [ ] Kayıt kaynağı için ikon (şimdi yazı: Uygulama/Bildirim/Widget)
- [ ] Ayrı "Hatırlatıcı" sekmesi (şimdi Ayarlar'da)
- [ ] "Geridesin" dilinin yumuşatılması

## 3. İlk spec'te olup yapılmayanlar
- [ ] Geçmiş güne kayıt ekleme (U-04)
- [ ] JSON dışa/içe aktarma yedeği (F-11)
- [ ] Dil (tr/en elle seçim) ve tema ayarı; Material You (B-08)
- [ ] "Hatırlatmalar gelmiyor mu?" pil rehberi: marka tespiti, dontkillmyapp (T-06)
- [ ] "Tam zamanında" alarm seçeneği, SCHEDULE_EXACT_ALARM (T-03)
- [ ] Yeniden başlatma alarmlarının yalnızca hatırlatma açıkken etkinleştirilmesi (T-04)
- [ ] ktlint + detekt, CI'da zorunlu (T-10)
- [ ] Erişilebilirlik gözden geçirme (U-11), 48 dp dokunma alanı denetimi
- [ ] Compose UI testlerinin genişletilmesi (şimdi tek duman testi), Room DAO ve migration testleri

## 4. İstenmedi / sonraya (Faz 2-3)
Hızlı Ayarlar kutucuğu, içecek türleri (çay, kahve katsayıları), Ramazan/oruç modu, antrenman günü düğmesi, seri ve sessiz rozetler, Health Connect, Wear OS, alışkanlık öğrenme.

## 5. Yayın
- [ ] Release imzası: GitHub secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD; /root/anahtarlar'dan hangi anahtar (kullanıcı kararı, anahtara dokunulmadı)
- [ ] targetSdk 36 + AGP 9 + Gradle 9: ayrı SDK kurulumu (~1 GB), kapatılan büyük Dependabot PR'ları bunu bekliyor
- [ ] Play (veri güvenliği beyanı, konum/internet açıklaması) veya F-Droid/IzzyOnDroid
- [ ] Ekran görüntüleri ve fastlane görselleri, README ekran görüntüleri
- [ ] CHANGELOG sürüm bölümleri ve v0.1.x etiketi (yayın kararı kullanıcıda)

## 6. Teknik borç
- Room şeması dışa aktarımı kapalı (exportSchema=false)
- Hava ve konum: son bilinen konum yoksa tek seferlik istek yalnızca Android 11+
- Sunucuda 2 GB RAM: yerel test+lint 12–17 dk; doğrulama CI'da
- docs/SPEC.md ilk konuşmaya göre; tempo, sürahi, istatistik, alt gezinme ve hava ayrıntıları DECISIONS.md ve CHANGELOG.md'de

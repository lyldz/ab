# ab — proje hafızası

**ab** Farsça "su" demek. Offline çalışan, reklamsız, hesapsız, takipsiz bir Android su takibi uygulaması. Bu dosya projeyle ilgili bilinen her şeyin tek yerde özetidir: ürün kararları, kod haritası, çalışma düzeni, durum ve açık işler. Ayrıntılar için: `docs/SPEC.md`, `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/TODO.md`, `CHANGELOG.md`.

Son güncelleme: 2026-11-12 · Sürüm 0.3.2 · Testi (versionCode 302; 0.3.1 yayında, 0.3.2 hazırlandı) · Lisans GPL-3.0-or-later · Paket `com.lyldz.ab` · Repo `github.com/lyldz/ab` (public)

---

## 1. Uygulama ne yapar

Çoğu su uygulaması "saatte bir" gibi sabit ve bunaltıcı bildirim yollar. ab, günün belirlenen saat aralığı (rutin) ve günlük hedef su miktarından **kaç dakikada bir ne kadar içilmesi gerektiğini tam dakikaya hesaplar**; su içildikçe yeniden hesaplar. Kullanıcı bildirimden bıkıp uygulamayı bırakmamalıdır.

**Kullanıcının olmazsa olmazları (öncelik sırasıyla):**
1. Akıllı hatırlatma: sabit aralık (pencere × bardak ÷ plan hedefi, tam dakika), miktar kalan toplama göre değişir. Sabit alt sınır (45 dk gibi) ve yuvarlak saat yok. Geride kalınca bildirimler **sıklaşmaz**, yalnızca miktar büyür. Baskıcı olmayan dil.
2. Hızlı kayıt: yalnızca bildirim düğmesinden (uygulama açılmadan) ve ana ekran widget'ından.

**Yapılmayacaklar:** reklam, analytics, Firebase, Google Play Services, hesap/giriş, sabit saatli spam bildirim, hedef dolduktan sonra bildirim, suçlayıcı pop-up, hatırlatma dışı amaçla pil muafiyeti isteme.

---

## 2. Ürün kuralları

### Günlük hedef
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

- Soğukta hedef düşürülmez. Hareket ve hava ekleri ayardan kapatılabilir; hedef elle de belirlenebilir (500–8000 ml); 5 L üstünde uyarı. "Tahmindir, tıbbi tavsiye değildir."
- Rakamlar resmî bir tablodan değil tahmindir (EFSA 2,0/2,5 L toplam su; IOM 2,7/3,7 L toplam su; 30–35 ml/kg resmî kılavuz değil, bu yüzden 25 ml/kg seçildi).
- Sıcaklık eki yalnızca **bugün** alınmış değerle uygulanır; dünün değeri hedefi etkilemez.

### Hatırlatma ve tempo
- Rutin penceresi = **kalkış → yatıştan 2 saat önce** (varsayılan 06:30–20:00 su içme, yatış 22:00; her gün aynı). Kullanıcı kalkış ve yatış girer, 2 saat otomatik düşülür (`STOP_BEFORE_BED_MIN`), altında not gösterilir. Pencere dışında **hatırlatma** yok. Durum bildirimi ise uygulama açılınca ve kurulum bitince sessizce gösterilir (pencere dışında başlığı "Bugünkü su"); hatırlatmalar yalnızca hesaplanan dakikada sesle gelir.
- **Aralık günün başında sabit:** `baseIntervalMin` = pencere × bardak ÷ **plan hedefi** (kilo + hareket, elle hedef varsa o; **hava eki yok**), tam dakika. Yalnızca kilo, hareket, rutin saatleri, bardak boyutu ya da elle hedef değişince değişir (varsayılan 200 ml bardak, 06:00–18:00, 2.850 ml → 51 dk). Hava sıcaklığı sıklığı değil miktarı artırır.
- **Miktar kalan toplama göre:** `nextPlan` = kalan ml ÷ pencereye sığan kalan hatırlatma sayısı, 50 ml'ye yuvarlanır, 100–500 ml arası, kalandan fazla olmaz ("1.900 ml kaldı → 250 ml iç"). Geride kalan kişi aynı aralıkla ama daha büyük miktar görür; tempo kartı geri kalmayı gösterir.
- Plan çıpası = son içiş **veya** son gösterilen hatırlatma (hangisi geçse); yani görmezden gelinen hatırlatmadan sonra bir sonraki aralık yine gelir. Hedef dolunca veya sıradaki hatırlatma pencereyi aşıyorsa o gün bildirim yok.
- **Tempo** = rutin penceresinde düz çizgi: pencere başında 0, sonunda hedefin tamamı. "X ml geridesin / ilerdesin / tam tempodasın" (eşik ±50 ml, 10 ml'ye yuvarlanır). Ana ekran kartı ve bildirim başlığı aynı kuralı kullanır.

### Hava ve konum
- Open-Meteo (hesapsız, anahtarsız): `api.open-meteo.com` tahmin, `geocoding-api.open-meteo.com` şehir arama. Servise yalnızca ~11 km'ye yuvarlanmış koordinat (1 ondalık) ve yazılan şehir adı gider.
- Günde bir kez: sabah rutin başında ayrı alarmla ve günün ilk açılışında. Bugün alındıysa tekrar çekmez; başarısızsa işaretlemez, sonraki açılış/sabah yeniden dener.
- Konum izni **isteğe bağlı**, yalnızca yaklaşık konum (`ACCESS_COARSE_LOCATION`), Play Services yok (Android `LocationManager`: son bilinen konum, yoksa Android 11+ tek seferlik istek). İzin yoksa şehir elle seçilir.
- Sabah hava hedefi yükseltirse (ek > 0, elle hedef yok) ayrı sessiz kanalda bildirim: "Bugün hava 33°C — Hedefin 2.750 ml oldu (+750 ml hava eki)".

### İzinler
| İzin | Neden | Nasıl istenir |
|---|---|---|
| `POST_NOTIFICATIONS` (Android 13+) | Hatırlatmalar | Onboarding'de ayrı ekran, "Şimdi değil" var |
| `ACCESS_COARSE_LOCATION` | Şehrin sıcaklığı | Onboarding'de ayrı ekran + Ayarlar'da düğme, isteğe bağlı |
| `INTERNET` | Günde bir sıcaklık isteği, şehir arama, güncelleme denetimi (yalnızca dokununca) | Sormaz (normal izin) |
| `REQUEST_INSTALL_PACKAGES` | Uygulama içi güncelleyici APK kurulumu | Güncelle'ye basınca sistemin "bu kaynaktan yükleme" ekranı açılır |
| `RECEIVE_BOOT_COMPLETED` | Yeniden başlatma sonrası alarmları kurmak | Sormaz (normal izin) |

Kütüphanelerden birleşen normal izinler: `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`. İstenmeyenler: pil muafiyeti (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`.

---

## 3. Ekranlar

Tasarım kaynağı: kullanıcının `/root/ab-water-reminder/` klasörü (CLAUDE.md, ab_su_tasarimi_claude.txt, PROMPT_FOR_CLAUDE.md, `index.html` interaktif prototip; repo dışında). Kopyası ve marka görseli `docs/design/brand/`. Ana ekran prototipe bire bir uyar; sapmalar bu bölümün sonunda.

- **Karşılama:** büyük "Āb" wordmark (280 dp), "Hoş geldiniz", Başla.
- **Kurulum (5 adımlı sihirbaz, geri tuşu/Geri düğmesi; Antigravity görev dosyasından, `/root/ab_repo/CLAUDE_TASK_ONBOARDING.md`):** (1) karşılama, (2) kilo (hazır 60–100 düğmeleri) + 3 hareket kartı, (3) kalkış/yatış saat seçici kartları (M3 TimePicker) + not + bardak 200/250/350/500, (4) izinler: bildirim, konum (+ sıcaklık sonucu, "Şehir ara") ve pil notu, (5) özet: büyük hedef, kırılım, "yaklaşık her X dakikada bir Y ml". Eski ayrıntı: karşılama → bilgiler (kilo 30–250, hareket, kalkış ve yatış saati ±30 dk (pencere otomatik kalkış → yatıştan 2 saat önce, altında not), canlı hedef önizlemesi) → bildirim izni (Android 13+) → **konum + hava**: "Konumumu kullan" izin ister, yaklaşık konumu bulur, bugünün sıcaklığını çeker ve kartta gösterir ("Bugün hava 17°C", "Sıcaklık hedefine +250 ml ekliyor" / "ekstra su gerekmiyor", "Bugünkü hedefin X ml"); konum yoksa/başarısızsa "Şehir ara"; "Şimdilik geç" ve "Kurulumu bitir". Sonuç kurulumla birlikte kaydedilir (ana ekranda hemen görünür).
- **Bugün (ana sayfa):** üstte büyük "Āb" wordmark (60 dp, kalın) + aqua damla, sağda bildirim çanı (Ayarlar'a gider); altında selamlama ("Bugün harikasın!" / isim varsa "Merhaba, X!"); "GÜNLÜK SU TÜKETİMİ" başlığı ve tempo cümlesi ("X ml geridesin / ilerdesin / tam tempodasın", hedef doldu, bugün plan yok); **dolu dairesel gösterge** (`WaterOrb`: aqua ışıltı halkası, iç yüzey, iki katmanlı dalga: arka #A0E7E5 %65, ön #88D9E6 %85, farklı faz/hız, ince kesik çizgi = şu an olman gereken seviye; ortada "%34" rozeti, büyük "600 ml", "Hedef", "Kalan"); **zaman çizelgesi kartı** (onay işaretli düğümler, son düğüm "Sıradaki" = bir sonraki hatırlatma saati ve +gereken ml, çoksa yatay kayar, "3 kayıt · 1 gün seri"); gradyanlı **"+ Su Ekle (200 ml)"** düğmesi; (alttaki hızlı kartlar kaldırıldı: miktar "+ Su Ekle"ye uzun basınca seçilir ve hemen eklenir; ilk 3 açılışta oklu geçici ipucu); Geri al · Özel · Bardak boyutu (simgeli seçici); hava satırı; "Bugünün kayıtları" listesi (⋮ Düzenle/Sil, silmede ve eklemede "Geri al" snackbar'ı).
- **Grafikler:** 7 / 30 gün çubuk grafiği, günün hedef çizgisi, ortalama, hedefe ulaşılan gün, en iyi gün (yalnızca kayıtlı günler). Geçmiş hedef için günlük anlık görüntü (`day_goal`).
- **Geçmiş:** son 30 gün, en yeni üstte, her gün için "ml / hedef ✓" ve ilerleme çubuğu.
- **Ayarlar (gruplu kartlar):** Profil (isim, kilo, hareket) · Günlük hedef (hesaplanan hedef, hareket/hava eki anahtarları, elle hedef, 5 L uyarısı) · Gün düzeni (kalkış, yatış + not, bardak boyutu) · Hava ve konum (şehir ara, konumumu kullan, kaldır, bugünün sıcaklığı, Open-Meteo atfı) · Görünüm (tema Sistem/Açık/Koyu) · İzinler ve bildirimler (bildirim ve konum izni durumu + sistem ayarına kısayol, pil rehberi) · Yedek · Güncelleme · Hakkında (sürüm, lisans, kaynak); değişiklikler 0,5 sn sonra kendiliğinden kaydedilir (Kaydet/Vazgeç yok, geçersiz alan kaydedilmez).
- **Yedek:** Ayarlar > Yedek: "Yedeği dışa aktar" (dosya seçici, `ab-yedek-<tarih>.json`) ve "Yedekten geri yükle" (özet + onay penceresi: kayıt/gün sayısı; kayıtlar eklenir, aynı an+miktar atlanır, ayarlar yedektekilerle değişir, sonra ana ekrana dönülür). Biçim: `ab-backup` sürüm 1 (settings, intakes, goals); bugünün sıcaklığı, son bildirim ve kurulum bayrağı yazılmaz. Dosya kilo, kayıtlar ve yaklaşık konum içerir; hiçbir yere gönderilmez. Giriş ve sunucu yok.
- **Güncelleme (uygulama içi güncelleyici):** "Güncellemeyi denetle" yalnızca dokununca `api.github.com/repos/lyldz/ab/releases/latest`e tek GET atar (User-Agent `ab-android`, kişisel veri yok); yeni sürüm varsa notlarını (CHANGELOG'daki madde satırları) gösterir, "İndir ve kur" APK'yı önbelleğe indirir ve sistem yükleyicisini açar (FileProvider). Bilinmeyen kaynak izni yoksa ilgili ayar ekranı açılır. Yeni APK **aynı anahtarla** imzalıysa üstüne kurulur.
- **Alt gezinme (4 sekme):** Bugün, Grafikler, Geçmiş, Ayarlar (Canvas simgeleri).
- **Bildirim:** başlık = tempo durumu, gövde "200 ml iç · 1.250 / 2.750 ml" + ilerleme çubuğu; düğmeler: bardak, 500 (bardak 500 ise 1000), satır içi "Özel". Android en fazla 3 düğme gösterir. Kayıttan sonra bildirim kapanmaz, yerinde ve sessizce güncellenir. Uygulama açılınca/kurulumdan sonra (kapatıldıysa) yeniden görünür.
- **Widget (Glance):** ilerleme çubuğu, "1.250 / 2.750 ml", +bardak/+500/+1000 ve "Özel"; açık/koyu renk çifti.
- **Su hareketi:** `WaterOrb` ivmeölçerle (izin yok, yalnızca ekran açıkken) suyu eğime göre yatay tutar (çizim eğimin tersine döner, açı yaylı sönümle yumuşar) ve telefon sallanınca dalga genliğini artırır; yatay duruşta eğim 0. Ayarlar > Görünüm > "Su, telefonun eğimine ve hareketine göre oynasın" ile kapatılır (`Settings.motionWater`, varsayılan açık). Yön işareti cihazda doğrulanmadı: ters dönerse `rotate(m.x, c)` işareti çevrilir. Gösterge: su dairesi halkanın %82'si (eskiden %74), en çok 380 dp.
- **Tema:** Açık: Aqua #88D9E6, Mint #A0E7E5, Deep Navy #0F203C, Pearl #F8FAFC, Slate #64748B, Mist #E2E8F0 (düğme yazıları için Material primary biraz koyu teal #1F7F92). **Koyu = siyah-beyaz (OLED):** zemin #000000, kart #111316, yazı beyaz, aqua vurgu. Kullanıcı akşam ekranların siyah olduğunu söyleyip koyu tema istedi; tema sistemi izler, Ayarlar'dan zorlanabilir. Durum/gezinme çubuğu simgeleri temaya göre. Yazı tipi **Plus Jakarta Sans** (uygulamada gömülü, `res/font`).
- **İkonlar/marka:** "Āb" (makron) yazı logosu — Cormorant Garamond (OFL) glifleri yola çevrilmiş vektör — + aqua damla; uygulama ikonu inci zemin, aqua gradyan; temalı (monochrome) katman; bildirim ikonu damla siluetinin içine negatif alanla oyulmuş "Āb" (`evenOdd`). Wordmark drawable'ları: `ic_wordmark` (lacivert), `ic_wordmark_light` (beyaz).
- **Diller:** `values/` (İngilizce), `values-tr/` (Türkçe); kodda sabit metin yok. Türkçe ek hatasından kaçınmak için "10:45'e kadar" gibi ekli saat kalıbı kullanılmaz.

**Prototipten bilerek ayrılanlar:** WorkManager yok (akıllı alarm AlarmManager ile); bildirimde "İçtim / 15 Dk Ertele" yok (3 düğme sınırı); prototip örnek verileri yok (gerçek kayıtlar); "Sıfırla/Maksimum" yok (yerine Geri al); logo ligatürü ("a ve b tek gövde") çizilmedi; hedef 2500 sabit değil, kilo/hareket/havaya göre. Eski sürahi kodu (`WaterPitcher`) `Vessels.kt`'de duruyor, kullanılmıyor.

---

## 4. Kod haritası (`app/src/main/java/com/lyldz/ab/`)

| Yol | Ne yapar |
|---|---|
| `AbApp.kt` | Application: `repo`, `reminders`; ortak yol `addWater/undoWater/deleteEntry/restoreEntry/editEntry` → `afterChange()` (alarmı tazele + bildirimi güncelle + widget'ı yenile) |
| `MainActivity.kt` | `AbRoot`: tema (ayara göre), sistem çubuğu stili, ilk açılışta hava + alarm + durum bildirimi; `AbContent`: onboarding ya da 4 sekme; `EXTRA_CUSTOM` |
| `core/Hydration.kt` | Saf mantık: `Activity`, `tempExtraMl`, `baseGoalMl`, `dailyGoalMl`, `nextPlan`, `paceMl`, `roundTo10`, sabitler |
| `core/Day.kt`, `core/Stats.kt` | Gün sınırları (yaz saati dahil), `withMinuteOfDay`; `buildDayStats`, `summarize` |
| `data/Db.kt` | Room v3: `intake` (id, atMs, ml, source) ve `day_goal` (day, goalMl); `MIGRATION_1_2` (source sütunu), `MIGRATION_2_3` (day_goal) |
| `data/SettingsStore.kt` | `Settings` + DataStore (weight_kg, activity, cup_ml, window_start_min, window_end_min, use_activity, use_weather, goal_override_ml, city, lat, lon, temp_c, temp_day, onboarded, last_notified_ms, name, theme_mode) |
| `data/AbRepository.kt` | Uygulama, widget ve bildirimin ortak tek veri kapısı; `today()`, `stats(days)` akışları |
| `data/WeatherApi.kt`, `data/Loc.kt` | Open-Meteo istemcisi ve ayrıştırıcı; yaklaşık konum |
| `data/Backup.kt` | Yedek biçimi: `toJson`, `parse` (doğrulamalı); `AbRepository.exportData/importData`, `AbApp.importBackup`; arayüz `ui/BackupSection.kt` |
| `data/Updater.kt` | GitHub sürüm denetimi (`fetchLatest`, `parseRelease`, `isNewer`, `cleanNotes`), APK indirme, kurulum intent'i (FileProvider) |
| `notify/Notifier.kt` | Kanallar (`reminders` varsayılan önem, `goal_updates` düşük), bildirimler |
| `notify/Reminders.kt` | Tek hatırlatma alarmı (istek kodu 0) + sabah alarmı (istek kodu 1); `refreshWeather`, `onMorning`, `onFire`, `refreshNotification(force)`, `paceTitle`; her `reschedule` günlük hedef anlık görüntüsünü yazar ve widget'ı tazeler |
| `notify/Receivers.kt` | `ReminderReceiver` (alarm, bildirim düğmeleri, dışarıdan erişilemez), `SystemEventsReceiver` (boot, saat, saat dilimi, paket güncelleme) |
| `widget/AbWidget.kt` | Glance widget ve `AddWaterAction` |
| `ui/` | `HomeScreen` (üst çubuk, gösterge, `TimelineCard`, `QuickCard`), `OnboardingScreen`, `SettingsScreen` (+ `CityDialog`), `UpdateSection`, `StatsScreen`, `HistoryScreen`, `BottomBar`, `Vessels` (`WaterOrb`, eski `WaterPitcher`, kare saatli `rememberSmoothed`/`rememberPhase`), `MainViewModel`, `Format`, `theme/Theme.kt` (`AbColors` belirteçleri, `Ab` temaya duyarlı renkler, açık/koyu şema, Plus Jakarta Sans) |

**Veri akışı:** kayıt → `AbApp.addWater` → `repo.addIntake` → `reminders.reschedule` (alarm + sabah alarmı + hedef anlık görüntüsü + widget) → `refreshNotification`. Plan yoksa (hedef doldu/pencere bitti) yarın rutin başına "uyandırma" alarmı kurulur, bildirim göstermez.

**Alarm:** inexact `setAndAllowWhileIdle` (izin gerektirmez; Doze'da dakikalar kayabilir). "Tam zamanında" seçeneği henüz yok.

---

## 5. Derleme, test, CI, dağıtım

- **Yığın:** Kotlin 2.0.21, AGP 8.7.3, Gradle 8.11.1, Compose BOM 2024.06.00 (Material 3), lifecycle **2.7.0** (2.8.0 ile BOM 2024.06 uyumsuz, açılışta çöker), Room 2.6.1 + KSP 2.0.21-1.0.28, DataStore 1.1.1, Glance 1.1.0, Plus Jakarta Sans (OFL) yazı tipi. `minSdk 26`, `compileSdk/targetSdk 34`. Hilt yok (elle bağımlılık).
- **Derleme:** `ANDROID_HOME=<sdk yolu> ./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs=-Xmx1g`. Az RAM'li makinede aynı anda tek gradle çalıştır.
- **Testler:** birim testleri `HydrationTest`, `DayTest`, `StatsTest` (seri dahil), `WeatherApiTest`, `UpdaterTest`; Robolectric `SmokeTest` gerçek `MainActivity`'yi onboarding'den 4 sekmenin tamamına kadar gezer. `*SmokeTest` sınıfları yalnızca CI'da (`CI=true`) ya da `AB_SMOKE=1` ile çalışır (ağırdır). **Ekran görüntüsü testleri** (`ScreenshotSmokeTest` açık, `ScreenshotNightSmokeTest` koyu, ortak `ScreenshotBase`): kurulum ve ana ekranın PNG'lerini `app/build/screenshots`a yazar, CI artifact'ı `ab-screenshots`; telefon olmadan arayüzü görmenin yolu: `gh run download <id> -n ab-screenshots`. Koyu sınıf ayrı SDK (33) kullanır, çünkü aynı Robolectric kum havuzunda DataStore durumu testler arası paylaşılır. Lifecycle 2.8.0 hatasını geri koyunca yakaladığı CI'da doğrulandı.
- **CI (`.github/workflows/ci.yml`):** push (main) ve PR'da `testDebugUnitTest lintDebug assembleDebug`, debug APK `ab-debug-apk` artifact'ı. **Release (`release.yml`):** `v*` etiketinde imzalı `app-release.apk`; GitHub secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` tanımlı (yeni `ab-release` anahtarı, `/root/anahtarlar/ab-release.jks` + `ab-release-keystore.properties`, yalnızca sunucuda ve secret'larda; yedeği kullanıcıda olmalı).
- **Test dağıtımı:** pre-release `test` (etiket `test`, `v*` değil, release workflow'unu tetiklemez), asset `ab-debug.apk`: https://github.com/lyldz/ab/releases/download/test/ab-debug.apk. Yerel derleme debug anahtarıyla imzalıdır; CI'ın ürettiği APK farklı anahtarla imzalanır (üstüne kurulmaz). Yenileme: `gh release upload test <apk> --clobber`.
- **Sürümleme:** yeni özellik gelene kadar yalnızca son rakam artar (0.3.1, 0.3.2…); yeni özellikte orta rakam ve yeni su temalı seri adı: 0.2 Damla, 0.3 Testi (su kabı), 0.4 Pınar (`core/Version.kt`; GitHub sürüm başlığı "ab 0.3.1 · Testi"). SemVer, `app/build.gradle.kts` içindeki `appVersion`; `versionCode = MAJOR*10000 + MINOR*100 + PATCH`; fastlane sürüm notu `fastlane/metadata/android/<dil>/changelogs/<versionCode>.txt`.
- **Yayın adımları:** `appVersion` artır, CHANGELOG'u `[x.y.z] - tarih` yap, `fastlane/.../changelogs/<versionCode>.txt` (tr + en) ekle, PR → CI → birleştir → `git tag -a vX.Y.Z` + `git push origin vX.Y.Z` → release workflow APK'yı yayınlar; sürüm notunu CHANGELOG bölümünden `gh release edit --notes-file` ile yaz (uygulama içi güncelleyici bu notu gösterir).
- **Git düzeni:** `main` yayınlanabilir kalır; riskli değişiklikler kısa ömürlü dal + PR (CI geri bildirimi yerel derlemeden hızlı). Commit'ler Conventional Commits.

---

## 6. Durum (2026-11-12)

**Yapıldı ve CI'da doğrulandı:** hedef/tempo/plan mantığı, Room + DataStore, kurulum (konum+hava dahil), yeni ana ekran, zaman çizelgesi, kayıt listesi, 4 sekme, Geçmiş, bildirim + alarm + durum bildirimi, widget, ayarlar, hava, bardak seçici, istatistik, ikonlar, açık + koyu tema, tema ayarı, uygulama içi güncelleyici, duman ve ekran görüntüsü testleri.

**Telefonda (kullanıcı, MIUI) doğrulananlar:** uygulama açılıyor, kurulum çalışıyor, bildirim geliyor ve bildirimden kayıt çalışıyor, tasarım beğenildi ("olmaya başlamış"), **dalga animasyonu çalışıyor** (sistem animasyon ölçeğinden bağımsız kare saati animasyonuna geçince).

**Telefonda HENÜZ doğrulanmadı:** hatırlatmanın hesaplanan dakikada gelmesi (pil/otomatik başlatma kısıtları), widget, kurulumdaki konum+hava akışı (ekran görüntüsünde yalnızca ilk hâli görüldü), koyu tema, durum bildiriminin artık hemen görünmesi, uygulama içi güncelleyici (uçtan uca için release APK kurulu olmalı ve daha yeni bir sürüm yayınlanmalı), Room v1→v2→v3 geçişi, TalkBack.

**Git ve yayın:** tek dal `main` (tüm eski dallar silindi; `arayuz` kalmış olabilir). **Sürüm 0.2.0 yayında:** https://github.com/lyldz/ab/releases/tag/v0.2.0 (tag `v0.2.0`, imzalı `app-release.apk` ~8 MB, `CN=ab, O=ab`). PR #16 = uygulama içi güncelleyici (CI yeşil). `test` pre-release'indeki debug APK farklı anahtarla imzalı, release APK'sının üstüne kurulmaz (önce kaldır). Release APK'sından sonraki güncellemeler uygulama içinden gelir.

**Sonraki sürüm fikri:** 0.2.1 / 0.3.0 = güncelleyici + varsa telefon geri bildirimleri; sonra bildirimde "İçtim / 15 Dk Ertele", sessiz saatler, geçmiş günleri düzenleme, JSON yedek.

---

## 7. Açık sorular

1. Bildirimde 3 düğme sınırı (bardak, 500, satır içi Özel) kabul mü? "İçtim / 15 Dk Ertele" istenir mi?
2. Görmezden gelinen hatırlatmalarda sıklığı azaltma?
3. Widget'ta 250/750 ml düğmeleri?
4. Ayarlardaki ±50 ml bardak düğmeleri kalsın mı?
5. "Geridesin" dilinin yumuşatılması?
6. Dağıtım: yalnızca GitHub mı, F-Droid / Play da olacak mı?

Eksiklerin tam listesi: `docs/TODO.md`.

---

## 8. Dersler ve tuzaklar

- Derleme ve birim testleri çalışma anı Compose çökmelerini yakalamaz; `SmokeTest` bunun için var.
- `strings.xml` içinde kesme işareti `\'` olmalı (kaçışsız `'` kaynak derlemesini kırar).
- Kotlin derleyici arka plan sürecinin "daemon terminated unexpectedly" mesajı düşük RAM'de zararsızdır; asıl sonuç `BUILD SUCCESSFUL/FAILED`.
- Room şemasını değiştirirken migration yaz (kullanıcı eski kurulumun üstüne kuruyor); `exportSchema=false` olduğundan migration'lar elle doğrulanır.
- Widget ve bildirim aynı kayıt yolunu (`AbApp.addWater`) kullanmalı, yoksa görünümler birbirinden kopar.

- **Arayüzü görmek için CI ekran görüntüsü kullan** (Robolectric NATIVE grafik, `view.draw(Canvas)`; `captureToImage` Robolectric'te zaman aşımına düşer). Telefon geri bildirimiyle birlikte ana doğrulama yolu.
- Sonsuz Compose animasyonları testte boşta sayılmalı: `withFrameNanos` döngüsü testi 60 sn kilitler; `InfiniteAnimationPolicy` ile sarmala (`infiniteFrame`). Sistem "animasyon ölçeği" kapalıyken `animate*` API'leri durur (telefonda dalga görünmedi); kare saatli kendi animasyonumuz bunu aşar. Döngü hiç çalışmazsa hedef değer doğrudan gösterilir.
- Robolectric aynı SDK + grafik moduyla kum havuzunu paylaşır; DataStore durumu testler arasında sızar. Gerekirse farklı SDK ver.
- `Config` nitelik sırası: boyut, `night`, sonra yoğunluk (`...-normal-night-xxhdpi`).
- Kullanıcı geri bildirimini yanlış yorumlama: "kalabalık/renkler/gösterge" deyince tasarım yönergesinden uzaklaşıp sadeleştirmek yanlıştı; asıl tasarım kullanıcının klasöründeydi (`/root/ab-water-reminder`). Belirsizse önce o klasöre ve prototipe bak, bire bir uygula, sapmayı açıkça yaz. Prototipi headless Chromium ile çizdirebilirsin (`~/.cache/ms-playwright/chromium_headless_shell-*/chrome-linux/headless_shell --screenshot`).
- Durum bildirimi yalnızca alarm anında çıkarsa kullanıcı "bildirim göremedim" der; uygulama açılınca sessizce göster.
- Güvenlik denetimi kancaları: silme/dal silme/`rm -rf` öncesi "ne silinecek, geri alma, kullanıcının talimatı" yazılı sunulmalı; talimat bu turdaki isteği kapsamalı, yoksa komut engellenir (aşmaya çalışma).
- Squash-merge sonrası eski dala PR açma (çakışır); `main`'den yeni dal aç. `gh pr edit --base` bu depoda Projects (classic) hatası verir; yeni PR aç.
- İmza anahtarı yalnızca release için; debug (sunucu) ve release APK'ları birbirinin üstüne kurulmaz.


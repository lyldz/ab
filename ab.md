# ab — proje hafızası

**ab** Farsça "su" demek. Offline çalışan, reklamsız, hesapsız, takipsiz bir Android su takibi uygulaması. Bu dosya projeyle ilgili bilinen her şeyin tek yerde özetidir: ürün kararları, kod haritası, çalışma düzeni, durum ve açık işler. Ayrıntılar için: `docs/SPEC.md`, `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/TODO.md`, `CHANGELOG.md`.

Son güncelleme: 2026-11-12 · Sürüm 0.1.0 (versionCode 100, seri 0.1.x) · Lisans GPL-3.0-or-later · Paket `com.lyldz.ab` · Repo `github.com/lyldz/ab` (public)

---

## 1. Uygulama ne yapar

Çoğu su uygulaması "saatte bir" gibi sabit ve bunaltıcı bildirim yollar. ab, günün belirlenen saat aralığı (rutin) ve günlük hedef su miktarından **kaç dakikada bir ne kadar içilmesi gerektiğini tam dakikaya hesaplar**; su içildikçe yeniden hesaplar. Kullanıcı bildirimden bıkıp uygulamayı bırakmamalıdır.

**Kullanıcının olmazsa olmazları (öncelik sırasıyla):**
1. Akıllı hatırlatma: aralık = kalan süre ÷ (kalan ml ÷ blok). Alt sınır (45 dk gibi) ve yuvarlak saat yok. Geride kalınca bildirimler sıklaşmaz. Baskıcı olmayan dil.
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
- Rutin penceresi (varsayılan 06:00–18:00, her gün aynı) dışında bildirim yok.
- Her blokta içilecek miktar = bardak boyutu (varsayılan 200 ml, değiştirilebilir); blok = min(bardak, kalan ml). Aralık = kalan süre × blok ÷ kalan ml, tam dakika.
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
| `INTERNET` | Günde bir sıcaklık isteği, şehir arama | Sormaz (normal izin) |
| `RECEIVE_BOOT_COMPLETED` | Yeniden başlatma sonrası alarmları kurmak | Sormaz (normal izin) |

Kütüphanelerden birleşen normal izinler: `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`. İstenmeyenler: pil muafiyeti (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`.

---

## 3. Ekranlar

- **Karşılama:** büyük logo, "Hoş geldiniz", Başla.
- **Onboarding (adımlar, geri tuşuyla geri gidilir):** karşılama → bilgiler (kilo 30–250, hareket seviyesi, uyanma/uyku saati ±30 dk, canlı hedef önizlemesi) → bildirim izni (yalnızca Android 13+) → konum izni (isteğe bağlı).
- **Ana sayfa (marka görseline göre):** "Selam", dairesel su göstergesi (WaterOrb; kapasite = günlük hedef, altın çift halka, altın kesik çizgi + yan oklar = şu an olman gereken seviye), altında "içilen / hedef ml · %", tempo kartı ("X ml geridesin" + sıradaki hedef), ortada büyük "+" düğmesi (varsayılan bardak) ve yanlarda 4 hızlı çip (250/500/750/1000), Özel ve Bardak düğmeleri (simgeli seçici), Hedef ve Seri (üst üste hedefe ulaşılan gün) kartları, hava satırı, "Bugünün kayıtları" (⋮ Düzenle/Sil, Geri al).
- **Gösterge:** WaterOrb ana ekranda; eski WaterPitcher (sürahi) kodu Vessels.kt'de duruyor, kullanılmıyor.
- **İstatistik:** 7 / 30 gün çubuk grafiği, her günün hedef çizgisi, ortalama, hedefe ulaşılan gün, en iyi gün. Ortalama ve hedef günleri yalnızca kayıtlı günleri sayar. Geçmiş hedef için günlük anlık görüntü tutulur (hedef her gün değişebilir).
- **Ayarlar:** kilo, hareket, hareket/hava eki anahtarları, hava (şehir ara, konumumu kullan, kaldır, bugünün sıcaklığı, Open-Meteo atfı), rutin saatleri, bardak boyutu (±50 ml), elle hedef, 5 L uyarısı. Değişiklikler yalnızca **Kaydet** ile yazılır.
- **Alt gezinme:** Ana sayfa, İstatistik, Ayarlar (Canvas ile çizilmiş simgeler).
- **Bildirim:** başlık = tempo durumu, gövde "200 ml iç · 1.250 / 2.750 ml" + ilerleme çubuğu; düğmeler: bardak, 500 (bardak 500 ise 1000), satır içi "Özel" (uygulamayı açmadan miktar yazılır; Android en fazla 3 düğme gösterir). Kayıttan sonra bildirim kapanmaz, yerinde ve sessizce güncellenir; hedef tamamsa başlık "Bugünün hedefi tamam".
- **Widget (Glance):** ilerleme çubuğu, "1.250 / 2.750 ml", +bardak/+500/+1000 ve "Özel" (uygulamayı özel miktar penceresi açık başlatır).
- **Tema/marka:** Aqua/Deep Navy/Pearl (açık: birincil `#2C7C8C`, zemin `#F4F1EA`, metin `#0F2A4A`; koyu: `#7CCFCB` / `#0B1D33`; altın vurgu `#B08A3E`). İkon: inci zemin, "Ãb" yazı logosu (Cormorant Garamond SemiBold, OFL, vektör) + damla, aqua gradyan; bildirim ikonu aynı şeklin beyaz silueti. Referans: `docs/design/brand/`.
- **Diller:** `values/` (İngilizce) ve `values-tr/` (Türkçe). Kodda sabit metin yok. Türkçe ek hatasından kaçınmak için "10:45'e kadar" gibi ekli saat kalıbı kullanılmaz.

---

## 4. Kod haritası (`app/src/main/java/com/lyldz/ab/`)

| Yol | Ne yapar |
|---|---|
| `AbApp.kt` | Application: `repo`, `reminders`; ortak yol `addWater/undoWater/deleteEntry/restoreEntry/editEntry` → `afterChange()` (alarmı tazele + bildirimi güncelle + widget'ı yenile) |
| `MainActivity.kt` | `AbRoot`: ilk açılışta hava + alarm tazele; onboarding ya da sekmeler; `EXTRA_CUSTOM` |
| `core/Hydration.kt` | Saf mantık: `Activity`, `tempExtraMl`, `baseGoalMl`, `dailyGoalMl`, `nextPlan`, `paceMl`, `roundTo10`, sabitler |
| `core/Day.kt`, `core/Stats.kt` | Gün sınırları (yaz saati dahil), `withMinuteOfDay`; `buildDayStats`, `summarize` |
| `data/Db.kt` | Room v3: `intake` (id, atMs, ml, source) ve `day_goal` (day, goalMl); `MIGRATION_1_2` (source sütunu), `MIGRATION_2_3` (day_goal) |
| `data/SettingsStore.kt` | `Settings` + DataStore (weight_kg, activity, cup_ml, window_start_min, window_end_min, use_activity, use_weather, goal_override_ml, city, lat, lon, temp_c, temp_day, onboarded, last_notified_ms) |
| `data/AbRepository.kt` | Uygulama, widget ve bildirimin ortak tek veri kapısı; `today()`, `stats(days)` akışları |
| `data/WeatherApi.kt`, `data/Loc.kt` | Open-Meteo istemcisi ve ayrıştırıcı; yaklaşık konum |
| `notify/Notifier.kt` | Kanallar (`reminders` varsayılan önem, `goal_updates` düşük), bildirimler |
| `notify/Reminders.kt` | Tek hatırlatma alarmı (istek kodu 0) + sabah alarmı (istek kodu 1); `refreshWeather`, `onMorning`, `onFire`, `refreshNotification`, `paceTitle`; her `reschedule` günlük hedef anlık görüntüsünü yazar ve widget'ı tazeler |
| `notify/Receivers.kt` | `ReminderReceiver` (alarm, bildirim düğmeleri, dışarıdan erişilemez), `SystemEventsReceiver` (boot, saat, saat dilimi, paket güncelleme) |
| `widget/AbWidget.kt` | Glance widget ve `AddWaterAction` |
| `ui/` | `HomeScreen`, `OnboardingScreen`, `SettingsScreen`, `StatsScreen`, `BottomBar`, `Vessels` (sürahi, bardak simgesi), `MainViewModel`, `Format`, `theme/Theme.kt` |

**Veri akışı:** kayıt → `AbApp.addWater` → `repo.addIntake` → `reminders.reschedule` (alarm + sabah alarmı + hedef anlık görüntüsü + widget) → `refreshNotification`. Plan yoksa (hedef doldu/pencere bitti) yarın rutin başına "uyandırma" alarmı kurulur, bildirim göstermez.

**Alarm:** inexact `setAndAllowWhileIdle` (izin gerektirmez; Doze'da dakikalar kayabilir). "Tam zamanında" seçeneği henüz yok.

---

## 5. Derleme, test, CI, dağıtım

- **Yığın:** Kotlin 2.0.21, AGP 8.7.3, Gradle 8.11.1, Compose BOM 2024.06.00 (Material 3), lifecycle **2.7.0** (2.8.0 ile BOM 2024.06 uyumsuz, açılışta çöker), Room 2.6.1 + KSP 2.0.21-1.0.28, DataStore 1.1.1, Glance 1.1.0. `minSdk 26`, `compileSdk/targetSdk 34`. Hilt yok (elle bağımlılık).
- **Derleme:** `ANDROID_HOME=<sdk yolu> ./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs=-Xmx1g`. Az RAM'li makinede aynı anda tek gradle çalıştır.
- **Testler:** birim testleri `HydrationTest`, `DayTest`, `StatsTest`, `WeatherApiTest`; Robolectric `SmokeTest` gerçek `MainActivity`'yi onboarding'den tüm sekmelere kadar gezer. `SmokeTest` yalnızca CI'da (`CI=true`) ya da `AB_SMOKE=1` ile çalışır (ağırdır). Lifecycle 2.8.0 hatasını geri koyunca yakaladığı CI'da doğrulandı.
- **CI (`.github/workflows/ci.yml`):** push (main) ve PR'da `testDebugUnitTest lintDebug assembleDebug`, debug APK `ab-debug-apk` artifact'ı. **Release (`release.yml`):** `v*` etiketinde imzalı APK; GitHub secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` gerekir, henüz tanımlı değil.
- **Test dağıtımı:** pre-release `test` (etiket `test`, `v*` değil, release workflow'unu tetiklemez), asset `ab-debug.apk`: https://github.com/lyldz/ab/releases/download/test/ab-debug.apk. Yerel derleme debug anahtarıyla imzalıdır; CI'ın ürettiği APK farklı anahtarla imzalanır (üstüne kurulmaz). Yenileme: `gh release upload test <apk> --clobber`.
- **Sürümleme:** SemVer, `app/build.gradle.kts` içindeki `appVersion`; `versionCode = MAJOR*10000 + MINOR*100 + PATCH`; fastlane sürüm notu `fastlane/metadata/android/<dil>/changelogs/<versionCode>.txt`.
- **Git düzeni:** `main` yayınlanabilir kalır; riskli değişiklikler kısa ömürlü dal + PR (CI geri bildirimi yerel derlemeden hızlı). Commit'ler Conventional Commits.

---

## 6. Durum (2026-11-12)

**Yapıldı ve CI'da doğrulandı:** hedef/tempo/plan mantığı, Room + DataStore, onboarding, ana ekran, kayıt listesi (düzenle/sil/geri al), bildirim + alarm, widget, ayarlar, hava, bardak seçici, sürahi, alt gezinme, istatistik, ikon paketi, duman testi.

**Telefonda (kullanıcı, MIUI) doğrulananlar:** uygulama açılıyor, onboarding çalışıyor, bildirim geliyor, bildirimden kayıt çalışıyor (kayıttan sonra bildirimin kaybolması bulundu ve düzeltildi), karşılama ekranı eklendi.

**Telefonda HENÜZ doğrulanmadı:** hatırlatmanın hesaplanan dakikada gelmesi (pil/otomatik başlatma kısıtları!), widget, hava, sürahi/tempo görünümü, istatistik, Room v1→v2→v3 geçişi, açık/koyu tema, TalkBack.

**Git:** `main` = Faz 0 + Faz 1 dilim 1–6 + hava + bardak seçici + alt gezinme/istatistik. [PR #11](https://github.com/lyldz/ab/pull/11) (marka + yeni ana ekran, #10'un üstünde) ve dal `smoke-test` = [PR #10](https://github.com/lyldz/ab/pull/10): duman testi + tempo + sürahi + bildirim başlığı + `docs/TODO.md` + bu dosya (CI yeşil, `main`'e birleştirme bekliyor). Büyük Dependabot sürüm atlamaları (AGP 9, Kotlin 2.4, Gradle 9, BOM 2026) kapatıldı; targetSdk 36 geçişinde birlikte ele alınacak.

---

## 7. Açık sorular (kullanıcıya soruldu)

1. Bildirimde 3 düğme sınırı (bardak, 500, satır içi Özel) kabul mü?
2. Görmezden gelinen hatırlatmalarda sıklığı azaltma?
3. Widget'ta 250/750 ml düğmeleri?
4. Ayarlardaki ±50 ml bardak düğmeleri kalsın mı?
5. Kayıt kaynağı için ikon (şimdi yazı)? Ayrı "Hatırlatıcı" sekmesi?
6. "Geridesin" dilinin yumuşatılması?
7. Release imzası için hangi anahtar ve secrets; Play mi F-Droid mi?

Eksiklerin tam listesi: `docs/TODO.md`.

---

## 8. Dersler ve tuzaklar

- Derleme ve birim testleri çalışma anı Compose çökmelerini yakalamaz; `SmokeTest` bunun için var.
- `strings.xml` içinde kesme işareti `\'` olmalı (kaçışsız `'` kaynak derlemesini kırar).
- Kotlin derleyici arka plan sürecinin "daemon terminated unexpectedly" mesajı düşük RAM'de zararsızdır; asıl sonuç `BUILD SUCCESSFUL/FAILED`.
- Room şemasını değiştirirken migration yaz (kullanıcı eski kurulumun üstüne kuruyor); `exportSchema=false` olduğundan migration'lar elle doğrulanır.
- Widget ve bildirim aynı kayıt yolunu (`AbApp.addWater`) kullanmalı, yoksa görünümler birbirinden kopar.
- Ekran görüntüsünü ben (Claude) göremeden yazdım: görsel ölçüler (sürahi 150×210 dp, kenar boşlukları) cihazda ince ayar isteyebilir.

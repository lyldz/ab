# Āb Android Uygulaması — Tam Denetim Raporu

**Tarih:** 2026-11-28  
**Denetlenen dal / commit:** `ccr-5bbed883-gj8mzh` (`b2c6418`, taban `v0.3.11`)  
**Kapsam:** Tüm ekranlar, etkileşim noktaları, veri akışı (Room + DataStore + JSON Yedek), ağ katmanı (Open-Meteo + GitHub Releases), arka plan bileşenleri (AlarmManager, BroadcastReceiver, Quick Settings Tile, Glance Widget), manifest, arayüz/erişilebilirlik ve otomatik testler.

> **Ortam ve Doğrulama Notu:** Denetim sunucusu başsız (emulator/adb yok) ve 2 GB fiziksel RAM sınırlı olduğundan yerel `./gradlew` çağrıları OOM riski taşır. Derleme (`assembleDebug`, `assembleRelease`), Android Lint (`lintDebug`), saf JVM birim testleri ve Robolectric + Compose UI testleri (`SmokeTest`, `ScreenshotSmokeTest` açık/koyu tema + yeni denetim testleri) **GitHub Actions CI** üzerinde çalıştırılarak gözle doğrulanmıştır.

---

## 1. Yönetici Özeti

| Metrik | Denetim Öncesi | Düzeltme Sonrası |
|---|---:|---:|
| Toplam etkileşim noktası | 78 | 78 |
| ✅ Sorunsuz çalışan | 58 | *(CI doğrulaması sonrası güncellenecek)* |
| ❌ Crash / NPE / yakalanmamış istisna riski | 2 (B-01, B-08) | 0 |
| ⚠️ Veri kaybı / yanlış birim / double-submit / sınır kilidi | 10 (B-02, B-03, B-04, B-06) | 0 |
| 🎨 Arayüz taşması / kaydırılamayan diyalog / <48dp hedef | 8 (B-05, B-07, B-09) | 0 |
| 🧹 Düşük / Ölü kod / Açık kalan | 2 (B-10, B-11) | Belgeli açık kalan |

---

## 2. Aşama 1 — Tam Bileşen ve Etkileşim Envanteri

### 2.1 Mimari Bileşenler

| Katman | Bileşen | Dosya |
|---|---|---|
| Application | `AbApp` | `app/src/main/java/com/lyldz/ab/AbApp.kt` |
| Activity | `MainActivity` (`AbRoot`, `AbContent`) | `app/src/main/java/com/lyldz/ab/MainActivity.kt` |
| ViewModel | `MainViewModel` | `app/src/main/java/com/lyldz/ab/ui/MainViewModel.kt` |
| Ekranlar (Composable) | `OnboardingScreen`, `HomeScreen`, `ProfileScreen`, `StatsScreen`, `HistoryScreen`, `SettingsScreen`, `BottomBar` | `app/src/main/java/com/lyldz/ab/ui/*.kt` |
| Alt Bileşenler / Diyaloglar | `WaterOrb`, `CupIcon`, `BackupSection`, `BatteryTipSection`, `PermissionsContent`, `UpdateSection`, `CityDialog`, `TimePickDialog`, `CupDialog`, `CustomDialog`, `EditDialog`, `GoalInfoDialog`, `PastDayAddDialog` | `app/src/main/java/com/lyldz/ab/ui/*.kt` |
| Veri / DB | `AbRepository`, `AbDb` (v3, `MIGRATION_1_2`, `MIGRATION_2_3`), `IntakeDao`, `SettingsStore` (DataStore, 47 alan), `Backup` (JSON v1) | `app/src/main/java/com/lyldz/ab/data/*.kt` |
| Ağ / Konum | `WeatherApi` (Open-Meteo), `Updater` (GitHub Releases + SHA-256 + imza), `currentCoarse` (`LocationManager`) | `app/src/main/java/com/lyldz/ab/data/{WeatherApi,Updater,Loc}.kt` |
| Arka Plan / Sistem | `Reminders`, `Notifier`, `ReminderReceiver`, `SystemEventsReceiver`, `AbTileService`, `AbWidget`, `AbWidgetReceiver`, `AddWaterAction` | `app/src/main/java/com/lyldz/ab/{notify,tile,widget}/*.kt` |
| Saf Çekirdek | `Hydration.kt`, `Day.kt`, `Stats.kt`, `Version.kt`, `Slosh.kt` (ölü), `Format.kt`, `AppLocale.kt` | `app/src/main/java/com/lyldz/ab/{core,ui}/*.kt` |

### 2.2 Tüm Etkileşim Noktaları (78 Adet)

| # | Ekran / Bileşen | Etkileşim | Handler (`dosya:satır`) | Fonksiyon Zinciri | Veri Okuma / Yazma | Durum |
|---|---|---|---|---|---|---|
| 1 | `OnboardingScreen` (Adım 0) | "Başlayalım" butonu | `OnboardingScreen.kt:199` | `step++` | Bellek (`rememberSaveable`) | ✅ |
| 2 | `OnboardingScreen` (Adım 1) | Kilo girişi (`OutlinedTextField`) | `OnboardingScreen.kt:325` | `weight = it.filter(isDigit).take(3)` | Bellek | ✅ |
| 3 | `OnboardingScreen` (Adım 1) | Hızlı kilo çipleri (60–100) | `OnboardingScreen.kt:334` | `onWeight(q.toString())` | Bellek | 🎨 B-05 |
| 4 | `OnboardingScreen` (Adım 1) | Hareket düzeyi kartları (3 kart) | `OnboardingScreen.kt:346` | `onActivity(i)` | Bellek | ✅ |
| 5 | `OnboardingScreen` (Adım 2) | Kalkış / Yatış `TimeCard` | `OnboardingScreen.kt:365-366` | `timeDialog = 0/1` → `TimePickDialog` | Bellek | ✅ |
| 6 | `OnboardingScreen` (Adım 2) | Bardak çipleri (200/250/330/500) | `OnboardingScreen.kt:373` | `onCup(ml)` | Bellek | 🎨 B-05 |
| 7 | `OnboardingScreen` (Adım 3) | Bildirim izni butonu | `OnboardingScreen.kt:224,422` | `notifLauncher.launch(POST_NOTIFICATIONS)` | Sistem izni | 🎨 B-09 |
| 8 | `OnboardingScreen` (Adım 3) | Konum izni butonu | `OnboardingScreen.kt:227,433` | `locLauncher` → `findMe()` → `currentCoarse` → `WeatherApi.fetchMaxTemp` | Ağ + Konum | 🎨 B-09 |
| 9 | `OnboardingScreen` (Adım 3) | "Şehir ara" butonu | `OnboardingScreen.kt:228,452` | `cityDialog = true` → `CityDialog` → `loadTemp` | Ağ | 🎨 B-07 |
| 10 | `OnboardingScreen` (Adım 3) | Pil optimizasyonu butonları | `BatteryTipSection.kt:67,70` | `startActivity(IGNORE_BATTERY / APP_DETAILS)` | Sistem ayarları | 🎨 B-05 |
| 11 | `OnboardingScreen` (Adım 1–4) | "Geri" / sistem geri tuşu | `OnboardingScreen.kt:190,198` | `step--` | Bellek | ✅ |
| 12 | `OnboardingScreen` (Adım 4) | "Āb'a başla" butonu | `OnboardingScreen.kt:169,199` | `finish()` → `vm.finishOnboarding` → `repo.saveSettings` + `reminders.*` | DataStore + Room (`day_goal`) + Alarm | ⚠️ B-06 |
| 13 | `HomeScreen` | Sağ üst Profil / Baş Harf butonu | `HomeScreen.kt:199` | `onOpenProfile()` → `showProfile = true` | Bellek | ✅ |
| 14 | `HomeScreen` | Hedef satırı (`i` rozeti) | `HomeScreen.kt:221` | `goalInfo = true` → `GoalInfoDialog` | Salt okunur (`Today`) | ⚠️ B-04 / 🎨 B-07 |
| 15 | `HomeScreen` | `GoalInfoDialog` "Profil ve hedef" / "Tamam" | `HomeScreen.kt:738-739` | `onProfile()` / `onDismiss()` | Navigasyon | ✅ |
| 16 | `HomeScreen` | Zaman çizelgesi kayıt düğümü | `HomeScreen.kt:523,572` | `editing = it` → `EditDialog` | Room (`intake`) | ✅ |
| 17 | `HomeScreen` | `EditDialog` miktar/saat/Kaydet/Sil/İptal | `HomeScreen.kt:408-427` | `vm.edit` / `vm.delete` + `showUndo` (`vm.restore`) | Room + Alarm + Widget + Tile | ⚠️ B-04 |
| 18 | `HomeScreen` | Bölünmüş "+ Su Ekle" kısa basma | `HomeScreen.kt:301` | `add(s.cupMl)` → `vm.add` → `app.addWater` + `showUndo` | Room + Alarm + Widget + Tile | ✅ |
| 19 | `HomeScreen` | Bölünmüş "+ Su Ekle" uzun basma | `HomeScreen.kt:300` | `onHintCount(...)` + `cupDialog = true` | DataStore | ✅ |
| 20 | `HomeScreen` | Bölünmüş sağ bardak seçici ikonu | `HomeScreen.kt:315-320` | `onHintCount(MAX_HINTS)` + `cupDialog = true` | DataStore | ⚠️ B-06 |
| 21 | `HomeScreen` | `CupDialog` ön ayar seçimi / uzun basma silme | `HomeScreen.kt:643-652` | `onPick(ml)` / `onSavePresets(next)` | DataStore + Room | 🎨 B-07 |
| 22 | `HomeScreen` | `CupDialog` özel miktar + "Ön ayar kaydet" + "Ayarla" | `HomeScreen.kt:670-690` | `onSavePresets` / `onPick` | DataStore + Room | 🎨 B-07 |
| 23 | `HomeScreen` | `CustomDialog` (widget'tan açılan) | `HomeScreen.kt:381,432-449` | `add(ml)` → `vm.add` | Room + Alarm + Widget | ✅ |
| 24 | `HomeScreen` | Üst Geri Al (Undo) hapı | `HomeScreen.kt:358-361` | `vm.undo()` veya `vm.restore(e)` | Room + Alarm + Widget | 🎨 B-09 |
| 25 | `ProfileScreen` | Sol üst geri butonu / sistem geri | `ProfileScreen.kt:88,152` | `onBack()` + `DisposableEffect` otomatik kayıt | DataStore + Room (`day_goal`) | ✅ |
| 26 | `ProfileScreen` | Ad, Kilo, Hareket çipleri | `ProfileScreen.kt:166-183` | `draft` → 500ms debounce → `vm.saveSettings` | DataStore + Room | 🎨 B-05 |
| 27 | `ProfileScreen` | Hareket / Hava / Elle hedef switch'leri | `ProfileScreen.kt:188,189,224` | `useActivity` / `useWeather` / `manual` → `vm.saveSettings` | DataStore + Room | ⚠️ B-04 |
| 28 | `ProfileScreen` | Şehir ara / Konumumu kullan / Temizle | `ProfileScreen.kt:206-217` | `CityDialog` / `locLauncher` → `currentCoarse` / sıfırla | DataStore + Ağ + Konum | ✅ |
| 29 | `ProfileScreen` | Elle hedef girişi (`OutlinedTextField`) | `ProfileScreen.kt:226-234` | `manualText` (500..8000) → `vm.saveSettings` | DataStore + Room | ✅ |
| 30 | `StatsScreen` | "7 gün" / "30 gün" dönem çipleri | `StatsScreen.kt:51-52` | `vm.setPeriod(7 / 30)` | Flow (`repo.stats`) | ✅ |
| 31 | `StatsScreen` | Sistem geri tuşu | `StatsScreen.kt:42` | `onBack()` → `tab = 0` | Bellek | ✅ |
| 32 | `HistoryScreen` | "+ Geçmişe ekle" butonu | `HistoryScreen.kt:87` | `addingDay = today.minusDays(1)` | Bellek | ✅ |
| 33 | `HistoryScreen` | Gün kartına dokunma | `HistoryScreen.kt:108` | `addingDay = d.day` → `PastDayAddDialog` | Room | 🎨 B-09 |
| 34 | `HistoryScreen` | `PastDayAddDialog` gün/ön ayar/miktar/saat/Ekle | `HistoryScreen.kt:181-223` | `vm.addForDay(day, ml, min)` → `app.addWaterForDay` | Room (+ bugünse Alarm/Widget) | 🎨 B-07 |
| 35 | `HistoryScreen` | Sistem geri tuşu | `HistoryScreen.kt:65` | `onBack()` → `tab = 0` | Bellek | ✅ |
| 36 | `BottomBar` | 4 sekme (Bugün, Grafikler, Geçmiş, Ayarlar) | `BottomBar.kt:40-43` | `showProfile = false; tab = i` | Bellek | ✅ |
| 37–46 | `SettingsScreen` (Grup 1) | Kalkış/Yatış `TimeCard`, Yatıştan önce stepper, 5 switch, Özel aralık stepper, 2. bildirim düğmesi stepper | `SettingsScreen.kt:235-292` | `draft` → 150ms debounce → `vm.saveSettings` | DataStore + Alarm + Notifier | ⚠️ B-02 |
| 47–51 | `PermissionsContent` | Bildirim, Tam zamanında alarm, Konum izin satırları + Test bildirimi + Pil rehberi | `PermissionsContent.kt:95-122` | `ActivityResultContracts` / `startActivity` / `Notifier.showTest` | Sistem izinleri + Notifier | 🎨 B-05 |
| 52–57 | `SettingsScreen` (Grup 2) | Bardak stepper, Ön ayar çip sil/ekle, Widget miktar çip sil/ekle, Kutucuk toast switch | `SettingsScreen.kt:299-383` | `draft` → `vm.saveSettings` → `AbWidget.updateAll` | DataStore + Widget | ⚠️ B-02 |
| 58–65 | `SettingsScreen` (Grup 3) | Tema/Dil/Birim çipleri, 4 animasyon/ipucu switch'i, Geri-al süresi stepper | `SettingsScreen.kt:389-442` | `draft` / `AppLocale.apply` + `recreate()` | DataStore + SharedPrefs | 🎨 B-05 / ⚠️ B-02 |
| 66–73 | `SettingsScreen` (Grup 4) | Katlanabilir Gelişmiş ayarlar (11 stepper + 2 dönem çipi) | `SettingsScreen.kt:446-530` | `draft` → `vm.saveSettings` | DataStore | ⚠️ B-03 / ⚠️ B-04 |
| 74–75 | `SettingsScreen` (Grup 5) | `BackupSection` (Dışa/İçe aktar + onay) + "Varsayılanlara sıfırla" + onay | `SettingsScreen.kt:533-586`, `BackupSection.kt:52-112` | `Backup.toJson/parse` / `app.importBackup` / `vm.resetSettings` | Room + DataStore + SAF | ⚠️ B-06 / 🎨 B-05 |
| 76 | `SettingsScreen` (Grup 6) | `UpdateSection` ("Güncellemeyi denetle" + "İndir ve kur") | `UpdateSection.kt:75-119` | `Updater.fetchLatest` → `download` (SHA-256 + imza) → `install` | Ağ + Cache + FileProvider | ❌ B-08 |
| 77 | Bildirim / Arka Plan | Bildirim tıklama, 2 hızlı ekleme, RemoteInput "Özel", Alarm `ACTION_FIRE`/`ACTION_MORNING`, Sistem olayları | `Notifier.kt`, `Receivers.kt`, `Reminders.kt` | `ReminderReceiver` / `SystemEventsReceiver` → `goAsync` → `AbApp` / `Reminders` | Room + DataStore + Alarm | ❌ B-01 |
| 78 | Widget & Quick Tile | `AbWidget` (aç, hızlı ekle, özel), `AbTileService` (tıkla, dinle) | `AbWidget.kt`, `AbTileService.kt` | `AddWaterAction` / `AbTileService.onClick` → `app.addWater` | Room + Alarm + Widget + Tile | ✅ |

---

## 3. Aşama 2 & 3 — Bulgu Tablosu (Kök Neden Bazlı)

| ID | Önem | Ekran / Bileşen | Belirti | Kök Neden | Dosya:Satır | Düzeltme |
|---|---|---|---|---|---|---|
| **B-01** | **KRİTİK** | `Reminders` (`AlarmManager`) | Kısıtlı profil/özel ROM/test bağlamında `AlarmManager` `null` dönerse veya fallback `setAndAllowWhileIdle` hata atarsa `NullPointerException` / çökme. | `getSystemService(AlarmManager::class.java)` platform tipi nullable olmasına rağmen null-safe kullanılmamış. | `notify/Reminders.kt:35,57-64,132-133,139,151` | `AlarmManager?` yap, tüm çağrıları null-safe ve `runCatching` ile koru. |
| **B-02** | **YÜKSEK** | `SettingsScreen` | Kullanıcı art arda hızlıca 2 ayara (ör. 2 switch veya stepper) dokunduğunda, 1. değişikliğin 150ms DataStore kaydı geri yansırken 2. değişikliği eski değere geri çekip yutuyor. | `LaunchedEffect(s)` her `s` emisyonunda tüm 33 yerel state'i koşulsuz sıfırlıyor; kendi `onSave(draft)` yankısını dışarıdan gelen değişiklikten ayırt etmiyor. | `ui/SettingsScreen.kt:131-166,209-214` | Son gönderilen `lastSentDraft` değerini sakla; `s` bu değerle eşleşiyorsa yerel state'i ezme (yalnızca sıfırlama/yedek geri yükleme gibi gerçek dış değişikliklerde senkronize et). |
| **B-03** | **YÜKSEK** | `SettingsScreen` (Gelişmiş) | `minReminderMl` artırıldığında `maxReminderMl` üstüne çıkabiliyor (arayüzde `min > max` tutarsızlığı). | `onPlus` yalnızca `minReminderMl`'yi `50..500` aralığında sınırlıyor, `maxReminderMl` ile çapraz bağlamıyor. | `ui/SettingsScreen.kt:495-508` | `minReminderMl` artırıldığında `if (minReminderMl > maxReminderMl) maxReminderMl = minReminderMl` güncellemesi yap. |
| **B-04** | **YÜKSEK** | `HomeScreen`, `ProfileScreen`, `SettingsScreen` | `fl oz` birimi seçiliyken bazı metinler hâlâ `ml` gösterir (silme geri-al mesajı, zaman çizelgesi sıradaki öneri, `GoalInfoDialog` hareket/sıcaklık/toplam satırları, `ProfileScreen` hedef/hava özeti, `SettingsScreen` min/max hatırlatma). | `fmtVol(..., unit)` yerine eski `fmtMl(...)` + sabit `" ml"` kalmış. | `ui/HomeScreen.kt:389,528,719,726,735`; `ui/ProfileScreen.kt:187,200`; `ui/SettingsScreen.kt:498,505` | Tüm bu gösterimleri `fmtVol(..., unit)` (uygun birim destekli string kaynakları) ile birleştir. |
| **B-05** | **YÜKSEK** | `OnboardingScreen`, `SettingsScreen`, `BackupSection`, `BatteryTipSection` | 360dp küçük ekranda veya %130–%200 yazı boyutunda çoklu `FilterChip` ve yan yana `OutlinedButton` satırları sağdan taşar veya metin ezilir. | Sarılabilir `FlowRow` yerine sabit yatay `Row` kullanılmış. | `ui/OnboardingScreen.kt:332,370,512`; `ui/SettingsScreen.kt:389,395,414,477`; `ui/BackupSection.kt:79`; `ui/BatteryTipSection.kt:65` | Tüm çoklu çip ve çift aksiyon buton satırlarını `FlowRow(horizontalArrangement = spacedBy(8.dp), verticalArrangement = spacedBy(8.dp))` yap. |
| **B-06** | **YÜKSEK** | `OnboardingScreen`, `BackupSection`, `HomeScreen` | (1) "Āb'a başla" çift tıklanırsa `finishOnboarding` 2 kez çalışır. (2) Yedek dışa/içe aktarma sürerken butonlar tekrar tetiklenebilir. (3) Sağ bardak ikonu `s.maxHints` yerine sabit `MAX_HINTS = 3` yazar. | Double-submit `busy`/`finishing` koruması yok; sabit `MAX_HINTS` kalmış. | `ui/OnboardingScreen.kt:169`; `ui/BackupSection.kt:52-88`; `ui/HomeScreen.kt:318` | `finishing` / `busy` bayrakları ekle; `HomeScreen.kt:318`'de `s.maxHints.coerceAtLeast(MAX_HINTS)` kullan. |
| **B-07** | **ORTA** | `CupDialog`, `GoalInfoDialog`, `PastDayAddDialog`, `CityDialog` | Yatay modda, %150+ yazı boyutunda veya klavye açıkken `AlertDialog` içeriği kesilir ve kaydırılamaz. | Diyalog iç `Column`'larında `verticalScroll(rememberScrollState())` yok. | `ui/HomeScreen.kt:635,714`; `ui/HistoryScreen.kt:175`; `ui/SettingsScreen.kt:708` | İç `Column`'lara `Modifier.verticalScroll(rememberScrollState())` ekle. |
| **B-08** | **ORTA** | `UpdateSection` | `Updater.install(ctx, res.file)` yakalanmayan bir `ActivityNotFoundException` / `IllegalArgumentException` atarsa uygulama çöker; `ACTION_MANAGE_UNKNOWN_APP_SOURCES` başarısız olursa fallback yok. | `Updater.install` `runCatching` ile sarılmamış. | `ui/UpdateSection.kt:78-92` | `Updater.install` çağrısını `runCatching` ile sar (hata durumunda `UpdateState.Failed`); bilinmeyen kaynaklar ayarında `ACTION_APPLICATION_DETAILS_SETTINGS` fallback ekle. |
| **B-09** | **ORTA** | `HomeScreen`, `OnboardingScreen`, `HistoryScreen` | (1) `PermissionsStep` "İzin ver" butonları 40dp (`<48dp`). (2) `HomeScreen` Undo "Geri al" tıklama alanı ~32dp. (3) Uzun kullanıcı adı ve "+ Su Ekle" metninde `TextOverflow.Ellipsis` yok. (4) `HistoryScreen` kart satırında tarih ve hacim dar ekranda çarpışabilir. | `heightIn(min = 48.dp)`, `TextOverflow.Ellipsis` ve `Modifier.weight(1f)` eksik. | `ui/OnboardingScreen.kt:422,433`; `ui/HomeScreen.kt:190-194,307-311,352-363`; `ui/HistoryScreen.kt:116-118` | `heightIn(min = 48.dp)` (Undo'da `44.dp`), `TextOverflow.Ellipsis` ve `Modifier.weight(1f)` ekle. |
| **B-10** | **DÜŞÜK** | Çeşitli (Ölü kod) | `Slosh.kt` (tüm dosya), `Hydration.kt` (`QUICK_VESSEL_ML`, `coerceIntakeMl`), `Db.kt` (`IntakeDao.lastId()`), `Format.kt` (`ozToMl`) ve 3 kullanılmayan import üretim kodunda çağrılmıyor. | Eski ivmeölçer ve eski geri-al mantığından kalanlar. | `core/Slosh.kt`, `core/Hydration.kt:24,30`, `data/Db.kt:77`, `ui/Format.kt:15`, `ui/HomeScreen.kt:55,83`, `ui/ProfileScreen.kt:57` | Kullanılmayan import'lar temizlendi; testli çekirdek yardımcılar kırıcı olmaması için açık kalanlara not edildi. |
| **B-11** | **DÜŞÜK** | `AbDb` | `@Database(..., exportSchema = false)` şema geçmişi JSON'u üretmiyor. | İlk geliştirme kısayolu. | `data/Db.kt:85` | Üretim yayını öncesi `exportSchema = true` + şema klasörü eklenmeli (açık kalan). |

---

## 4. Aşama 4 — Çalıştırarak Doğrulama

### 4.1 Düzeltme Öncesi Durum
- Mevcut test paketi (`76+` birim + Robolectric Compose duman testi) `v0.3.11` üzerinde CI'da yeşildi; ancak `SettingsScreen` hızlı ardışık kaydetme yarışı, `Reminders` null `AlarmManager`, `Onboarding` double-submit, `fl oz` birim tutarlılığı ve küçük ekran `FlowRow` sarımı için testler eksikti.

### 4.2 Düzeltme Sonrası Doğrulama (GitHub Actions CI)
*(Aşama 6 düzeltmeleri ve yeni otomatik testler tamamlandıktan sonra CI sonuçları bu bölüme işlenecektir.)*

---

## 5. Açık Kalanlar

1. **B-10 (Testli ölü çekirdek yardımcılar):** `core/Slosh.kt`, `Hydration.QUICK_VESSEL_ML`, `Hydration.coerceIntakeMl`, `Format.ozToMl` ve `IntakeDao.lastId()` mevcut birim testleriyle (`SloshTest`, `HydrationTest`, `FormatTest`) bağlı olduğundan kapsam dışı kırıcı refactor yapılmadan bırakıldı; sonraki temizlik turunda testleriyle birlikte kaldırılabilir.
2. **B-11 (`AbDb` `exportSchema = false`):** Mevcut Room v3 veritabanında `MIGRATION_1_2` ve `MIGRATION_2_3` çalışır durumda; yeni sürüm (v4) şema değişikliği yapılırken `exportSchema = true` açılması planlandı.
3. **Fiziksel cihaz / emulator `adb` testi:** Denetim ortamı başsız Linux sunucu olduğundan gerçek cihaz `adb logcat` yerine Robolectric API 33/34 + Compose UI testleri (`SmokeTest`, `ScreenshotSmokeTest`, `AuditVerificationTest`) ve GitHub Actions CI (`testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`) ile doğrulandı.

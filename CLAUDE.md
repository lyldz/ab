# ab — proje notları

Android su takibi uygulaması (Kotlin, Jetpack Compose, Material 3). Offline ve takipsiz. Tasarım ve kararlar: `docs/SPEC.md`, `docs/DECISIONS.md`.

## Komutlar
- Derle: `ANDROID_HOME=<sdk yolu> ./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.jvmargs=-Xmx1g` (az RAM'li makinede aynı anda tek gradle çalıştır)
- Test: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`

## Harita
- `app/src/main/java/com/lyldz/ab/` — uygulama kodu (`ui/theme` tema)
- `app/src/main/res/values` (en), `values-tr` (tr) — tüm metinler
- `fastlane/metadata/android/` — mağaza metinleri ve sürüm notları
- `.github/workflows/` — CI ve release

## Kurallar
- Google Play Services, Firebase, reklam, analytics, hesap yok.
- Kodda sabit metin yok; her metin `strings.xml`'de (en + tr).
- Türkçe büyük/küçük harf dönüşümünde `Locale("tr")` kullan.
- Hedef formülü ve hatırlatma kuralları `docs/SPEC.md`'dedir; oradan sapma, önce `docs/DECISIONS.md`'ye yazılır.
- `SCHEDULE_EXACT_ALARM` yalnızca opsiyonel; `USE_EXACT_ALARM` kullanılmaz.
- Commit: Conventional Commits. Anahtar/keystore asla commit edilmez.

## Bitti tanımı
Testler geçer, lint temiz, CHANGELOG güncel, `fastlane/.../changelogs/<versionCode>.txt` eklendi.

## Sürüm
SemVer, `app/build.gradle.kts` içindeki `appVersion`. `versionCode = MAJOR*10000 + MINOR*100 + PATCH`. Sürümler biriktirilerek çıkarılır.

<p align="center"><img src="docs/design/icon/svg/ab-logo.svg" alt="ab" height="96"></p>

# ab

**ab** Farsça "su" demek. Offline çalışan, takipsiz, sade bir Android su takibi uygulaması.

Sabit saatli, bunaltıcı bildirimler yerine günün belirlediğin saat aralığı ve hedef su miktarından kaç dakikada bir ne kadar içmen gerektiğini hesaplar. Su içtikçe plan yenilenir.

> Durum: geliştirme aşamasında (v0.1.0, boş iskelet). Tasarım: [docs/SPEC.md](docs/SPEC.md)

## Özellikler (planlanan)
- Dakikaya hesaplanan akıllı hatırlatma (rutin penceresi içinde)
- Bildirimden ve ana ekran widget'ından tek dokunuşla kayıt
- Günlük hedef: kilo + hareket seviyesi + sabah bir kez alınan şehir sıcaklığı (hepsi kapatılabilir)
- Ana ekranda "şu saate kadar şu kadar ml" planı ve ilerleme çubuğu

## Gizlilik
Reklam, analytics, hesap ve Google Play Services yok. İnternet yalnızca günde bir kez, seçtiğin şehrin sıcaklığını almak için kullanılır. Konum izni isteğe bağlıdır.

## Derleme
Gereksinimler: JDK 17, Android SDK (platform 34).

```
ANDROID_HOME=<sdk yolu> ./gradlew :app:assembleDebug
```

## Lisans
GPL-3.0-or-later. Bkz. [LICENSE](LICENSE).

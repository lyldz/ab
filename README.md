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
Reklam, analytics, hesap ve Google Play Services yok. İnternet yalnızca günde bir kez bugünün sıcaklığını almak (yaklaşık, ~11 km'ye yuvarlanmış koordinat) ve şehir aramak (yazdığın şehir adı) için [Open-Meteo](https://open-meteo.com/)'ya gider; başka hiçbir veri gönderilmez. Konum izni isteğe bağlıdır, şehri elle de seçebilirsin. Ayarlar'daki "Güncellemeyi denetle" yalnızca sen dokununca bu deponun GitHub sürümlerine bakar ve istersen yeni APK'yı indirir; hiçbir kişisel veri gönderilmez.

## Derleme
Gereksinimler: JDK 17, Android SDK (platform 34).

```
ANDROID_HOME=<sdk yolu> ./gradlew :app:assembleDebug
```

## Lisans
GPL-3.0-or-later. Bkz. [LICENSE](LICENSE).

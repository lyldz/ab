# Ab — İkon Paketi (Claude Code için)

- **Uygulama ikonu / logo:** Damla şeklinde "A" (çizgisi dalga) + "b"
- **Bildirim ikonu:** İçinde su olan tek "b", siyah-beyaz

Bu klasörü projeye `docs/design/icon/` olarak kopyala, sonra Claude Code'a şunu söyle:

> `docs/design/icon/README.md` dosyasındaki talimatlara göre uygulama ikonunu ve bildirim ikonunu projeye ekle.

---

## Claude Code'a talimat

1. **Android kaynaklarını kopyala.** `android/res/` altındakileri aynı yapıyla `app/src/main/res/` altına koy:
   - `drawable/ic_launcher_foreground.xml`
   - `drawable/ic_launcher_monochrome.xml`
   - `drawable/ic_stat_ab.xml`
   - `values/ic_launcher_background.xml`
   - `mipmap-anydpi-v26/ic_launcher.xml`
   - `mipmap-anydpi-v26/ic_launcher_round.xml`
2. **Eski ikonları sil.** Şablondan kalan `mipmap-*/ic_launcher*.webp|png` dosyalarını ve eski ikon XML'lerini kaldır. minSdk 26 olduğu için adaptive icon yeterli.
3. **Manifest'i kontrol et.**
   ```xml
   android:icon="@mipmap/ic_launcher"
   android:roundIcon="@mipmap/ic_launcher_round"
   ```
4. **Bildirim ikonunu bağla.** Bütün bildirimlerde:
   ```kotlin
   NotificationCompat.Builder(context, CHANNEL_ID)
       .setSmallIcon(R.drawable.ic_stat_ab)
   ```
   - **`setColor()` çağırma.** Bildirim ikonu siyah-beyaz kalmalı.
   - `ic_launcher`'ı asla `setSmallIcon` olarak kullanma (durum çubuğunda beyaz kare çıkar).
5. **Diğer yerler.** Widget (F-20) ve Hızlı Ayarlar kutucuğu (F-21) için de `ic_stat_ab` kullan.
6. **Mağaza ikonu.** `store/ab-icon-512.png` dosyasını şu iki yere kopyala:
   - `fastlane/metadata/android/tr-TR/images/icon.png`
   - `fastlane/metadata/android/en-US/images/icon.png`
7. **README logosu.** Proje README'sinde `docs/design/icon/svg/ab-logo.svg` kullan.
8. **Test ve commit.** Derle, cihazda şunları kontrol et:
   - ana ekran ikonu
   - temalı ikon (Android 13+)
   - bir test bildirimi

   Sonra commit at: `feat: add Ab app and notification icons`

---

## Dosyalar

| Dosya | Açıklama |
|---|---|
| `android/res/...` | Doğrudan kullanılacak vector drawable'lar |
| `svg/ab-app-icon.svg` | Uygulama ikonu, tam kare kaynak |
| `svg/ab-logo.svg` | Logo, açık zemin (lacivert + mavi dalga) |
| `svg/ab-logo-white.svg` | Logo, koyu zemin (beyaz + açık mavi dalga) |
| `svg/ab-logo-mono.svg` | Logo, tek renk siyah |
| `svg/ab-notification.svg` | Bildirim ikonu kaynağı (beyaz, tek b) |
| `store/ab-icon-512.png` | Play Store / F-Droid ikonu |
| `preview.png` | Önizleme |

## Teknik notlar

- **Renkler:**
  - zemin `#0A2A43`
  - harfler `#FFFFFF`
  - dalga `#4FC3F7` (açık zeminde `#1E88E5`)
- **Ölçek:** Ön plan 0.86 oranında küçültüldü; 66dp güvenli alanda kalıyor, hiçbir maskede kesilmiyor.
- **Bildirim ikonu:** 24dp, şeffaf zemin üzerinde beyaz silüet. Sistem tint uyguladığı için renk eklenmemeli.
- **Temalı ikon:** `monochrome` katmanı tek renk; Android 13+ duvar kağıdına göre boyar.

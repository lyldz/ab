# Değişiklik günlüğü

[Keep a Changelog](https://keepachangelog.com/tr/1.1.0/) biçimi, [SemVer](https://semver.org/lang/tr/) sürümleme.

## [Unreleased]
### Added
- Günlük hedef hesabı (25 ml/kg + hareket + sıcaklık) ve dakikaya hesaplanan hatırlatma planı, birim testleriyle.
- Veri katmanı: Room (içme kayıtları), DataStore (ayarlar) ve ortak AbRepository.
- Onboarding (kilo, hareket, rutin saatleri, bildirim izni) ve ana ekran (plan kartı, ilerleme çubuğu, hızlı ekleme, özel miktar, geri al).
- Hatırlatma alarmı ve bildirim: bardak, 500 ml ve satır içi özel miktar düğmeleri; yeniden başlatma ve saat değişiminde alarm yeniden kurulur.
- Logo ve ikon paketi (uygulama, temalı ve bildirim ikonu); onboarding'de bildirim izni ayrı adımda istenir.
- Ana ekran widget'ı (Glance): ilerleme, +bardak/+500/+1000 ve özel miktar için uygulamayı açan düğme.
- Mavi tema (ikon paletinden), "Bugünün kayıtları" listesi: kaynak (uygulama/bildirim/widget), düzenleme, silme ve silmeyi geri alma.
- Ayarlar ekranı: kilo, hareket, rutin saatleri, bardak boyutu, hareket/hava eklerini kapatma, elle hedef ve 5 L uyarısı.

## [0.1.0]
### Added
- Boş Compose iskeleti, marka paleti ve ikon, tr/en metinler.

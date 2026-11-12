# Kararlar

| Tarih | Karar | Neden |
|---|---|---|
| 2026-11-12 | Uygulama adı `ab`, paket `com.lyldz.ab` | Kullanıcı seçti (Farsça "su") |
| 2026-11-12 | compile/target SDK 34, AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1 | Sunucudaki SDK yalnızca android-34. targetSdk 36'ya Play öncesi geçilir |
| 2026-11-12 | Faz 0'da Hilt, Room, DataStore, ktlint, detekt yok | Faz 0 boş iskelet; 2 GB RAM'li sunucuda derleme maliyeti. Faz 1'de eklenecek |
| 2026-11-12 | Hatırlatma aralığında alt sınır yok | Kullanıcı sabit 45 dk istemiyor |
| 2026-11-12 | Hedef = max(1500, 25 ml/kg) + hareket + sıcaklık | 33 ml/kg yüksek bulundu; EFSA/IOM değerleri ve 30–35 ml/kg kaba kural incelendi |
| 2026-11-12 | Widget Faz 1'de | Kullanıcı için olmazsa olmaz |
| 2026-11-12 | Hava servisi Open-Meteo (öneri) | Hesapsız, anahtarsız. Kullanıcı yalnızca "sadece sıcaklık çekilsin" dedi |
| 2026-11-12 | Hilt kullanılmıyor; bağımlılıklar Application üzerinden elle | Uygulama küçük; KSP/Hilt derleme maliyeti ve karmaşıklık. Room için KSP yine gerekir |
| 2026-11-12 | Hatırlatmada her blokta içilecek miktar bardak boyutudur (varsayılan 200 ml), aralık buna göre hesaplanır | "Bir sonraki bildirime kadar olan miktar" + "aralığı dakikaya böl" birlikte bunu verir; SPEC'teki "10:45'e kadar 600 ml" örneği daha büyük blok ima eder, kullanıcıya soruldu |
| 2026-11-12 | Sürümler 0.1.x serisiyle ilerler (şimdiki 0.1.0, versionCode 100) | Kullanıcı önce 0.0.x dedi, sonra 0.1.x olsun dedi. fastlane changelog dosyası 100.txt |

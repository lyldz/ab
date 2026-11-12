# Mimari

Şimdilik tek `app` modülü, tek `MainActivity` ve Compose arayüz. Faz 1'de eklenecekler:

- **Veri:** Room (içme kayıtları), DataStore (ayarlar). Uygulama, widget ve bildirim aksiyonları aynı tek repository'yi kullanır.
- **Mantık:** Hedef hesabı ve hatırlatma aralığı saf Kotlin fonksiyonlarıdır, birim testle doğrulanır (`docs/SPEC.md` formülleri).
- **Zamanlama:** `AlarmManager` ile inexact (`setAndAllowWhileIdle`/`setWindow`). Yeniden başlatma, saat ve saat dilimi değişiminde alarmlar yeniden kurulur.
- **Bildirim:** Aksiyon düğmeleri `BroadcastReceiver`'a gider, uygulama açılmadan kayıt yapılır, sonra bildirim ve widget güncellenir.
- **Widget:** Glance.
- **Hava:** günde bir kez sıcaklık isteği (WorkManager veya alarm), yalnızca sıcaklık okunur.

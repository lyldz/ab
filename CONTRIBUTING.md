# Katkı

- Commit mesajları [Conventional Commits](https://www.conventionalcommits.org/) biçiminde: `feat:`, `fix:`, `chore:`, `docs:`.
- `main` her zaman yayınlanabilir kalır. Kısa ömürlü `feat/*` ve `fix/*` dalları squash merge ile birleştirilir.
- Metinler `strings.xml` içinde olur (`values/` en, `values-tr/` tr). Kodda sabit metin yok.
- Google Play Services, Firebase, reklam ve analytics eklenmez.
- PR öncesi: `./gradlew testDebugUnitTest lintDebug assembleDebug` geçmeli, CHANGELOG güncel olmalı.

# Makay Launcher sürümleri

Uygulama bu depodaki `latest.json` dosyasını okuyarak güncelleme denetler.

- Manifest: https://raw.githubusercontent.com/makaydestek/Makay-Lanucher/main/latest.json
- APK: GitHub Releases (`vX.Y.Z` etiketi)

## Yeni sürüm (diğer uygulamalarla aynı)

1. `Apk Hazırla.bat` çalıştırın — APK ve `.apk.sha256` `APK\` klasörüne yazılır.
2. GitHub'da **Draft a new release** (veya mevcut sürümü düzenle).
3. **Tag** alanına `v1.0.9` gibi sürümü yazın.
4. Yalnızca `MakayLauncher-1.0.9.apk` dosyasını ekleyin ve yayınlayın.

GitHub Action otomatik olarak `.apk.sha256` ekler ve `latest.json` dosyasını günceller.

İsteğe bağlı yerel yol: `GitHub-Release.bat` (tag + APK + sha256 birlikte yükler).

`latest.json` örneği:

```json
{
  "versionName": "1.0.8",
  "versionCode": 9,
  "apkUrl": "https://github.com/makaydestek/Makay-Lanucher/releases/download/v1.0.8/MakayLauncher-1.0.8.apk",
  "notes": "MakayLauncher-1.0.8.apk"
}
```

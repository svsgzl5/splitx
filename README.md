# SplitX — Bölünmüş Ekran Uygulaması
## Redmi 14C için özel yapıldı

---

## APK Nasıl Yapılır? (Ücretsiz, PC gerekmez)

### YÖNTEM 1 — Buildozer Online (Önerilen)
1. https://appetize.io veya https://replit.com aç
2. Bu klasörü ZIP olarak yükle
3. "Run" → APK çıkar

### YÖNTEM 2 — GitHub Actions (Ücretsiz, otomatik)
1. https://github.com'a giriş yap
2. Yeni repo oluştur → bu dosyaları yükle
3. `.github/workflows/build.yml` dosyası ekle (aşağıda)
4. Actions sekmesinden APK indir

#### build.yml içeriği:
```yaml
name: Build APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build APK
        run: |
          chmod +x gradlew
          ./gradlew assembleRelease
      - uses: actions/upload-artifact@v3
        with:
          name: SplitX-APK
          path: app/build/outputs/apk/release/*.apk
```

### YÖNTEM 3 — Android Studio (PC)
1. Android Studio indir: https://developer.android.com/studio
2. Bu klasörü aç
3. Build → Generate Signed APK
4. Telefona aktar, "Bilinmeyen kaynaklardan yükle" aç

---

## Özellikler
- ✅ İki panel — her biri bağımsız WebView
- ✅ ⊞ butonuyla hazır uygulama listesi (ChatGPT, YouTube, Instagram, TikTok...)
- ✅ Bölünmüş çizgiyi parmakla sürükleyerek boyut ayarla
- ✅ Herhangi bir URL yazabilirsin
- ✅ Arama kutusu — URL değilse Google'da arar
- ✅ Geri tuşu WebView'de geri gider
- ✅ Kamera ve mikrofon izni (web uygulamaları için)

## Desteklenen Siteler
WebView native tarayıcı gibi davrandığından, iframe engeli YOK.
ChatGPT, YouTube, Instagram, TikTok, WhatsApp Web hepsi çalışır.

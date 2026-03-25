# GraphCite - Makale Graf Analiz Sistemi

GraphCite, akademik makaleler arasındaki atıf (citation) ağlarını, modern algoritmalar ve interaktif görselleştirme yöntemleriyle incelemenizi sağlayan, Java tabanlı bir masaüstü uygulamasıdır.

## 🚀 Özellikler

- **Bütünleşik Veri Okuma:** Özel JSON parser ile binlerce satırlık makale bilgisi (`data.json`) anında okunur ve modellere dönüştürülür.
- **Dinamik Atıf Ağı (Graf) Görselleştirme:** Ağır masaüstü bileşenleri yerine D3.js alt yapısı kullanılarak pürüzsüz ve etkileşimli bir graf deneyimi (JavaFX WebView) sunulur.
- **Gelişmiş Metrik Algoritmaları:**
  - **Atıf Sayısı (In-Degree):** Makalenin ne kadar refere edildiği.
  - **H-Index Hesaplaması:** Atıfta bulunan makalelerin niteliğine bağlı etki faktörü hesaplaması.
  - **H-Core:** Bu index'i oluşturan ana (çekirdek) makalelerin tespiti.
  - **H-Median:** H-Core makalelerinin atıf sayılarının ortanca değeri.
- **Canlı Etkileşim:** Herhangi bir spesifik makaleye tıklandığında dinamik olarak H-Core alt grafı genişler; yan panelde hızlı ve detaylı istatistik/log akışı izlenir.

## 🛠 Kullanılan Teknolojiler

- **Dil:** Java 17
- **Proje Yönetimi:** Maven
- **Grafik Arayüz:** Java Swing & JavaFX WebView Hibrit Mimarisi
- **Görselleştirme:** HTML5 Canvas, D3.js (Veri Güdümlü Belgeler)
- **Loglama:** SLF4J & Logback

## 📂 Proje Yapısı

```text
src/
 ├── main/
 │   ├── java/com/kocaeli/graphcite/
 │   │   ├── graph/       # H-Index, H-Core, In-Degree matematiksel algoritmaları
 │   │   ├── model/       # Makale veri modülleri
 │   │   ├── parser/      # Regex tabanlı JSON parser sistemi
 │   │   └── ui/          # Swing Arayüz panelleri ve WebView bileşenleri
 │   └── resources/web/   # D3.js graf görselleştirme dosyaları (HTML/JS)
```

## ⚙️ Kurulum ve Çalıştırma

### Gereksinimler

- **JDK 17** veya üstü bir sürüm kurulumu olmalıdır.
- **Apache Maven** bilgisayarınızda mevcut olmalıdır.

### Adımlar

1. Projeyi bir terminal penceresinde veya Favori IDE'nizde (IntelliJ IDEA, VS Code, Eclipse vb.) açın.
2. Maven ile bağımlılıkları indirin ve projeyi derleyin:
   ```bash
   mvn clean install
   ```
3. Uygulamayı JavaFX Plugin'i üzerinden başlatın:
   ```bash
   mvn javafx:run
   ```
   _(Eğer manuel çalıştırmak isterseniz `MainApp.java` veya `Main.java` sınıflarından başlatabilirsiniz)._

## 📖 Kullanım Kılavuzu Yaklaşımı

1. Program açıldığında haritalanmış bir veri graf ağıyla karşılaşacaksınız.
2. Fare yardımıyla graf görünümünü yakınlaştırıp uzaklaştırabilir (Zoom-in/Out) veya düğümleri sürükleyebilirsiniz.
3. Herhangi bir **Node (Makale)**'a tıkladığınızda:
   - Sağ taraftaki _Log ve İstatistik panelinde_ makalenin H-Index, H-Median değerlerini göreceksiniz.
   - Seçili makalenin H-Core kümesinde olan diğer makaleler grafikte görsel olarak vurgulanacak ve ağaç genişleyecektir.

---

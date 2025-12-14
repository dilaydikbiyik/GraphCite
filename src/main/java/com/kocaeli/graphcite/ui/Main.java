package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.graph.GraphManager;
import com.kocaeli.graphcite.model.Makale;
import com.kocaeli.graphcite.parser.JsonParser;
import org.graphstream.graph.Graph;
import org.graphstream.ui.view.Viewer;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Mac ve Windows uyumluluğu için UI ayarı
        System.setProperty("org.graphstream.ui", "swing");

        try {
            System.out.println("⏳ Veriler yükleniyor...");
            List<Makale> tumMakaleler = JsonParser.parse("data.json");

            // Eğer test hızlı olsun istersen burayı .subList(0, 100) yapabilirsin.
            // Ama gerçek sonuç ("Boss Savaşı") için hepsini (tumMakaleler) kullanıyoruz.
            List<Makale> makaleler = tumMakaleler;

            System.out.println("✅ " + makaleler.size() + " makale işleme alındı.");

            // --- 1. GÖRSELLEŞTİRME KISMI ---
            System.out.println("🔨 Graf penceresi açılıyor...");
            GraphManager manager = new GraphManager(makaleler);
            Graph graph = manager.createGraph();
            Viewer viewer = graph.display();
            viewer.enableAutoLayout(); // Düğümlerin yayılması için

            // --- 2. ALGORİTMA ANALİZ KISMI ---
            System.out.println("🧮 Algoritmalar hazırlanıyor...");
            GraphAlgorithms algo = new GraphAlgorithms(makaleler);

            // A) H-INDEX VE H-CORE TESTİ
            if (!makaleler.isEmpty()) {
                String testId = makaleler.get(0).getId(); // İlk makaleyi test edelim
                System.out.println("\n--- H-INDEX ANALİZİ: " + testId + " ---");

                Makale hedef = algo.getMakale(testId);
                if (hedef != null) {
                    System.out.println("Makale Başlığı: " + hedef.getTitle());
                    System.out.println("Toplam Atıf Sayısı: " + hedef.getCitationCount());

                    int hIndex = algo.calculateHIndex(testId);
                    System.out.println("🔥 H-INDEX: " + hIndex);

                    List<Makale> hCore = algo.getHCore(testId);
                    System.out.println("💎 H-CORE Listesi (" + hCore.size() + " makale):");
                    for (Makale m : hCore) {
                        System.out.println("   -> [" + m.getCitationCount() + " atıf] " + m.getId());
                    }
                }
            }

            // B) BETWEENNESS CENTRALITY (BOSS SAVAŞI)
            System.out.println("\n🚀 BETWEENNESS CENTRALITY Hesaplanıyor... (Biraz sürebilir)");
            long startTime = System.currentTimeMillis();

            // Hesabı başlat
            Map<String, Double> scores = algo.calculateBetweennessCentrality();

            long endTime = System.currentTimeMillis();
            System.out.println("✅ Hesaplama Bitti! Süre: " + (endTime - startTime) + " ms");

            // En yüksek skorlu ilk 5 makaleyi bulup yazdıralım
            System.out.println("\n🏆 EN MERKEZİ 5 MAKALE (Betweenness Centrality):");
            scores.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Büyükten küçüğe sırala
                    .limit(5) // Sadece ilk 5'i al
                    .forEach(entry -> {
                        System.out.println("ID: " + entry.getKey() + " | Skor: " + entry.getValue());
                    });

            // C) K-CORE DECOMPOSITION TESTİ
            System.out.println("\n🧅 K-CORE DECOMPOSITION Testi (k=2)...");
            long kStartTime = System.currentTimeMillis();

            // k=2 için çalıştır
            List<Makale> kCoreList = algo.runKCoreDecomposition(2);

            long kEndTime = System.currentTimeMillis();
            System.out.println("✅ K-Core Bitti! Süre: " + (kEndTime - kStartTime) + " ms");

            System.out.println("📊 Başlangıç Makale Sayısı: " + makaleler.size());
            System.out.println("📉 K-Core (k=2) Sonrası Kalan: " + kCoreList.size());

            if (!kCoreList.isEmpty()) {
                System.out.println("   Örnek Kalan ID: " + kCoreList.get(0).getId());
            } else {
                System.out.println("⚠️ Uyarı: Hiçbir makale K=2 şartını sağlayamadı (Graf çok seyrek olabilir).");
            }

        } catch (Exception e) {
            System.err.println("❌ Bir hata oluştu:");
            e.printStackTrace();
        }
    }
}
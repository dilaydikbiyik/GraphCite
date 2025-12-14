package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.model.Makale;
import com.kocaeli.graphcite.parser.JsonParser;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("⏳ Veriler yükleniyor...");
            List<Makale> makaleler = JsonParser.parse("data.json");

            System.out.println("🧮 Algoritmalar hazırlanıyor...");
            GraphAlgorithms algo = new GraphAlgorithms(makaleler);

            // TEST: Rastgele bir makale seçelim (veya atıfı bol olan birini)
            // Örnek ID (Listedeki ilk makaleyi alalım)
            String testId = makaleler.get(0).getId();
            // Veya elle bildiğin bir ID yaz: String testId = "https://openalex.org/W2002615855";

            System.out.println("\n--- ANALİZ BAŞLIYOR: " + testId + " ---");

            Makale hedef = algo.getMakale(testId);
            if(hedef != null) {
                System.out.println("Makale Başlığı: " + hedef.getTitle());
                System.out.println("Toplam Atıf Sayısı (Citation Count): " + hedef.getCitationCount());

                // H-INDEX HESAPLA
                int hIndex = algo.calculateHIndex(testId);
                System.out.println("🔥 H-INDEX: " + hIndex);

                // H-CORE LİSTELE
                List<Makale> hCore = algo.getHCore(testId);
                System.out.println("💎 H-CORE Listesi (" + hCore.size() + " makale):");
                for(Makale m : hCore) {
                    System.out.println("   -> [" + m.getCitationCount() + " atıf] " + m.getId());
                }
            } else {
                System.out.println("Makale bulunamadı!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
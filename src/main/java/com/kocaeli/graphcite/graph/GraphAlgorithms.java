package com.kocaeli.graphcite.graph;

import com.kocaeli.graphcite.model.Makale;
import java.util.*;

public class GraphAlgorithms {
    private List<Makale> makaleler;
    private Map<String, Makale> makaleMap; // ID ile hızlı arama yapmak için

    public GraphAlgorithms(List<Makale> makaleler) {
        this.makaleler = makaleler;
        this.makaleMap = new HashMap<>();

        // 1. Hızlı erişim için Map'i doldur
        for (Makale m : makaleler) {
            makaleMap.put(m.getId(), m);
        }

        // 2. Herkesin atıf sayısını (Citation Count) baştan hesapla
        calculateAllCitationCounts();
    }

    /**
     * Tüm makalelerin kaç kez referans ALDIĞINI (In-Degree) hesaplar.
     */
    private void calculateAllCitationCounts() {
        // Önce sayaçları sıfırla
        for (Makale m : makaleler) {
            m.setCitationCount(0);
        }

        // Referansları gez ve puan dağıt
        for (Makale m : makaleler) { // m, başkalarına referans veriyor
            for (String refId : m.getReferencedWorkIds()) {
                Makale atifAlan = makaleMap.get(refId);
                if (atifAlan != null) {
                    atifAlan.incrementCitationCount();
                }
            }
        }
    }

    /**
     * Verilen makale ID'si için H-Index hesaplar.
     * Tanım: En az h atıfa sahip h makale sayısı.
     */
    public int calculateHIndex(String targetId) {
        // 1. Hedef makaleye atıf yapanları (Referans Verenleri) bul
        List<Integer> citationCountsOfCiters = new ArrayList<>();

        // Tüm makaleleri gez, eğer bizim hedef makaleye referans verdiyse listeye al
        for (Makale m : makaleler) {
            if (m.getReferencedWorkIds().contains(targetId)) {
                // Atıf yapan bu makalenin kendi atıf sayısını listeye ekle
                citationCountsOfCiters.add(m.getCitationCount());
            }
        }

        // 2. H-Index Hesabı: Listeyi büyükten küçüğe sırala
        Collections.sort(citationCountsOfCiters, Collections.reverseOrder());

        // 3. h değerini bul
        int hIndex = 0;
        for (int i = 0; i < citationCountsOfCiters.size(); i++) {
            // i+1 (sıra sayısı) <= atıf sayısı ise h-index artar
            if (citationCountsOfCiters.get(i) >= (i + 1)) {
                hIndex = i + 1;
            } else {
                break;
            }
        }

        return hIndex;
    }

    /**
     * H-Core kümesini (Makale ID listesi olarak) döndürür.
     */
    public List<Makale> getHCore(String targetId) {
        List<Makale> hCoreList = new ArrayList<>();
        int hIndex = calculateHIndex(targetId);

        // Atıf yapanları bul
        List<Makale> citers = new ArrayList<>();
        for (Makale m : makaleler) {
            if (m.getReferencedWorkIds().contains(targetId)) {
                citers.add(m);
            }
        }

        // Büyükten küçüğe sırala
        citers.sort((m1, m2) -> Integer.compare(m2.getCitationCount(), m1.getCitationCount()));

        // İlk 'hIndex' kadar makaleyi al
        for (int i = 0; i < hIndex && i < citers.size(); i++) {
            hCoreList.add(citers.get(i));
        }

        return hCoreList;
    }

    public Makale getMakale(String id) {
        return makaleMap.get(id);
    }
}
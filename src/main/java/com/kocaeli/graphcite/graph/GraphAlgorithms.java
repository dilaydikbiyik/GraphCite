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

    // --- BÖLÜM 2: BETWEENNESS CENTRALITY ---

    /**
     * Proje isterlerine göre:
     * 1. Grafı yönsüz (undirected) hale getirir.
     * 2. Her düğüm için Betweenness Centrality skorunu hesaplar.
     * * Formül Mantığı: s ve t arasındaki en kısa yol v düğümünden geçiyor mu?
     * Kontrol: dist(s, v) + dist(v, t) == dist(s, t) ise v yol üzerindedir.
     */
    public Map<String, Double> calculateBetweennessCentrality() {
        // 1. Yönsüz Graf Yapısını Kur (Komşuluk Listesi)
        Map<String, List<String>> adjList = new HashMap<>();
        for (Makale m : makaleler) {
            adjList.putIfAbsent(m.getId(), new ArrayList<>());
            for (String refId : m.getReferencedWorkIds()) {
                // Eğer referans verilen makale bizim listemizde varsa bağla
                if (makaleMap.containsKey(refId)) {
                    // YÖNSÜZ DEMEK: A -> B ise, hem A-B hem B-A var demektir.
                    adjList.putIfAbsent(refId, new ArrayList<>());

                    // Çift taraflı ekle (Duplicate kontrolü yaparak)
                    if (!adjList.get(m.getId()).contains(refId)) adjList.get(m.getId()).add(refId);
                    if (!adjList.get(refId).contains(m.getId())) adjList.get(refId).add(m.getId());
                }
            }
        }

        // 2. Tüm Çiftler Arası Mesafeleri Hesapla (All-Pairs Shortest Path)
        // Floyd-Warshall (N^3) yerine, 1000 düğüm için N kere BFS (N*(V+E)) daha hızlıdır.
        Map<String, Map<String, Integer>> allDistances = new HashMap<>();
        List<String> nodeIds = new ArrayList<>(makaleMap.keySet());

        for (String id : nodeIds) {
            allDistances.put(id, runBFS(id, adjList));
        }

        // 3. Skorları Hesapla
        Map<String, Double> centralityScores = new HashMap<>();
        for (String id : nodeIds) centralityScores.put(id, 0.0);

        // Tüm ikilileri (s, t) gez
        for (int i = 0; i < nodeIds.size(); i++) {
            for (int j = i + 1; j < nodeIds.size(); j++) {
                String s = nodeIds.get(i);
                String t = nodeIds.get(j);

                // s ve t arasında bir yol var mı?
                if (allDistances.get(s).containsKey(t)) {
                    int dist_ST = allDistances.get(s).get(t);

                    // Aradaki tüm düğümleri (v) kontrol et
                    for (String v : nodeIds) {
                        // v düğümü s veya t'nin kendisi olmamalı
                        if (v.equals(s) || v.equals(t)) continue;

                        // s -> v ve v -> t mesafelerini kontrol et
                        if (allDistances.get(s).containsKey(v) && allDistances.get(v).containsKey(t)) {
                            int dist_SV = allDistances.get(s).get(v);
                            int dist_VT = allDistances.get(v).get(t);

                            // KRİTİK FORMÜL: Eğer s-v + v-t == s-t ise, v en kısa yol üzerindedir.
                            if (dist_SV + dist_VT == dist_ST) {
                                centralityScores.put(v, centralityScores.get(v) + 1.0);
                            }
                        }
                    }
                }
            }
        }

        return centralityScores;
    }

    /**
     * YARDIMCI METOD: BFS Algoritması
     * Bir başlangıç düğümünden diğer herkese olan en kısa mesafeyi bulur.
     */
    private Map<String, Integer> runBFS(String startNode, Map<String, List<String>> adjList) {
        Map<String, Integer> distances = new HashMap<>();
        Queue<String> queue = new LinkedList<>();

        distances.put(startNode, 0);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDist = distances.get(current);

            if (adjList.containsKey(current)) {
                for (String neighbor : adjList.get(current)) {
                    if (!distances.containsKey(neighbor)) {
                        distances.put(neighbor, currentDist + 1);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return distances;
    }

    // --- BÖLÜM 3: K-CORE DECOMPOSITION ---

    /**
     * K-Core Algoritması:
     * Grafı "soyar". Derecesi (degree) k'dan küçük olan düğümleri siler.
     * Silinen düğümlerin komşularının derecesi düşeceği için, onlar da silinebilir.
     * Bu işlem zincirleme devam eder.
     *
     * @param k İstenen minimum derece (K değeri)
     * @return K-Core şartını sağlayan Makalelerin Listesi
     */
    public List<Makale> runKCoreDecomposition(int k) {
        // 1. Orijinal veriyi bozmamak için geçici bir derece haritası yapalım
        Map<String, Integer> currentDegrees = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>(); // Kim kime bağlı (Yönsüz/Çift yönlü kabul edelim)

        // Graf yapısını kur (Betweenness'taki gibi yönsüz düşünüyoruz genelde K-Core için)
        for (Makale m : makaleler) {
            adjList.putIfAbsent(m.getId(), new ArrayList<>());
            // Atıf alanlar ve verenler (Toplam Derece / Total Degree)
            // Not: Proje isterine göre sadece In-Degree veya Out-Degree istenirse burası değişir.
            // Genelde K-Core "bağlantı sayısı" üzerinden yapılır.

            // Referans verdiklerini ekle
            for (String refId : m.getReferencedWorkIds()) {
                if (makaleMap.containsKey(refId)) {
                    adjList.get(m.getId()).add(refId);

                    adjList.putIfAbsent(refId, new ArrayList<>());
                    adjList.get(refId).add(m.getId());
                }
            }
        }

        // Başlangıç derecelerini hesapla
        for (String id : makaleMap.keySet()) {
            int degree = adjList.containsKey(id) ? adjList.get(id).size() : 0;
            currentDegrees.put(id, degree);
        }

        // 2. SOĞAN KABUĞU ALGORİTMASI (Pruning)
        boolean changed = true;
        Set<String> removedNodes = new HashSet<>();

        while (changed) {
            changed = false;
            // Şuan aktif olan (silinmemiş) düğümleri gez
            for (String id : makaleMap.keySet()) {
                if (removedNodes.contains(id)) continue;

                // Eğer derecesi K'dan küçükse sil
                if (currentDegrees.get(id) < k) {
                    removedNodes.add(id);
                    changed = true;

                    // Bu düğüm silindiği için, komşularının derecesini 1 düşür
                    if (adjList.containsKey(id)) {
                        for (String neighbor : adjList.get(id)) {
                            if (!removedNodes.contains(neighbor)) {
                                int oldDegree = currentDegrees.get(neighbor);
                                currentDegrees.put(neighbor, oldDegree - 1);
                            }
                        }
                    }
                }
            }
        }

        // 3. Geriye kalan sağlam makaleleri listele
        List<Makale> kCoreList = new ArrayList<>();
        for (String id : makaleMap.keySet()) {
            if (!removedNodes.contains(id)) {
                kCoreList.add(makaleMap.get(id));
            }
        }

        return kCoreList;
    }

}
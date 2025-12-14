package com.kocaeli.graphcite.graph;

import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphManager {
    private Graph graph;
    private List<Makale> data;

    public GraphManager(List<Makale> data) {
        this.data = data;
        // Grafiği oluştururken strict modu kapatıyoruz, hata toleransı artsın
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        this.graph = new SingleGraph("CitationGraph");
    }

    public Graph createGraph() {
        graph.clear(); // Grafiği temizle

        // 1. CSS DÜZELTMESİ (Text Block yerine düz String kullanıyoruz)
        // Okuma hatasını önlemek için hepsini tek satırda veya + ile birleştirerek yazıyoruz.
        String style = "node { size: 15px; fill-color: #333; text-mode: hidden; } " +
                "node:clicked { fill-color: red; size: 25px; } " +
                "edge.reference { fill-color: black; size: 0.5px; arrow-shape: arrow; arrow-size: 4px, 2px; }";

        // Stili ata
        graph.setAttribute("ui.stylesheet", style);
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        // 2. ADIM: Düğümleri Ekle
        Collections.sort(data, Comparator.comparing(Makale::getId));
        Set<String> existingIds = new HashSet<>();

        for (Makale m : data) {
            Node n = graph.addNode(m.getId());
            n.setAttribute("ui.label", m.getId());
            n.setAttribute("data", m);
            existingIds.add(m.getId());
        }

        // 3. ADIM: Sıralı Bağlantı (Yeşil/Kırmızı Çizgiler)
        // Burada CSS class yerine direkt stil (Inline Style) basıyoruz ki GARANTİ görünsün.
        for (int i = 0; i < data.size() - 1; i++) {
            String sourceId = data.get(i).getId();
            String targetId = data.get(i + 1).getId();

            Edge e = graph.addEdge("seq_" + i, sourceId, targetId, true);

            // DİKKAT: Test için rengi RED (Kırmızı) ve boyutu 3px yaptık.
            // arrow-shape: none -> Ok ucunu kaldırır.
            e.setAttribute("ui.style", "fill-color: green; size: 3px; arrow-shape: none;");
        }

        // 4. ADIM: Referanslar (Siyah Oklar)
        int edgeCounter = 0;
        for (Makale m : data) {
            for (String refId : m.getReferencedWorkIds()) {
                if (existingIds.contains(refId)) {
                    try {
                        Edge e = graph.addEdge("ref_" + edgeCounter++, m.getId(), refId, true);
                        e.setAttribute("ui.class", "reference"); // Yukarıdaki CSS'i kullanır
                    } catch (Exception ignored) {}
                }
            }
        }

        return graph;
    }
}
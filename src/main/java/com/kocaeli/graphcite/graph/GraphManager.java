package com.kocaeli.graphcite.graph;

import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphManager {
    private Graph graph;
    private List<Makale> data;

    public GraphManager(List<Makale> data) {
        this.data = data;

        // Renderer
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

        this.graph = new SingleGraph("CitationGraph");

        // Hata toleransı ve kolaylık
        this.graph.setStrict(false);
        this.graph.setAutoCreate(true);
    }

    public Graph createGraph() {
        graph.clear();

        // CSS: okunaklı + isterlerdeki görsele yakın
        String style =
                "graph { fill-color: #FDFDFD; padding: 50px; } " +

                        // Default node
                        "node { size: 16px; fill-color: #3498db; stroke-mode: plain; stroke-color: white; stroke-width: 1px; " +
                        "text-mode: hidden; text-size: 12px; text-color: #2c3e50; } " +

                        // Tıklanan node
                        "node.selected { fill-color: #f1c40f; size: 28px; stroke-color: #333; stroke-width: 2px; } " +

                        // H-core node
                        "node.hcore { fill-color: #e67e22; size: 20px; } " +

                        // Yeni eklenen/ön plana çıkarılan (isteğe uygun ayırt etme)
                        "node.newcore { fill-color: #2ecc71; size: 20px; } " +

                        // Hover bilgi kartı (label açılır)
                        "node.hover { text-mode: normal; text-background-mode: rounded-box; text-background-color: white; " +
                        "text-padding: 4px; text-color: #2c3e50; } " +

                        // Siyah oklar (referans)
                        "edge.reference { fill-color: #2c3e50; size: 1px; arrow-shape: arrow; arrow-size: 7px, 4px; } " +

                        // Yeşil bağlantı (sıralı)
                        "edge.sequence { fill-color: #2ecc71; size: 1px; arrow-shape: none; } ";

        graph.setAttribute("ui.stylesheet", style);
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        // 1) Düğümler
        data.sort(Comparator.comparing(Makale::getId));
        for (Makale m : data) {
            Node n = graph.addNode(m.getId());
            n.setAttribute("data", m); // hover ve info için
        }

        // 2) Yeşil sıralı bağlantı (id artan)
        for (int i = 0; i < data.size() - 1; i++) {
            try {
                Edge e = graph.addEdge("seq_" + i, data.get(i).getId(), data.get(i + 1).getId(), true);
                e.setAttribute("ui.class", "sequence");
            } catch (Exception ignored) {}
        }

        // 3) Siyah oklar (referans)
        int edgeCounter = 0;
        Set<String> allIds = new HashSet<>();
        for (Makale m : data) allIds.add(m.getId());

        for (Makale m : data) {
            for (String refId : m.getReferencedWorkIds()) {
                if (allIds.contains(refId)) {
                    try {
                        Edge e = graph.addEdge("ref_" + edgeCounter++, m.getId(), refId, true);
                        e.setAttribute("ui.class", "reference");
                    } catch (Exception ignored) {}
                }
            }
        }

        return graph;
    }
}

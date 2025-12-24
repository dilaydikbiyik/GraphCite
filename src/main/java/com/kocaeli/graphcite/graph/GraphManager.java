package com.kocaeli.graphcite.graph;

import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class GraphManager {

    private static final Logger logger = LoggerFactory.getLogger(GraphManager.class);

    private final Graph graph;
    private final List<Makale> data;

    public GraphManager(List<Makale> makaleler) {
        this.data = makaleler == null ? Collections.emptyList() : makaleler;

        System.setProperty(
                "org.graphstream.ui.renderer",
                "org.graphstream.ui.j2dviewer.J2DGraphRenderer"
        );

        graph = new SingleGraph("CitationGraph");
    }

    public Graph createGraph() {
        graph.clear();

        graph.setAttribute("ui.stylesheet", """
graph { padding: 60px; fill-color: #f8fafc; }

node {
  size: 12px;
  fill-color: #3b82f6;
  stroke-mode: plain;
  stroke-color: #1e40af;
  text-size: 11px;
  text-color: #0f172a;
  text-background-mode: rounded-box;
  text-background-color: rgba(255,255,255,200);
  text-padding: 2px, 4px;
}

node.selected { fill-color: #facc15; size: 22px; }
node.hcore { fill-color: #ef4444; size: 18px; }
node.newlyAdded { fill-color: #22c55e; size: 16px; }
node.betweenness { fill-color: #a855f7; size: 18px; stroke-mode: plain; stroke-color: #581c87; }
node.kcore { fill-color: #f97316; size: 16px; stroke-mode: plain; stroke-color: #9a3412; }

edge { fill-color: #64748b; size: 1px; arrow-size: 8px,4px; }
edge.blackEdge { fill-color: #64748b; size: 1px; arrow-size: 8px,4px; }
edge.timeline { fill-color: #10b981; size: 1px; arrow-size: 8px,4px; }
edge.timelineEdge { fill-color: #10b981; size: 1px; arrow-size: 8px,4px; }

edge.kcoreEdge { fill-color: #f97316; size: 2px; arrow-size: 9px,5px; }

sprite.hoverCard {
  text-size: 12;
  text-color: white;
  text-background-mode: rounded-box;
  text-background-color: rgba(15,23,42,220);
  text-padding: 6px, 8px;
  text-offset: 12px, -18px;
  text-alignment: at-right;
  z-index: 10;
}
""");

        graph.setAttribute("ui.antialias");
        graph.setAttribute("ui.quality");
        return graph;
    }

    public Graph getGraph() {
        return graph;
    }

    public void ensureNode(String id) {
        if (id == null) return;
        id = id.trim();
        if (id.isEmpty()) return;

        synchronized (graph) {
            Node n = graph.getNode(id);

            // Node yoksa ekle
            if (n == null) {
                try {
                    n = graph.addNode(id);
                    n.setAttribute("citationCount", 0);
                } catch (Exception e) {
                    logger.debug("ensureNode addNode hata: id={}", id, e);
                    return;
                }
            }

            // ✅ Node varsa bile xyz yoksa ver (kritik!)
            try {
                double[] xyz = n.getAttribute("xyz");
                if (xyz == null || xyz.length < 2) {
                    double x = (id.hashCode() % 2000) / 200.0;
                    double y = ((id.hashCode() / 2000) % 2000) / 200.0;
                    n.setAttribute("xyz", x, y, 0);
                }
            } catch (Exception ex) {
                double x = (id.hashCode() % 2000) / 200.0;
                double y = ((id.hashCode() / 2000) % 2000) / 200.0;
                n.setAttribute("xyz", x, y, 0);
            }
        }
    }


    /**
     * Tek ve kesin ensureDirectedEdge implementasyonu.
     * - Girdi temizleme (trim)
     * - Node varlığı kontrolü
     * - Tekil edge id oluşturma (sanitize edilmiş)
     * - Hata yakalama ve loglama
     */
    public void ensureDirectedEdge(String from, String to) {
        if (from == null || to == null) return;
        from = from.trim();
        to = to.trim();
        if (from.isEmpty() || to.isEmpty()) return;

        synchronized (graph) {
            try {
                if (graph.getNode(from) == null || graph.getNode(to) == null) return;

                String eid = "e_" + sanitizeId(from) + "_" + sanitizeId(to);
                if (graph.getEdge(eid) != null) return;

                Edge e = graph.addEdge(eid, from, to, true);
                if (e != null) e.setAttribute("ui.class", "blackEdge");
            } catch (Exception ex) {
                logger.debug("ensureDirectedEdge sırasında hata: {} -> {} (exception={})", from, to, ex.toString(), ex);
            }
        }
    }

    public void rebuildTimelineEdges() {
        synchronized (graph) {
            try {
                // 1. Mevcut yeşil (timeline) kenarları temizle
                List<Edge> toRemove = new ArrayList<>();
                for (Edge e : graph.getEdgeSet()) {
                    Boolean tl = e.getAttribute("timeline");
                    if (Boolean.TRUE.equals(tl)) toRemove.add(e);
                }
                for (Edge e : toRemove) {
                    try { graph.removeEdge(e); } catch (Exception ex) {
                        logger.debug("Timeline edge silinirken hata: {}", e.getId(), ex);
                    }
                }

                // 2. Graf üzerindeki düğüm ID'lerini al
                List<String> ids = new ArrayList<>();
                for (Node n : graph) ids.add(n.getId());

                // --- DEĞİŞİKLİK: Sayısal Sıralama ---
                // ID'leri içindeki sayısal değere göre sırala (Meryem Hoca'nın uyarısı) [cite: 17, 114]
                ids.sort((id1, id2) -> {
                    long n1 = extractNumber(id1);
                    long n2 = extractNumber(id2);
                    return Long.compare(n1, n2);
                });
                // ------------------------------------

                // 3. Sıralı ID'ler arasında yeşil kenar oluştur
                for (int i = 0; i < ids.size() - 1; i++) {
                    String a = ids.get(i);
                    String b = ids.get(i + 1);
                    String gid = "g_" + sanitizeId(a) + "_" + sanitizeId(b);
                    if (graph.getEdge(gid) != null) continue;

                    try {
                        // true -> Yönlü kenar (PDF Şekil 1'e uygun)
                        Edge ge = graph.addEdge(gid, a, b, true);
                        if (ge != null) {
                            ge.setAttribute("ui.class", "timeline");
                            ge.setAttribute("timeline", true);
                        }
                    } catch (Exception ex) {
                        logger.debug("Timeline edge eklenirken hata: {} <-> {} (gid={})", a, b, gid, ex);
                    }
                }
            } catch (Exception e) {
                logger.error("rebuildTimelineEdges sırasında beklenmeyen hata", e);
            }
        }
    }

    private String sanitizeId(String id) {
        if (id == null) return "";
        return id.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    /**
     * ID stringinden sayısal değeri çeker.
     * Örn: "https://openalex.org/W2756105776" -> 2756105776
     */
    private long extractNumber(String id) {
        if (id == null) return 0;
        try {
            // Sadece rakamları bırak, gerisini sil
            String numeric = id.replaceAll("[^0-9]", "");
            if (numeric.isEmpty()) return 0;
            return Long.parseLong(numeric);
        } catch (Exception e) {
            return 0;
        }
    }
}
package com.kocaeli.graphcite.graph;

import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.util.*;

public class GraphManager {

    private final Graph graph;
    private final List<Makale> data;

    public GraphManager(List<Makale> makaleler) {
        this.data = makaleler;

        System.setProperty(
                "org.graphstream.ui.renderer",
                "org.graphstream.ui.j2dviewer.J2DGraphRenderer"
        );

        graph = new SingleGraph("CitationGraph");
    }

    public Graph createGraph() {
        graph.clear();

        graph.setAttribute("ui.stylesheet", """
                    graph {
                        padding: 60px;
                        fill-color: #f8fafc;
                    }
                    node {
                        size: 12px;
                        fill-color: #3b82f6;
                        stroke-mode: plain;
                        stroke-color: #1e40af;
                        text-size: 12px;
                    }
                    node.selected {
                        fill-color: #facc15;
                        size: 22px;
                    }
                    node.hcore {
                        fill-color: #ef4444;
                        size: 18px;
                    }
                    edge {
                        fill-color: #94a3b8;
                        size: 1px;
                        arrow-size: 8px,4px;
                    }
                """);

        graph.setAttribute("ui.antialias");
        graph.setAttribute("ui.quality");
        graph.setAttribute("layout.force", 1.2);
        graph.setAttribute("layout.repulsion", 2.5);
        graph.setAttribute("layout.gravity", 0.08);
        graph.setAttribute("layout.quality", 4);


        // 1️⃣ NODE’LAR
        for (Makale m : data) {
            Node n = getOrCreateNode(m.getId());
            n.setAttribute("data", m);
        }

        // 2️⃣ EDGE’LER
        int edgeId = 0;
        Set<String> ids = new HashSet<>();
        data.forEach(m -> ids.add(m.getId()));

        for (Makale m : data) {
            for (String ref : m.getReferencedWorkIds()) {
                if (ids.contains(ref)) {
                    String eid = "e" + edgeId++;
                    if (graph.getEdge(eid) == null) {
                        try {
                            graph.addEdge(eid, m.getId(), ref, true);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        return graph;
    }

    private Node getOrCreateNode(String id) {
        Node n = graph.getNode(id);
        if (n == null) {
            n = graph.addNode(id);
        }
        return n;
    }

}
package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.graph.GraphManager;
import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.graphicGraph.GraphicElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MainFrame extends JFrame implements ViewerListener {

    private final Graph graph;
    private final GraphAlgorithms algorithms;
    private final ControlPanel controlPanel;
    private final ArticleInfoPanel articleInfoPanel;
    private final ViewerPipe pipe;

    private boolean viewReady = false; // 🔒 Camera/NPE kilidi

    public MainFrame(List<Makale> makaleler) {
        setTitle("GraphCite – Makale Graf Analiz Sistemi");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        algorithms = new GraphAlgorithms(makaleler);
        graph = new GraphManager(makaleler).createGraph();

        applyGraphStyle(graph);

        Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout(
                new org.graphstream.ui.layout.springbox.implementations.SpringBox()
        );

        ViewPanel view = viewer.addDefaultView(false);
        view.setBackground(new Color(248, 250, 252));

        setupZoom(view);
        setupMouseInteraction(view);

        add(view, BorderLayout.CENTER);

        controlPanel = new ControlPanel(algorithms, graph);
        articleInfoPanel = new ArticleInfoPanel();

        add(buildSidebar(makaleler), BorderLayout.EAST);

        pipe = viewer.newViewerPipe();
        pipe.addViewerListener(this);
        new Timer(40, e -> pipe.pump()).start();

        // 🔥 layout oturana kadar mouse kapalı
        new Timer(500, e -> viewReady = true).start();
    }

    /* ------------------ ZOOM ------------------ */
    private void setupZoom(ViewPanel view) {
        view.addMouseWheelListener(e -> {
            double zoomFactor = e.getWheelRotation() < 0 ? 0.85 : 1.15;

            Point3 before = view.getCamera().transformPxToGu(e.getX(), e.getY());
            view.getCamera().setViewPercent(view.getCamera().getViewPercent() * zoomFactor);
            Point3 after = view.getCamera().transformPxToGu(e.getX(), e.getY());

            view.getCamera().setViewCenter(
                    view.getCamera().getViewCenter().x + (before.x - after.x),
                    view.getCamera().getViewCenter().y + (before.y - after.y),
                    0
            );
        });
    }

    /* ------------------ NODE INTERACTION ------------------ */
    private void setupMouseInteraction(ViewPanel view) {
        MouseAdapter adapter = new MouseAdapter() {
            String hoverId = null;

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!viewReady) return;

                GraphicElement ge = view.findNodeOrSpriteAt(e.getX(), e.getY());
                if (ge != null) {
                    buttonPushed(ge.getId());
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!viewReady) return;

                GraphicElement ge = view.findNodeOrSpriteAt(e.getX(), e.getY());

                if (ge != null) {
                    String id = ge.getId();
                    if (!id.equals(hoverId)) {
                        if (hoverId != null) mouseLeft(hoverId);
                        hoverId = id;
                        mouseOver(id);
                    }
                } else if (hoverId != null) {
                    mouseLeft(hoverId);
                    hoverId = null;
                }
            }
        };

        view.addMouseListener(adapter);
        view.addMouseMotionListener(adapter);
    }

    /* ------------------ STYLE ------------------ */
    private void applyGraphStyle(Graph g) {
        g.setAttribute("ui.stylesheet", """
            graph {
                padding: 60px;
                fill-color: #f8fafc;
            }
            node {
                size: 12px;
                fill-color: #3b82f6;
                stroke-mode: plain;
                stroke-color: #1e40af;
                text-size: 11px;
                shape: circle;
                text-alignment: under;
                text-color: #0f172a;
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
                size: 1px;
                fill-color: #64748b;
                arrow-shape: arrow;
            }
        """);
        g.setAttribute("ui.quality");
        g.setAttribute("ui.antialias");
    }

    /* ------------------ SIDEBAR ------------------ */
    private JPanel buildSidebar(List<Makale> makaleler) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setPreferredSize(new Dimension(360, 0));

        JPanel header = new JPanel();
        header.setBackground(new Color(30, 41, 59));
        JLabel t = new JLabel("GraphCite");
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.add(t);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(new StatsPanel(makaleler));
        content.add(controlPanel);
        content.add(articleInfoPanel);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(new JScrollPane(content), BorderLayout.CENTER);
        return wrapper;
    }

    /* ------------------ VIEWER CALLBACKS ------------------ */
    @Override
    public void buttonPushed(String id) {
        Makale m = algorithms.getMakale(id);
        if (m == null) return;

        // info paneller
        controlPanel.showInfo(m);
        articleInfoPanel.update(
                m,
                algorithms.calculateHIndex(id),
                (int) algorithms.calculateHMedian(id)
        );

        // reset
        for (Node n : graph) n.removeAttribute("ui.class");

        Node center = graph.getNode(id);
        if (center != null) center.setAttribute("ui.class", "selected");

        for (Makale hc : algorithms.getHCore(id)) {
            Node n = graph.getNode(hc.getId());
            if (n != null) n.setAttribute("ui.class", "hcore");
        }
    }

    private void mouseOver(String id) {
        Node n = graph.getNode(id);
        if (n != null) n.setAttribute("ui.label", id);
    }

    private void mouseLeft(String id) {
        Node n = graph.getNode(id);
        if (n != null) n.removeAttribute("ui.label");
    }

    @Override public void buttonReleased(String id) {}
    @Override public void viewClosed(String viewName) {}
}

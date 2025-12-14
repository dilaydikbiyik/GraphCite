package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.graph.GraphManager;
import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Graph;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.View;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private GraphAlgorithms algorithms;
    private Graph graph;
    private Viewer viewer;

    public MainFrame(List<Makale> makaleler) {
        // 1. Pencere Ayarları
        setTitle("GraphCite - Makale Analiz Sistemi");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 2. Veri ve Graf Hazırlığı
        this.algorithms = new GraphAlgorithms(makaleler);
        GraphManager manager = new GraphManager(makaleler);
        this.graph = manager.createGraph();

        // 3. Görselleştirme (GraphStream 1.3)
        // DİKKAT: System.setProperty satırını kaldırdık.
        viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();

        // Grafiği panele gömme
        ViewPanel viewPanel = viewer.addDefaultView(false);
        add(viewPanel, BorderLayout.CENTER);

        // 4. Sağ Panel
        JPanel controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(250, 0));
        controlPanel.setBackground(Color.LIGHT_GRAY);
        controlPanel.setBorder(BorderFactory.createTitledBorder("Kontrol Paneli"));

        controlPanel.add(new JLabel("<html><h3>YAPILACAKLAR:</h3>" +
                "<ul><li>Butonlar buraya</li>" +
                "<li>K-Core Filtresi</li></ul></html>"));

        add(controlPanel, BorderLayout.EAST);
    }
}
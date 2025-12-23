package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ControlPanel extends JPanel {
    private final GraphAlgorithms algorithms;
    private final Graph graph;
    private JTextField txtSearch, txtK;
    private JTextArea txtInfo;

    public ControlPanel(GraphAlgorithms algorithms, Graph graph) {
        this.algorithms = algorithms;
        this.graph = graph;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Arama Bölümü
        add(createSectionTitle("Makale Sorgula"));
        txtSearch = new JTextField();
        JButton btnSearch = createButton("Bul & Odaklan", new Color(59, 130, 246));
        btnSearch.addActionListener(e -> findArticle());
        add(txtSearch);
        add(Box.createVerticalStrut(5));
        add(btnSearch);

        // Bilgi Kutusu
        add(Box.createVerticalStrut(20));
        add(createSectionTitle("Analiz Çıktısı"));
        txtInfo = new JTextArea(6, 20);
        txtInfo.setEditable(false);
        txtInfo.setLineWrap(true);
        txtInfo.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtInfo.setBackground(new Color(248, 250, 252));
        add(new JScrollPane(txtInfo));

        // Analiz Butonları
        add(Box.createVerticalStrut(20));
        add(createSectionTitle("Graf Analiz Metrikleri"));

        JButton btnH = createButton("H-Index / Median Hesapla", new Color(16, 185, 129));
        btnH.addActionListener(e -> calcH());
        add(btnH);

        add(Box.createVerticalStrut(8));
        JButton btnB = createButton("Betweenness Centrality", new Color(139, 92, 246));
        btnB.addActionListener(e -> calcB());
        add(btnB);

        add(Box.createVerticalStrut(8));
        JPanel kPanel = new JPanel(new BorderLayout(5, 0));
        kPanel.setOpaque(false);
        txtK = new JTextField("2", 3);
        JButton btnK = createButton("K-Core Uygula", new Color(245, 158, 11));
        btnK.addActionListener(e -> calcK());
        kPanel.add(new JLabel("K:"), BorderLayout.WEST);
        kPanel.add(txtK, BorderLayout.CENTER);
        kPanel.add(btnK, BorderLayout.EAST);
        add(kPanel);
    }

    private JLabel createSectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return l;
    }

    private JButton createButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void findArticle() {
        String id = txtSearch.getText().trim();
        Node n = graph.getNode(id);
        if (n != null) {
            n.setAttribute("ui.class", "selected");
            showInfo(algorithms.getMakale(id));
        }
    }

    private void calcH() {
        String id = txtSearch.getText().trim();
        if (id.isEmpty()) return;
        int h = algorithms.calculateHIndex(id);
        double median = algorithms.calculateHMedian(id); // [cite: 58]
        txtInfo.setText("H-Index: " + h + "\nH-Median: " + median + "\n(h-core görseli oluşturuldu.)");
    }

    private void calcB() {
        txtInfo.setText("Centrality hesaplanıyor, lütfen bekleyin...");
        Map<String, Double> scores = algorithms.calculateBetweennessCentrality();
        txtInfo.setText("Analiz Tamamlandı.\nEn merkezi düğümler kırmızıya boyandı.");
    }

    private void calcK() {
        int k = Integer.parseInt(txtK.getText());
        algorithms.runKCoreDecomposition(k);
        txtInfo.setText("K-Core (k=" + k + ") işlemi uygulandı.");
    }

    public void showInfo(Makale m) {
        if (m == null) return;

        // Tıklanan düğüm için h-index ve h-median hesapla
        int hIndex = algorithms.calculateHIndex(m.getId());
        double hMedian = algorithms.calculateHMedian(m.getId());

        txtInfo.setText(
                "═══ MAKALE ANALİZİ ═══\n\n" +
                        "ID: " + m.getId() + "\n" +
                        "H-Index: " + hIndex + "\n" +
                        "H-Median: " + hMedian + "\n" +
                        "Atıf Sayısı: " + m.getCitationCount() + "\n\n" +
                        "Başlık: " + m.getTitle()
        );

        // Grafı Genişletme Mantığı: h-core düğümlerini belirginleştir [cite: 69, 71]
        List<Makale> hCore = algorithms.getHCore(m.getId());
        for (Makale hc : hCore) {
            Node n = graph.getNode(hc.getId());
            if (n != null) n.setAttribute("ui.class", "hcore");
            n.setAttribute("ui.z-index", 15);

            // Not: Yeni düğüm ekleme işlemi JsonParser'da tüm düğümler
            // yüklendiği için burada sadece görünürlük/stil üzerinden yapılır.
        }
    }
}
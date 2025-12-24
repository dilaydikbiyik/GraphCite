package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphManager;
import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ControlPanel extends JPanel {
    private final Color BG_DARK = new Color(15, 23, 42);
    private final Color ACCENT = new Color(37, 99, 235);

    private final GraphAlgorithms algorithms;
    private final Graph graph;
    private final GraphManager graphManager;
    private final StatsPanel statsPanel;
    private final ArticleInfoPanel infoPanel;

    private final JTextField txtSearch;
    private final JTextField txtK;
    private final JTextArea txtLog;
    private final Set<String> inGraph = new HashSet<>();
    private Consumer<String> focusHandler;

    public ControlPanel(GraphAlgorithms alg, Graph g, GraphManager gm, StatsPanel sp, ArticleInfoPanel ip) {
        this.algorithms = alg;
        this.graph = g;
        this.graphManager = gm;
        this.statsPanel = sp;
        this.infoPanel = ip;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(createHeader("🔍 SORGULA"));
        txtSearch = new JTextField();
        txtSearch.setBackground(new Color(51, 65, 85));
        txtSearch.setForeground(Color.WHITE);
        txtSearch.setCaretColor(Color.WHITE);
        add(txtSearch);
        add(Box.createVerticalStrut(10));

        JButton btnFind = createBtn("Bul & Odaklan", ACCENT);
        btnFind.addActionListener(this::onFindArticle);
        add(btnFind);

        add(Box.createVerticalStrut(20));
        txtLog = new JTextArea(5, 20);
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setBackground(new Color(10, 15, 25));
        txtLog.setForeground(new Color(134, 239, 172));
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        add(new JScrollPane(txtLog) {{ setBorder(null); }});

        add(Box.createVerticalStrut(20));
        add(createHeader("⚙️ ANALİZ"));

        txtK = new JTextField("2");
        txtK.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        txtK.setBackground(new Color(51, 65, 85));
        txtK.setForeground(Color.WHITE);
        txtK.setCaretColor(Color.WHITE);

        add(new JLabel("k değeri") {{ setForeground(new Color(148,163,184)); setFont(new Font("Segoe UI", Font.PLAIN, 11)); }});
        add(txtK);
        add(Box.createVerticalStrut(8));

        JButton btnBetweenness = createBtn("Betweenness Centrality", new Color(124, 58, 237));
        btnBetweenness.addActionListener(this::onBetweenness);
        add(btnBetweenness);

        add(Box.createVerticalStrut(8));

        JButton btnKCore = createBtn("K-Core Uygula", new Color(245, 158, 11));
        btnKCore.addActionListener(this::onKCore);
        add(btnKCore);
    }

    private JLabel createHeader(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(new Color(148, 163, 184));
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return l;
    }

    private JButton createBtn(String t, Color c) {
        JButton b = new JButton(t);
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void onFindArticle(ActionEvent e) {
        findArticle();
    }

    private void findArticle() {
        String id = txtSearch.getText().trim();
        if (id.isEmpty()) {
            txtLog.setText("Lütfen bir makale ID'si girin.");
            return;
        }

        Makale m = algorithms.getMakale(id);
        if (m == null) {
            txtLog.setText("Makale bulunamadı: " + id);
            return;
        }

        // --- DÜZELTME: Grafı temizleme kodu KALDIRILDI ---
        // graph.clear();  <-- SİLİNDİ
        // inGraph.clear(); <-- SİLİNDİ
        // Artık yeni düğümler eskilerin üzerine eklenerek "graf genişletiliyor".

        showInfo(m);
    }

    public void showInfo(Makale m) {
        if (m == null) return;
        String cid = m.getId();
        int h = algorithms.calculateHIndex(cid);
        int med = algorithms.calculateHMedian(cid);

        txtLog.setText("Analiz Raporu\nID: " + cid + "\nH-Index: " + h + "\nH-Median: " + med);

        // Seçilen makaleyi ve h-core'u grafa ekle
        graphManager.ensureNode(cid);
        inGraph.add(cid);

        List<Makale> hcore = algorithms.getHCore(cid);
        for (Makale hc : hcore) {
            graphManager.ensureNode(hc.getId());
            inGraph.add(hc.getId());
        }

        // inGraph içindeki düğümler arasındaki referansları ekle
        // Bu döngü, yeni eklenen düğümlerle eskiler arasındaki bağlantıları da kurar.
        for (String from : new HashSet<>(inGraph)) {
            Makale f = algorithms.getMakale(from);
            if (f == null) continue;
            for (String to : f.getReferencedWorkIds()) {
                if (inGraph.contains(to)) {
                    graphManager.ensureDirectedEdge(from, to);
                }
            }
        }

        // timeline (yeşil) kenarlarını yeniden oluştur
        graphManager.rebuildTimelineEdges();

        // Stats ve info panel güncelle
        statsPanel.update(graph);
        infoPanel.update(m, h, med);

        // H-core düğümlerine özel sınıf atama
        applyNodeClassesForHCore(hcore);

        if (focusHandler != null) focusHandler.accept(cid);
    }

    private void applyNodeClassesForHCore(List<Makale> hcore) {
        Set<String> hIds = new HashSet<>();
        for (Makale mm : hcore) hIds.add(mm.getId());

        for (Node n : graph) {
            if (hIds.contains(n.getId())) {
                n.setAttribute("ui.class", "hcore");
            } else {
                Object cls = n.getAttribute("ui.class");
                if ("hcore".equals(cls)) n.removeAttribute("ui.class");
            }
        }
    }

    private void onBetweenness(ActionEvent e) {
        try {
            Map<String, Double> scores = algorithms.calculateBetweennessCentrality();
            if (scores == null || scores.isEmpty()) {
                txtLog.setText("Betweenness hesaplanamadı veya sonuç boş.");
                return;
            }

            // Tüm düğümlere betweenness değeri ata
            double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            for (Map.Entry<String, Double> en : scores.entrySet()) {
                Node n = graph.getNode(en.getKey());
                if (n != null) {
                    n.setAttribute("betweenness", en.getValue());
                    // normalize edip sınıf atama: üst %10'u vurgula
                    if (en.getValue() >= 0.9 * max) {
                        n.setAttribute("ui.class", "betweenness");
                    } else {
                        Object cls = n.getAttribute("ui.class");
                        if ("betweenness".equals(cls)) n.removeAttribute("ui.class");
                    }
                }
            }

            txtLog.setText("Betweenness centrality hesaplandı. En yüksek değer: " + max);
            statsPanel.update(graph);
        } catch (Exception ex) {
            txtLog.setText("Betweenness hesaplama sırasında hata: " + ex.getMessage());
        }
    }

    private void onKCore(ActionEvent e) {
        String kText = txtK.getText().trim();
        int k;
        try {
            k = Integer.parseInt(kText);
            if (k < 1) throw new NumberFormatException("k en az 1 olmalı");
        } catch (NumberFormatException ex) {
            txtLog.setText("Geçersiz k değeri: " + kText);
            return;
        }

        try {
            List<Makale> core = algorithms.runKCoreDecomposition(k);
            if (core == null) {
                txtLog.setText("K-Core sonucu boş döndü.");
                return;
            }

            Set<String> coreIds = new HashSet<>();
            for (Makale mm : core) coreIds.add(mm.getId());

            // Tüm düğümlere kcore sınıfı uygula veya kaldır
            for (Node n : graph) {
                if (coreIds.contains(n.getId())) {
                    n.setAttribute("ui.class", "kcore");
                } else {
                    Object cls = n.getAttribute("ui.class");
                    if ("kcore".equals(cls)) n.removeAttribute("ui.class");
                }
            }

            txtLog.setText("K-Core (k=" + k + ") uygulandı. Düğüm sayısı: " + core.size());
            statsPanel.update(graph);
        } catch (Exception ex) {
            txtLog.setText("K-Core uygulama sırasında hata: " + ex.getMessage());
        }
    }

    public void setFocusHandler(Consumer<String> h) {
        this.focusHandler = h;
    }
}
package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ControlPanel extends JPanel {

    private final GraphAlgorithms algorithms;
    private final Graph graph;

    private JTextField txtSearchId;
    private JTextArea txtInfo;
    private JTextField txtKInput;

    public ControlPanel(GraphAlgorithms algorithms, Graph graph) {
        this.algorithms = algorithms;
        this.graph = graph;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        initSearchBox();
        add(Box.createVerticalStrut(15));
        initInfoBox();
        add(Box.createVerticalStrut(15));
        initActionButtons();
    }

    // ---------------- UI ----------------

    private void initSearchBox() {
        JPanel pnl = createPanel("Makale Ara (ID)");
        txtSearchId = new JTextField(12);
        JButton btn = styledButton("Bul", new Color(52, 152, 219));
        btn.addActionListener(e -> findArticle());

        pnl.add(txtSearchId);
        pnl.add(btn);
        add(pnl);
    }

    private void initInfoBox() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(createBorder("Makale Bilgileri"));
        pnl.setMaximumSize(new Dimension(300, 240));

        txtInfo = new JTextArea("Bir makaleye tıklayın veya aratın...");
        txtInfo.setEditable(false);
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        txtInfo.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtInfo.setBackground(new Color(250, 250, 250));
        txtInfo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        pnl.add(new JScrollPane(txtInfo), BorderLayout.CENTER);
        add(pnl);
    }

    private void initActionButtons() {
        JPanel pnl = new JPanel(new GridLayout(4, 1, 5, 5));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(createBorder("Analiz İşlemleri"));
        pnl.setMaximumSize(new Dimension(300, 260));

        JButton btnH = styledButton("H-Index Hesapla", new Color(46, 204, 113));
        btnH.addActionListener(e -> calcHIndex());

        JButton btnCent = styledButton("Betweenness Centrality", new Color(155, 89, 182));
        btnCent.addActionListener(e -> calcCentrality());

        JPanel pnlK = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlK.setBackground(Color.WHITE);
        pnlK.add(new JLabel("K: "));
        txtKInput = new JTextField("2", 3);
        JButton btnK = styledButton("Filtrele", new Color(230, 126, 34));
        btnK.addActionListener(e -> calcKCore());

        pnlK.add(txtKInput);
        pnlK.add(Box.createHorizontalStrut(5));
        pnlK.add(btnK);

        JButton btnReset = styledButton("Grafiği Sıfırla", new Color(149, 165, 166));
        btnReset.addActionListener(e -> resetGraph());

        pnl.add(btnH);
        pnl.add(btnCent);
        pnl.add(pnlK);
        pnl.add(btnReset);
        add(pnl);
    }

    // ---------------- LOGIC ----------------

    private void findArticle() {
        String id = txtSearchId.getText().trim();
        if (id.isEmpty()) return;

        Node n = graph.getNode(id);
        if (n != null) {
            resetGraph();
            n.setAttribute("ui.class", "selected");
            showInfo(algorithms.getMakale(id));
        } else {
            JOptionPane.showMessageDialog(this, "Bulunamadı: " + id);
        }
    }

    private void calcHIndex() {
        String id = txtSearchId.getText().trim();
        if (id.isEmpty()) return;

        int h = algorithms.calculateHIndex(id);
        int hMedian = algorithms.calculateHMedian(id);
        List<Makale> core = algorithms.getHCore(id);

        txtInfo.setText(
                "H-INDEX SONUCU\n" +
                        "---------------------------\n" +
                        "ID: " + id + "\n" +
                        "h-index: " + h + "\n" +
                        "h-median: " + hMedian + "\n" +
                        "h-core size: " + core.size()
        );

        resetGraph();

        for (Makale m : core) {
            Node n = graph.getNode(m.getId());
            if (n != null) n.setAttribute("ui.class", "hcore");
        }

        Node target = graph.getNode(id);
        if (target != null) target.setAttribute("ui.class", "selected");
    }

    private void calcCentrality() {
        Map<String, Double> scores = algorithms.calculateBetweennessCentrality();
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        resetGraph();

        StringBuilder sb = new StringBuilder("TOP 5 MERKEZİLİK:\n");
        int count = 0;
        for (Map.Entry<String, Double> ent : sorted) {
            if (count++ >= 5) break;
            sb.append(ent.getKey()).append(" : ")
                    .append(String.format("%.2f", ent.getValue())).append("\n");

            Node n = graph.getNode(ent.getKey());
            if (n != null) {
                n.setAttribute("ui.style",
                        "fill-color:#e74c3c; size:35px; stroke-color:#333; stroke-width:2px;");
            }
        }
        txtInfo.setText(sb.toString());
    }

    private void calcKCore() {
        try {
            int k = Integer.parseInt(txtKInput.getText().trim());
            List<Makale> list = algorithms.runKCoreDecomposition(k);

            for (Node n : graph) {
                n.setAttribute("ui.style", "fill-color:#ecf0f1; size:6px;");
                n.setAttribute("ui.label", "");
                n.removeAttribute("ui.class");
            }

            for (Makale m : list) {
                Node n = graph.getNode(m.getId());
                if (n != null) {
                    n.setAttribute("ui.style",
                            "fill-color:#2ecc71; size:18px; stroke-color:white;");
                    n.setAttribute("ui.label", m.getId());
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Geçersiz sayı!");
        }
    }

    public void resetGraph() {
        for (Node n : graph) {
            n.removeAttribute("ui.style");
            n.removeAttribute("ui.class");
            n.setAttribute("ui.label", "");
        }
        txtInfo.setText("Bir makaleye tıklayın veya aratın...");
    }

    public void showInfo(Makale m) {
        if (m == null) return;

        txtInfo.setText(
                "ID: " + m.getId() + "\n" +
                        "Authors: " + safeAuthors(m.getAuthors()) + "\n" +
                        "Başlık: " + safe(m.getTitle()) + "\n" +
                        "Yıl: " + m.getYear() + "\n" +
                        "Atıf: " + m.getCitationCount()
        );
        txtSearchId.setText(m.getId());
    }

    // ---------------- HELPERS ----------------

    private String safe(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s;
    }

    // ✅ KRİTİK FIX: List<String> → String
    private String safeAuthors(List<String> authors) {
        if (authors == null || authors.isEmpty()) return "-";
        return authors.stream().collect(Collectors.joining(", "));
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    private JPanel createPanel(String title) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(Color.WHITE);
        p.setBorder(createBorder(title));
        p.setMaximumSize(new Dimension(300, 70));
        return p;
    }

    private javax.swing.border.Border createBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), title,
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.BLACK);
    }
}

package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.model.Makale;
import com.kocaeli.graphcite.parser.JsonParser;
import javafx.application.Platform;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * GraphCite Ana Uygulama Penceresi
 * JavaFX WebView ile D3.js görselleştirme kullanır.
 */
public class MainApp extends JFrame {
    
    private List<Makale> makaleler;
    private GraphAlgorithms algorithms;
    private GraphWebView graphWebView;
    
    // Sağ panel bileşenleri
    private JPanel rightPanel;
    private JTextArea logArea;
    private JLabel lblHIndex;
    private JLabel lblHMedian;
    private JLabel lblHCoreSize;
    private JLabel lblSelectedNode;
    
    // H-Core alt graf düğümleri (seçili + h-core)
    private java.util.Set<String> currentHCoreSubgraph = new java.util.HashSet<>();
    
    public MainApp(List<Makale> makaleler) {
        this.makaleler = makaleler;
        this.algorithms = new GraphAlgorithms(makaleler);
        
        initUI();
    }
    
    private void initUI() {
        setTitle("GraphCite – Makale Graf Analiz Sistemi");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        
        // JavaFX'i başlat (Platform.startup sadece bir kere çağrılmalı)
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Zaten başlatılmış
        }
        Platform.setImplicitExit(false);
        
        // Sol tarafta graf (WebView)
        graphWebView = new GraphWebView(makaleler, algorithms);
        graphWebView.setPreferredSize(new Dimension(1000, 600));
        
        // Düğüm tıklama olayını dinle
        graphWebView.setNodeClickListener((nodeId, hIndex, hMedian, hcoreIds) -> {
            updateInfoPanel(nodeId, hIndex, hMedian, hcoreIds.size());
            log("📊 " + nodeId.substring(nodeId.length() - 10) + " → H-Index: " + hIndex + ", H-Median: " + hMedian);
            
            // H-Core alt grafını GENİŞLET (temizlemeden ekle!)
            int previousSize = currentHCoreSubgraph.size();
            currentHCoreSubgraph.add(nodeId);
            currentHCoreSubgraph.addAll(hcoreIds);
            int addedCount = currentHCoreSubgraph.size() - previousSize;
            
            log("📈 Genişleme: +" + addedCount + " düğüm → Toplam: " + currentHCoreSubgraph.size() + " düğüm");
        });
        
        // H-Core temizleme olayını dinle
        graphWebView.setHCoreClearListener(() -> {
            currentHCoreSubgraph.clear();
            log("🔄 H-Core alt graf temizlendi");
        });
        
        add(graphWebView, BorderLayout.CENTER);
        
        // Sağ panel
        rightPanel = createRightPanel();
        add(rightPanel, BorderLayout.EAST);
        
        // Başlangıç istatistikleri
        updateInitialStats();
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(15, 23, 42));
        panel.setPreferredSize(new Dimension(340, 0));
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // Header - Gradient efekti için
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, new Color(59, 130, 246), getWidth(), 0, new Color(139, 92, 246));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 15));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        header.setPreferredSize(new Dimension(340, 70));
        JLabel title = new JLabel("GraphCite");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        header.add(title);
        panel.add(header);
        
        // Ayırıcı çizgi
        panel.add(createSeparator());
        
        // İstatistikler
        panel.add(createStatsSection());
        
        // Ayırıcı çizgi
        panel.add(createSeparator());
        
        // Seçili makale bilgisi
        panel.add(createInfoSection());
        
        // Ayırıcı çizgi
        panel.add(createSeparator());
        
        // Analiz butonları
        panel.add(createButtonsSection());
        
        // Ayırıcı çizgi
        panel.add(createSeparator());
        
        // Log alanı
        panel.add(createLogSection());
        
        return panel;
    }
    
    private JPanel createSeparator() {
        JPanel sep = new JPanel();
        sep.setBackground(new Color(51, 65, 85));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(340, 1));
        return sep;
    }
    
    private JPanel createStatsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(new Color(15, 23, 42));
        section.setBorder(new EmptyBorder(18, 20, 15, 20));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));
        
        JLabel header = createSectionHeader("📊 GENEL İSTATİSTİKLER");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(header);
        section.add(Box.createVerticalStrut(12));
        
        section.add(createStatRow("Toplam Makale:", String.valueOf(makaleler.size())));
        section.add(Box.createVerticalStrut(4));
        
        int totalRefs = makaleler.stream()
            .mapToInt(m -> m.getReferencedWorkIds() != null ? m.getReferencedWorkIds().size() : 0)
            .sum();
        section.add(createStatRow("Toplam Referans:", String.valueOf(totalRefs)));
        section.add(Box.createVerticalStrut(4));
        
        int totalCited = makaleler.stream()
            .mapToInt(Makale::getCitationCount)
            .sum();
        section.add(createStatRow("Toplam Alınan Atıf:", String.valueOf(totalCited)));
        section.add(Box.createVerticalStrut(4));
        
        // En çok atıf alan
        Makale topCited = makaleler.stream()
            .max((a, b) -> Integer.compare(a.getCitationCount(), b.getCitationCount()))
            .orElse(null);
        if (topCited != null) {
            section.add(createStatRow("En Çok Atıf Alan:", 
                topCited.getId().substring(topCited.getId().length() - 10) + " (" + topCited.getCitationCount() + ")"));
            section.add(Box.createVerticalStrut(4));
        }
        
        // En çok atıf veren
        Makale topReferencer = makaleler.stream()
            .max((a, b) -> Integer.compare(
                a.getReferencedWorkIds() != null ? a.getReferencedWorkIds().size() : 0,
                b.getReferencedWorkIds() != null ? b.getReferencedWorkIds().size() : 0))
            .orElse(null);
        if (topReferencer != null) {
            int refCount = topReferencer.getReferencedWorkIds() != null ? topReferencer.getReferencedWorkIds().size() : 0;
            section.add(createStatRow("En Çok Atıf Veren:", 
                topReferencer.getId().substring(topReferencer.getId().length() - 10) + " (" + refCount + ")"));
        }
        
        return section;
    }
    
    private JPanel createInfoSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(new Color(22, 33, 54));
        section.setBorder(new EmptyBorder(20, 20, 20, 20));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        
        JLabel header = createSectionHeader("🎯 SEÇİLİ MAKALE");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(header);
        section.add(Box.createVerticalStrut(10));
        
        lblSelectedNode = new JLabel("Bir düğüme tıklayın...");
        lblSelectedNode.setForeground(new Color(148, 163, 184));
        lblSelectedNode.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        lblSelectedNode.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(lblSelectedNode);
        
        section.add(Box.createVerticalStrut(14));
        
        JPanel metricsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        metricsPanel.setBackground(new Color(22, 33, 54));
        metricsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        metricsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // H-Index kutusu
        lblHIndex = new JLabel("-");
        JPanel hIndexBox = createMetricBox("H-Index", lblHIndex, new Color(59, 130, 246));
        metricsPanel.add(hIndexBox);
        
        // H-Median kutusu
        lblHMedian = new JLabel("-");
        JPanel hMedianBox = createMetricBox("H-Median", lblHMedian, new Color(16, 185, 129));
        metricsPanel.add(hMedianBox);
        
        // H-Core kutusu
        lblHCoreSize = new JLabel("-");
        JPanel hCoreBox = createMetricBox("H-Core", lblHCoreSize, new Color(139, 92, 246));
        metricsPanel.add(hCoreBox);
        
        section.add(metricsPanel);
        
        return section;
    }
    
    private JPanel createMetricBox(String title, JLabel valueLabel, Color accentColor) {
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 41, 59));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.setColor(accentColor);
                g2d.fillRoundRect(0, 0, getWidth(), 3, 3, 3);
            }
        };
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(new EmptyBorder(10, 8, 8, 8));
        
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
        inner.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(148, 163, 184));
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // valueLabel parametreden geliyor, stilini uygula
        valueLabel.setForeground(accentColor);
        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        inner.add(titleLabel);
        inner.add(Box.createVerticalStrut(2));
        inner.add(valueLabel);
        box.add(inner);
        
        return box;
    }
    
    private JPanel createButtonsSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(new Color(15, 23, 42));
        section.setBorder(new EmptyBorder(20, 20, 20, 20));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        
        JLabel header = createSectionHeader("⚡ ANALİZ ARAÇLARI");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(header);
        section.add(Box.createVerticalStrut(15));
        
        // === TÜM GRAF BÖLÜMÜ ===
        JLabel lblFullGraph = new JLabel("📊 Tüm Graf (1000 makale)");
        lblFullGraph.setForeground(new Color(148, 163, 184));
        lblFullGraph.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        lblFullGraph.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(lblFullGraph);
        section.add(Box.createVerticalStrut(8));
        
        // Betweenness - Tüm Graf
        JButton btnBetweennessAll = createButton("Betweenness (Tüm)", new Color(139, 92, 246));
        btnBetweennessAll.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBetweennessAll.addActionListener(e -> runBetweenness(false));
        section.add(btnBetweennessAll);
        section.add(Box.createVerticalStrut(8));
        
        // K-Core - Tüm Graf
        JPanel kCoreAllPanel = new JPanel();
        kCoreAllPanel.setLayout(new BoxLayout(kCoreAllPanel, BoxLayout.X_AXIS));
        kCoreAllPanel.setBackground(new Color(15, 23, 42));
        kCoreAllPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        kCoreAllPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField txtKAll = new JTextField("2");
        txtKAll.setMaximumSize(new Dimension(50, 40));
        txtKAll.setPreferredSize(new Dimension(50, 40));
        txtKAll.setBackground(new Color(30, 41, 59));
        txtKAll.setForeground(Color.WHITE);
        txtKAll.setCaretColor(Color.WHITE);
        txtKAll.setHorizontalAlignment(JTextField.CENTER);
        txtKAll.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        txtKAll.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1));
        
        JButton btnKCoreAll = createButton("K-Core (Tüm)", new Color(245, 158, 11));
        btnKCoreAll.addActionListener(e -> runKCore(txtKAll.getText(), false));
        
        kCoreAllPanel.add(txtKAll);
        kCoreAllPanel.add(Box.createHorizontalStrut(8));
        kCoreAllPanel.add(btnKCoreAll);
        section.add(kCoreAllPanel);
        
        section.add(Box.createVerticalStrut(15));
        
        // === H-CORE ALT GRAF BÖLÜMÜ ===
        JLabel lblSubGraph = new JLabel("🎯 H-Core Alt Graf");
        lblSubGraph.setForeground(new Color(148, 163, 184));
        lblSubGraph.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        lblSubGraph.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.add(lblSubGraph);
        section.add(Box.createVerticalStrut(8));
        
        // Betweenness - H-Core Alt Graf
        JButton btnBetweennessHCore = createButton("Betweenness (H-Core)", new Color(99, 102, 241));
        btnBetweennessHCore.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBetweennessHCore.addActionListener(e -> runBetweenness(true));
        section.add(btnBetweennessHCore);
        section.add(Box.createVerticalStrut(8));
        
        // K-Core - H-Core Alt Graf
        JPanel kCoreHCorePanel = new JPanel();
        kCoreHCorePanel.setLayout(new BoxLayout(kCoreHCorePanel, BoxLayout.X_AXIS));
        kCoreHCorePanel.setBackground(new Color(15, 23, 42));
        kCoreHCorePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        kCoreHCorePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField txtKHCore = new JTextField("2");
        txtKHCore.setMaximumSize(new Dimension(50, 40));
        txtKHCore.setPreferredSize(new Dimension(50, 40));
        txtKHCore.setBackground(new Color(30, 41, 59));
        txtKHCore.setForeground(Color.WHITE);
        txtKHCore.setCaretColor(Color.WHITE);
        txtKHCore.setHorizontalAlignment(JTextField.CENTER);
        txtKHCore.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        txtKHCore.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1));
        
        JButton btnKCoreHCore = createButton("K-Core (H-Core)", new Color(234, 88, 12));
        btnKCoreHCore.addActionListener(e -> runKCore(txtKHCore.getText(), true));
        
        kCoreHCorePanel.add(txtKHCore);
        kCoreHCorePanel.add(Box.createHorizontalStrut(8));
        kCoreHCorePanel.add(btnKCoreHCore);
        section.add(kCoreHCorePanel);
        
        return section;
    }
    
    private JPanel createLogSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(new Color(15, 23, 42));
        section.setBorder(new EmptyBorder(15, 20, 20, 20));
        
        JLabel header = createSectionHeader("📋 İŞLEM GEÇMİŞİ");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        section.add(header, BorderLayout.NORTH);
        
        logArea = new JTextArea(10, 20);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(10, 15, 30));
        logArea.setForeground(new Color(134, 239, 172));
        logArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        logArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        logArea.setSelectionColor(new Color(59, 130, 246));
        
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(30, 41, 59), 1));
        scroll.getVerticalScrollBar().setBackground(new Color(15, 23, 42));
        section.add(scroll, BorderLayout.CENTER);
        
        return section;
    }
    
    private JLabel createSectionHeader(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(226, 232, 240));
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        return label;
    }
    
    private JPanel createStatRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(15, 23, 42));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        
        JLabel lblName = new JLabel(label);
        lblName.setForeground(new Color(148, 163, 184));
        lblName.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        
        JLabel lblValue = new JLabel(value);
        lblValue.setForeground(new Color(96, 165, 250));
        lblValue.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        
        row.add(lblName, BorderLayout.WEST);
        row.add(lblValue, BorderLayout.EAST);
        
        return row;
    }
    
    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2d.setColor(color.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(color.brighter());
                } else {
                    g2d.setColor(color);
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                java.awt.FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setPreferredSize(new Dimension(200, 44));
        btn.setMinimumSize(new Dimension(100, 44));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        return btn;
    }
    
    private void updateInitialStats() {
        log("✅ " + makaleler.size() + " makale yüklendi.");
        log("🔄 Graf oluşturuluyor...");
    }
    
    private void updateInfoPanel(String nodeId, int hIndex, int hMedian, int hCoreSize) {
        SwingUtilities.invokeLater(() -> {
            String shortId = nodeId.length() > 20 ? "..." + nodeId.substring(nodeId.length() - 15) : nodeId;
            lblSelectedNode.setText(shortId);
            lblHIndex.setText(String.valueOf(hIndex));
            lblHMedian.setText(String.valueOf(hMedian));
            lblHCoreSize.setText(String.valueOf(hCoreSize));
        });
    }
    
    private void runBetweenness(boolean useHCoreSubgraph) {
        java.util.Set<String> nodeSubset = null;
        
        if (useHCoreSubgraph) {
            if (currentHCoreSubgraph.isEmpty()) {
                log("⚠️ Önce bir düğüme tıklayarak H-Core oluşturun!");
                return;
            }
            nodeSubset = new java.util.HashSet<>(currentHCoreSubgraph);
            log("⏳ Betweenness (H-Core: " + nodeSubset.size() + " düğüm) hesaplanıyor...");
        } else {
            log("⏳ Betweenness (Tüm Graf: " + makaleler.size() + " düğüm) hesaplanıyor...");
        }
        
        final java.util.Set<String> finalSubset = nodeSubset;
        
        SwingWorker<java.util.Map<String, Double>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.Map<String, Double> doInBackground() {
                return algorithms.calculateBetweennessCentralityBrandes(finalSubset);
            }
            
            @Override
            protected void done() {
                try {
                    var scores = get();
                    double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
                    String mode = finalSubset != null ? "H-Core" : "Tüm Graf";
                    log("✅ Betweenness (" + mode + ") tamamlandı. " + scores.size() + " düğüm, Max: " + String.format("%.2f", max));
                    graphWebView.sendBetweennessResults(scores);
                } catch (Exception e) {
                    log("❌ Betweenness hatası: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void runKCore(String kText, boolean useHCoreSubgraph) {
        try {
            int k = Integer.parseInt(kText.trim());
            
            java.util.Set<String> nodeSubset = null;
            
            if (useHCoreSubgraph) {
                if (currentHCoreSubgraph.isEmpty()) {
                    log("⚠️ Önce bir düğüme tıklayarak H-Core oluşturun!");
                    return;
                }
                nodeSubset = new java.util.HashSet<>(currentHCoreSubgraph);
                log("⏳ K-Core k=" + k + " (H-Core: " + nodeSubset.size() + " düğüm) hesaplanıyor...");
            } else {
                log("⏳ K-Core k=" + k + " (Tüm Graf: " + makaleler.size() + " düğüm) hesaplanıyor...");
            }
            
            final java.util.Set<String> finalSubset = nodeSubset;
            
            SwingWorker<List<Makale>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<Makale> doInBackground() {
                    return algorithms.runKCoreDecomposition(k, finalSubset);
                }
                
                @Override
                protected void done() {
                    try {
                        List<Makale> core = get();
                        String mode = finalSubset != null ? "H-Core" : "Tüm Graf";
                        log("✅ K-Core (" + mode + ") tamamlandı. " + core.size() + " düğüm bulundu.");
                        
                        // Sonuçları JavaScript'e gönder
                        List<String> kcoreIds = core.stream()
                            .map(Makale::getId)
                            .collect(java.util.stream.Collectors.toList());
                        graphWebView.sendKCoreResults(kcoreIds);
                        
                    } catch (Exception e) {
                        log("❌ K-Core hatası: " + e.getMessage());
                    }
                }
            };
            worker.execute();
            
        } catch (NumberFormatException e) {
            log("❌ Geçersiz k değeri: " + kText);
        }
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    // Main metodu
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("⏳ Veriler okunuyor...");
                List<Makale> data = JsonParser.parse("data.json");
                System.out.println("✅ " + data.size() + " makale yüklendi.");
                
                System.out.println("🚀 Arayüz başlatılıyor...");
                MainApp app = new MainApp(data);
                app.setVisible(true);
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Hata: " + e.getMessage(), 
                    "Başlatma Hatası", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

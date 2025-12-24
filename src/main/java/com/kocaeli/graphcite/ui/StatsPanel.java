package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Edge; // Edge sınıfını import ettik
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * İstatistik Paneli
 * PDF İsterleri:
 * - Toplam makale sayısı
 * - Toplam referans (siyah kenar) sayısı
 * - En çok referans alan makale
 * - En çok referans veren makale
 */
public class StatsPanel extends JPanel {
    private final JLabel lblTotalNodes;
    private final JLabel lblTotalEdges; // Siyah kenarlar
    private final JLabel lblTopCited;   // En çok referans alan (In-Degree)
    private final JLabel lblTopReferencer; // En çok referans veren (Out-Degree)

    private List<Makale> makaleler;

    public StatsPanel() {
        this(null);
    }

    public StatsPanel(List<Makale> makaleler) {
        this.makaleler = makaleler;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(15, 23, 42));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel header = new JLabel("Genel İstatistikler");
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(header);
        add(Box.createVerticalStrut(10));

        lblTotalNodes = createStatLabel("Düğüm Sayısı: -");
        lblTotalEdges = createStatLabel("Referans (Kenar): -");
        lblTopCited = createStatLabel("En Çok Atıf Alan: -");
        lblTopReferencer = createStatLabel("En Çok Atıf Veren: -");

        add(lblTotalNodes);
        add(Box.createVerticalStrut(6));
        add(lblTotalEdges);
        add(Box.createVerticalStrut(6));
        add(lblTopCited);
        add(Box.createVerticalStrut(6));
        add(lblTopReferencer);

        // Eğer başlangıçta makaleler verilmişse güncelle
        if (this.makaleler != null) update(this.makaleler);
    }

    private JLabel createStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(148, 163, 184));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    /**
     * Graph üzerinden anlık görüntü güncelleme (Görselleştirme sırasında)
     * DÜZELTME: edges() yerine getEdgeSet() kullanıldı.
     */
    public void update(Graph g) {
        if (g == null) return;

        int nodes = g.getNodeCount();

        // Hata veren Stream yerine klasik döngü kullanıyoruz
        int blackEdges = 0;
        // getEdgeSet() metodunu GraphManager'da da kullandığınız için burada güvenlidir
        for (Edge e : g.getEdgeSet()) {
            Object uiClass = e.getAttribute("ui.class");
            if ("blackEdge".equals(uiClass)) {
                blackEdges++;
            }
        }

        lblTotalNodes.setText("Görsel Düğüm: " + nodes);
        lblTotalEdges.setText("Görsel Referans: " + blackEdges);
    }

    /**
     * Tüm veri seti üzerinden kesin istatistikleri hesaplar (Başlangıçta çalışır)
     */
    public void update(List<Makale> makaleler) {
        this.makaleler = makaleler;
        if (makaleler == null || makaleler.isEmpty()) {
            lblTotalNodes.setText("Düğüm Sayısı: 0");
            lblTotalEdges.setText("Referans Sayısı: 0");
            lblTopCited.setText("En Çok Atıf Alan: -");
            lblTopReferencer.setText("En Çok Atıf Veren: -");
            return;
        }

        // 1. Toplam Düğüm
        lblTotalNodes.setText("Toplam Makale: " + makaleler.size());

        // 2. Toplam Referans (Verilen siyah kenar sayısı)
        int totalRefs = makaleler.stream()
                .mapToInt(m -> m.getReferencedWorkIds() == null ? 0 : m.getReferencedWorkIds().size())
                .sum();
        lblTotalEdges.setText("Toplam Referans: " + totalRefs);

        // 3. En Çok Atıf ALAN (Max In-Degree)
        Makale mostCited = makaleler.stream()
                .filter(Objects::nonNull)
                .max((a, b) -> Integer.compare(a.getCitationCount(), b.getCitationCount()))
                .orElse(null);

        if (mostCited != null) {
            lblTopCited.setText(String.format("En Çok Atıf Alan: %s (%d)", mostCited.getId(), mostCited.getCitationCount()));
        } else {
            lblTopCited.setText("En Çok Atıf Alan: -");
        }

        // 4. En Çok Atıf VEREN (Max Out-Degree)
        Makale mostReferencing = makaleler.stream()
                .filter(Objects::nonNull)
                .max((a, b) -> Integer.compare(
                        a.getReferencedWorkIds().size(),
                        b.getReferencedWorkIds().size())
                )
                .orElse(null);

        if (mostReferencing != null) {
            lblTopReferencer.setText(String.format("En Çok Atıf Veren: %s (%d)", mostReferencing.getId(), mostReferencing.getReferencedWorkIds().size()));
        } else {
            lblTopReferencer.setText("En Çok Atıf Veren: -");
        }
    }
}
package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * Basit istatistik paneli.
 * - Hem parametresiz hem de List<Makale> alan kurucu sağlar.
 * - Graph veya List<Makale> ile güncellenebilen update metodları içerir.
 */
public class StatsPanel extends JPanel {
    private final JLabel lblTotalNodes;
    private final JLabel lblTotalEdges;
    private final JLabel lblTopCited;
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
        add(Box.createVerticalStrut(8));

        lblTotalNodes = createStatLabel("Düğüm: -");
        lblTotalEdges = createStatLabel("Ken ar: -");
        lblTopCited = createStatLabel("En çok atıf alan: -");

        add(lblTotalNodes);
        add(Box.createVerticalStrut(6));
        add(lblTotalEdges);
        add(Box.createVerticalStrut(6));
        add(lblTopCited);

        // Eğer başlangıçta makaleler verilmişse, onları kullanarak ilk güncellemeyi yap
        if (this.makaleler != null) update(this.makaleler);
    }

    private JLabel createStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(148, 163, 184));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    /**
     * Graph üzerinden istatistikleri günceller.
     */
    public void update(Graph g) {
        if (g == null) {
            lblTotalNodes.setText("Düğüm: -");
            lblTotalEdges.setText("Ken ar: -");
            return;
        }

        int nodes = 0;
        int edges = 0;
        for (Node n : g) nodes++;
        edges = g.getEdgeCount();

        lblTotalNodes.setText("Düğüm: " + nodes);
        lblTotalEdges.setText("Ken ar: " + edges);

        // Eğer makale listesi de varsa en çok atıf alanı göster (fallback)
        if (makaleler != null && !makaleler.isEmpty()) {
            Makale top = makaleler.stream()
                    .filter(Objects::nonNull)
                    .max((a, b) -> Integer.compare(a.getCitationCount(), b.getCitationCount()))
                    .orElse(null);
            if (top != null) lblTopCited.setText("En çok atıf alan: " + top.getId() + " (" + top.getCitationCount() + ")");
            else lblTopCited.setText("En çok atıf alan: -");
        }
    }

    /**
     * Makale listesi üzerinden istatistikleri günceller.
     */
    public void update(List<Makale> makaleler) {
        this.makaleler = makaleler;
        if (makaleler == null || makaleler.isEmpty()) {
            lblTotalNodes.setText("Düğüm: 0");
            lblTotalEdges.setText("Ken ar: 0");
            lblTopCited.setText("En çok atıf alan: -");
            return;
        }

        lblTotalNodes.setText("Düğüm: " + makaleler.size());

        // Toplam referans (verilen kenar sayısı) hesapla
        int totalGivenRefs = makaleler.stream()
                .mapToInt(m -> m.getReferencedWorkIds() == null ? 0 : m.getReferencedWorkIds().size())
                .sum();
        lblTotalEdges.setText("Ken ar: " + totalGivenRefs);

        Makale top = makaleler.stream()
                .filter(Objects::nonNull)
                .max((a, b) -> Integer.compare(a.getCitationCount(), b.getCitationCount()))
                .orElse(null);
        if (top != null) lblTopCited.setText("En çok atıf alan: " + top.getId() + " (" + top.getCitationCount() + ")");
        else lblTopCited.setText("En çok atıf alan: -");
    }
}

package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.model.Makale;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class StatsPanel extends JPanel {

    public StatsPanel(List<Makale> makaleler) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Veri Analizi
        int totalNodes = makaleler.size();
        int totalBlackEdges = 0;
        int totalGiven = 0;
        int totalTaken = 0;
        Map<String, Integer> incoming = new HashMap<>();
        Makale workhorse = null; // En çok referans veren

        for (Makale m : makaleler) {
            int refs = m.getReferencedWorkIds().size();
            totalGiven += refs;
            if (workhorse == null || refs > workhorse.getReferencedWorkIds().size()) workhorse = m;

            for (String rid : m.getReferencedWorkIds()) {
                incoming.put(rid, incoming.getOrDefault(rid, 0) + 1);
                totalBlackEdges++; // Sadeleştirilmiş siyah kenar hesabı [cite: 16, 49]
            }
        }

        String topId = "-";
        int topVal = 0;
        for (Map.Entry<String, Integer> e : incoming.entrySet()) {
            if (e.getValue() > topVal) { topVal = e.getValue(); topId = e.getKey(); }
            totalTaken += e.getValue();
        }

        // UI Kartları
        add(createStatRow("Genel Durum", "", true));
        add(createStatRow("Toplam Makale", String.valueOf(totalNodes), false));
        add(createStatRow("Siyah Kenar", String.valueOf(totalBlackEdges), false));

        add(Box.createVerticalStrut(15));
        add(createStatRow("En Popüler (Atıf Alan)", "", true));
        add(createStatRow("ID", topId, false));
        add(createStatRow("Atıf Sayısı", String.valueOf(topVal), false));

        add(Box.createVerticalStrut(15));
        add(createStatRow("En Çalışkan (Atıf Veren)", "", true));
        add(createStatRow("ID", (workhorse != null ? workhorse.getId() : "-"), false));
        add(createStatRow("Verilen Atıf", (workhorse != null ? String.valueOf(workhorse.getReferencedWorkIds().size()) : "0"), false));
    }

    private JPanel createStatRow(String label, String value, boolean isHeader) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(300, 30));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", isHeader ? Font.BOLD : Font.PLAIN, isHeader ? 14 : 12));
        lbl.setForeground(isHeader ? new Color(51, 65, 85) : new Color(100, 116, 139));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(new Color(37, 99, 235));

        p.add(lbl, BorderLayout.WEST);
        if (!isHeader) p.add(val, BorderLayout.EAST);
        return p;
    }
}
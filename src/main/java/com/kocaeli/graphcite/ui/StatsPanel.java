package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.model.Makale;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsPanel extends JPanel {

    public StatsPanel(List<Makale> makaleler) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(236, 240, 241)); // Hafif gri

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)), "Genel İstatistikler");
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        border.setTitleColor(Color.DARK_GRAY);
        setBorder(border);

        // Daha fazla satır gösterebilmek için büyüttük
        setMaximumSize(new Dimension(300, 190));

        int totalNodes = makaleler.size();

        // incoming hesaplamak için: refId -> kaç kere referans verilmiş
        Map<String, Integer> incoming = new HashMap<>();

        int totalGivenRefs = 0; // toplam verilen referans (outgoing)
        int totalBlackEdges = 0; // "siyah kenar": dataset içindeki geçerli referans sayısı

        // id seti: sadece dataset içinde olanları saymak istersek
        Map<String, Boolean> exists = new HashMap<>();
        for (Makale m : makaleler) exists.put(m.getId(), true);

        Makale mostReferencing = null; // en çok referans veren (outgoing max)
        String mostCitedId = null;     // en çok referans alan (incoming max)
        int mostCitedCount = -1;

        for (Makale m : makaleler) {
            int given = (m.getReferencedWorkIds() == null) ? 0 : m.getReferencedWorkIds().size();
            totalGivenRefs += given;

            if (mostReferencing == null || given > mostReferencing.getReferencedWorkIds().size()) {
                mostReferencing = m;
            }

            // incoming say
            if (m.getReferencedWorkIds() != null) {
                for (String refId : m.getReferencedWorkIds()) {
                    // sadece bizim grafımızda olan makalelere olan referansları "siyah kenar" sayalım
                    if (refId != null && exists.containsKey(refId)) {
                        totalBlackEdges++;
                        incoming.put(refId, incoming.getOrDefault(refId, 0) + 1);
                    }
                }
            }
        }

        int totalTakenRefs = 0; // toplam alınan referans = incoming toplamı
        for (int v : incoming.values()) totalTakenRefs += v;

        for (Map.Entry<String, Integer> e : incoming.entrySet()) {
            if (e.getValue() > mostCitedCount) {
                mostCitedCount = e.getValue();
                mostCitedId = e.getKey();
            }
        }

        // UI
        addLabel("• Toplam Makale: " + totalNodes);
        addLabel("• Toplam Referans (Siyah Kenar): " + totalBlackEdges);

        add(Box.createVerticalStrut(6));

        addLabel("• Toplam Verilen Referans: " + totalGivenRefs);
        addLabel("• Toplam Alınan Referans: " + totalTakenRefs);

        add(Box.createVerticalStrut(10));

        addLabel("• En Çok Referans Alan:");
        addLabel("  ID: " + (mostCitedId != null ? mostCitedId : "Bulunamadı"));
        addLabel("  Sayı: " + (mostCitedId != null ? mostCitedCount : 0));

        add(Box.createVerticalStrut(10));

        addLabel("• En Çok Referans Veren:");
        addLabel("  ID: " + (mostReferencing != null ? mostReferencing.getId() : "Bulunamadı"));
        addLabel("  Sayı: " + (mostReferencing != null ? mostReferencing.getReferencedWorkIds().size() : 0));
    }

    private void addLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        add(lbl);
    }
}

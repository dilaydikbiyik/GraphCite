package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.model.Makale;

import javax.swing.*;
import java.awt.*;

public class ArticleInfoPanel extends JPanel {

    private final JLabel content;

    public ArticleInfoPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Makale Bilgisi"));

        content = new JLabel("Bir düğüme tıklayınız.");
        content.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        add(content, BorderLayout.CENTER);
    }

    public void update(Makale m, int hIndex, int hMedian) {
        content.setText(String.format("""
            <html>
            <b>ID:</b> %s<br><br>
            <b>Yazarlar:</b> %s<br><br>
            <b>Başlık:</b> %s<br><br>
            <b>Yıl:</b> %d<br>
            <b>Atıf Sayısı:</b> %d<br><br>
            <b>h-index:</b> %d<br>
            <b>h-median:</b> %d
            </html>
        """,
                m.getId(),
                m.getAuthors(),
                m.getTitle(),
                m.getYear(),
                m.getCitationCount(),
                hIndex,
                hMedian
        ));
    }
}

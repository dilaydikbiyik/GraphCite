package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.model.Makale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ArticleInfoPanel extends JPanel {
    private final JEditorPane pane;

    public ArticleInfoPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 41, 59));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        pane = new JEditorPane("text/html", "");
        pane.setEditable(false);
        pane.setOpaque(false);

        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        add(scroll, BorderLayout.CENTER);
        reset();
    }

    public void reset() {
        pane.setText("<html><body style='font-family:Segoe UI; color:#94A3B8; margin:10px;'>Düğüm seçiniz.</body></html>");
    }

    public void update(Makale m, int h, int med) {
        if (m == null) {
            reset();
            return;
        }

        String authors = m.getAuthors() != null ? String.join(", ", m.getAuthors()) : "-";
        String title = m.getTitle() != null ? m.getTitle() : "-";
        int year = m.getYear();
        int citationCount = m.getCitationCount();

        pane.setText(String.format("""
            <html><body style='font-family:Segoe UI; color:#F8FAF6; margin:5px;'>
                <div style='color:#60A5FA; font-weight:bold; font-size:12px;'>%s</div>
                <div style='font-size:9px; color:#94A3B8; margin-top:4px;'>ID: %s</div>
                <hr style='border:0; border-top:1px solid #475569; margin:10px 0;'>
                <div style='font-size:11px; margin-bottom:4px;'>Yazarlar: <i style='color:#CBD5E1;'>%s</i></div>
                <table width='100%%' style='font-size:11px; color:#94A3B8;'>
                    <tr><td>Yıl: <b>%d</b></td><td align='right'>Atıf: <b style='color:#60A5FA;'>%d</b></td></tr>
                    <tr><td>H-Index: <b style='color:#10B981;'>%d</b></td><td align='right'>H-Median: <b style='color:#10B981;'>%d</b></td></tr>
                </table>
            </body></html>
            """, title, m.getId(), authors, year, citationCount, h, med));
    }
}

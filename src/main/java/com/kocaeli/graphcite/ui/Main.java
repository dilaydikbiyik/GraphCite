package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.model.Makale;
import com.kocaeli.graphcite.parser.JsonParser;

import javax.swing.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Arayüzün işletim sistemine (Mac/Windows) uygun görünmesi için
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Uygulamayı Başlat
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("⏳ Veriler okunuyor...");
                List<Makale> data = JsonParser.parse("data.json");
                System.out.println("✅ " + data.size() + " makale yüklendi.");

                System.out.println("🚀 Arayüz başlatılıyor...");
                MainFrame frame = new MainFrame(data);
                frame.setVisible(true); // Pencereyi göster

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Veri okuma hatası:\n" + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
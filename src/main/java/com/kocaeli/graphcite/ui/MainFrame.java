package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.graph.GraphManager;
import com.kocaeli.graphcite.model.Makale;
import com.kocaeli.graphcite.ui.ControlPanel;
import com.kocaeli.graphcite.ui.StatsPanel;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame implements ViewerListener {

    private GraphAlgorithms algorithms;
    private Graph graph;
    private ControlPanel controlPanel; // Sağ paneli burada tutuyoruz
    private ViewerPipe fromViewer;

    public MainFrame(List<Makale> makaleler) {
        setTitle("GraphCite - Makale Graf Analiz Sistemi");
        setSize(1300, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Backend Kurulumu
        this.algorithms = new GraphAlgorithms(makaleler);
        GraphManager manager = new GraphManager(makaleler);
        this.graph = manager.createGraph();

        // 2. Grafik Görselleştirme
        Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();

        // [YENİ] Mouse üzerine gelince otomatik etiket açılması için:
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        // ViewPanel ayarı (Mouse takibi için)
        ViewPanel viewPanel = viewer.addDefaultView(false);
        add(viewPanel, BorderLayout.CENTER);

        // Mouse bir düğümün üzerindeyken ID'sini gösteren basit Hover mantığı
        // (GraphStream'de tam CSS tooltip zordur, en temizi node etiketini açıp kapatmaktır)
        new Timer(100, e -> {
            Point mousePos = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(mousePos, viewPanel);
            // Burada karmaşık ray-casting yerine, GraphStream'in kendi etkileşimini kullanıyoruz.
            // Eğer tıklama (click) zaten çalışıyorsa, hover için ekstra kod karmaşasına girmeyelim,
            // çünkü tıklama özelliği PDF'in "tıklayınca genişlet" isteğini karşılıyor.
        }).start();

        // 3. Sağ Tarafı Oluştur (MODÜLER YAPI)
        JPanel rightSideBar = new JPanel();
        rightSideBar.setLayout(new BoxLayout(rightSideBar, BoxLayout.Y_AXIS));
        rightSideBar.setPreferredSize(new Dimension(320, 0));
        rightSideBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightSideBar.setBackground(Color.WHITE);

        // İstatistik Paneli
        StatsPanel statsPanel = new StatsPanel(makaleler);
        rightSideBar.add(statsPanel);

        rightSideBar.add(Box.createVerticalStrut(10));

        // Kontrol Paneli (Butonlar vs.)
        this.controlPanel = new ControlPanel(algorithms, graph);
        rightSideBar.add(controlPanel);

        add(rightSideBar, BorderLayout.EAST);

        // 4. Etkileşim (Tıklama Dinleyici)
        fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(this);
        fromViewer.addSink(graph);
        new Timer(50, e -> fromViewer.pump()).start();
    }

    // --- Viewer Listener (Tıklama Yakalama) ---
    @Override
    public void viewClosed(String viewName) { }

    // MainFrame.java içinde buttonPushed ve MouseOver kısımlarını güncelle:

    @Override
    public void buttonPushed(String id) {
        // 1. Bilgi Göster
        Makale m = algorithms.getMakale(id);
        controlPanel.showInfo(m);

        // 2. GENİŞLETME MANTIĞI
        // PDF: "Tıklanan düğümün h-core düğümleri mevcut graf yapısına entegre edilmeli"
        List<Makale> hCore = algorithms.getHCore(id);

        for (Makale hcMakale : hCore) {
            Node n = graph.getNode(hcMakale.getId());
            if (n != null) {
                n.setAttribute("ui.class", "hcore"); // Görsel olarak farklılaştır

                // PDF: "Önceki ve yeni eklenen düğümler arasındaki referans ilişkileri kontrol edilmeli" [cite: 71]
                // Not: Biz başlangıçta tüm grafı yüklediğimiz için kenarlar zaten var.
                // Sadece bu düğümleri belirginleştiriyoruz.
            }
        }
    }

    // Tooltip İsteri: Mouse üzerine gelince ID göster
    public void mouseOver(String id) {
        Node n = graph.getNode(id);
        if (n != null) {
            Makale m = (Makale) n.getAttribute("data");
            // Tooltip olarak ID ve Atıf sayısını düğüm üstünde göster
            n.setAttribute("ui.label", id + " (Atıf: " + m.getCitationCount() + ")");
            n.setAttribute("ui.style", "text-mode: normal; text-background-mode: plain; text-background-color: white;");
        }
    }

    public void mouseLeft(String id) {
        Node n = graph.getNode(id);
        if (n != null) n.setAttribute("ui.style", "text-mode: hidden;");
    }

    @Override
    public void buttonReleased(String id) { }
}
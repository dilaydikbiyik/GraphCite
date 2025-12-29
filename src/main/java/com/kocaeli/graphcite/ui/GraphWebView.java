package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.model.Makale;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaFX WebView ile D3.js graf görselleştirmesini sağlayan panel.
 * Swing içine gömülü olarak çalışır.
 */
public class GraphWebView extends JPanel {
    
    private JFXPanel jfxPanel;
    private WebView webView;
    private WebEngine webEngine;
    private GraphAlgorithms algorithms;
    private List<Makale> makaleler;
    private boolean webViewReady = false;
    
    // Java-JavaScript köprüsü
    private JavaConnector javaConnector;
    
    public GraphWebView(List<Makale> makaleler, GraphAlgorithms algorithms) {
        this.makaleler = makaleler;
        this.algorithms = algorithms;
        
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        
        // JavaFX'i başlat
        initJavaFX();
    }
    
    private void initJavaFX() {
        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);
        
        // JavaFX thread'inde çalıştır
        Platform.runLater(this::createWebView);
    }
    
    private void createWebView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        
        // JavaScript konsolunu Java'ya yönlendir
        webEngine.setOnError(event -> {
            System.err.println("WebView Error: " + event.getMessage());
        });
        
        // JavaScript alert'lerini yakala
        webEngine.setOnAlert(event -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, event.getData(), "Bilgi", JOptionPane.INFORMATION_MESSAGE);
            });
        });
        
        // Sayfa yüklendiğinde
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                webViewReady = true;
                
                // Java-JavaScript köprüsünü kur
                javaConnector = new JavaConnector();
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", javaConnector);
                
                // Veriyi JavaScript'e gönder
                sendDataToJS();
                
                System.out.println("✅ WebView hazır, veri gönderildi.");
            }
        });
        
        // HTML dosyasını yükle
        String htmlContent = loadHtmlFromResources();
        if (htmlContent != null) {
            webEngine.loadContent(htmlContent);
        } else {
            // Fallback: Dosyadan yükle
            String url = getClass().getResource("/web/graph.html").toExternalForm();
            webEngine.load(url);
        }
        
        Scene scene = new Scene(webView);
        jfxPanel.setScene(scene);
    }
    
    private String loadHtmlFromResources() {
        try (InputStream is = getClass().getResourceAsStream("/web/graph.html")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("HTML yüklenemedi: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Makale verilerini JSON olarak JavaScript'e gönder
     */
    private void sendDataToJS() {
        if (!webViewReady || makaleler == null) return;
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (Makale m : makaleler) {
            if (!first) json.append(",");
            first = false;
            
            json.append("{");
            json.append("\"id\":\"").append(escapeJson(m.getId())).append("\",");
            json.append("\"title\":\"").append(escapeJson(m.getTitle())).append("\",");
            json.append("\"year\":").append(m.getYear()).append(",");
            json.append("\"doi\":\"").append(escapeJson(m.getDoi())).append("\",");
            json.append("\"authors\":[");
            
            List<String> authors = m.getAuthors();
            if (authors != null && !authors.isEmpty()) {
                json.append(authors.stream()
                    .map(a -> "\"" + escapeJson(a) + "\"")
                    .collect(Collectors.joining(",")));
            }
            json.append("],");
            
            json.append("\"referenced_works\":[");
            List<String> refs = m.getReferencedWorkIds();
            if (refs != null && !refs.isEmpty()) {
                json.append(refs.stream()
                    .map(r -> "\"" + escapeJson(r) + "\"")
                    .collect(Collectors.joining(",")));
            }
            json.append("]");
            
            json.append("}");
        }
        json.append("]");
        
        final String jsonStr = json.toString();
        
        Platform.runLater(() -> {
            try {
                webEngine.executeScript("loadGraphData(" + jsonStr + ")");
            } catch (Exception e) {
                System.err.println("JS çağrısı hatası: " + e.getMessage());
            }
        });
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * JavaScript'e H-Core sonuçlarını gönder
     */
    public void sendHCoreResults(int hIndex, int hMedian, List<String> hcoreIds) {
        if (!webViewReady) return;
        
        String idsJson = hcoreIds.stream()
            .map(id -> "\"" + escapeJson(id) + "\"")
            .collect(Collectors.joining(",", "[", "]"));
        
        Platform.runLater(() -> {
            try {
                webEngine.executeScript(
                    "showHCoreResults(" + hIndex + "," + hMedian + "," + idsJson + ")"
                );
            } catch (Exception e) {
                System.err.println("H-Core JS hatası: " + e.getMessage());
            }
        });
    }
    
    /**
     * JavaScript'e Betweenness sonuçlarını gönder
     */
    public void sendBetweennessResults(java.util.Map<String, Double> scores) {
        if (!webViewReady) return;
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (var entry : scores.entrySet()) {
            if (!first) json.append(",");
            first = false;
            json.append("{\"id\":\"").append(escapeJson(entry.getKey()))
                .append("\",\"score\":").append(entry.getValue()).append("}");
        }
        json.append("]");
        
        Platform.runLater(() -> {
            try {
                webEngine.executeScript("showBetweennessResults(" + json + ")");
            } catch (Exception e) {
                System.err.println("Betweenness JS hatası: " + e.getMessage());
            }
        });
    }
    
    /**
     * JavaScript'e K-Core sonuçlarını gönder
     */
    public void sendKCoreResults(List<String> kcoreIds) {
        if (!webViewReady) return;
        
        String idsJson = kcoreIds.stream()
            .map(id -> "\"" + escapeJson(id) + "\"")
            .collect(Collectors.joining(",", "[", "]"));
        
        Platform.runLater(() -> {
            try {
                webEngine.executeScript(
                    "showKCoreResults(" + idsJson + ")"
                );
            } catch (Exception e) {
                System.err.println("K-Core JS hatası: " + e.getMessage());
            }
        });
    }

    /**
     * Java-JavaScript köprüsü sınıfı.
     * JavaScript'ten Java metodlarını çağırmak için kullanılır.
     */
    public class JavaConnector {
        
        /**
         * Düğüme tıklandığında JavaScript tarafından çağrılır.
         */
        public void onNodeClicked(String nodeId) {
            System.out.println("🖱️ Düğüme tıklandı: " + nodeId);
            
            SwingUtilities.invokeLater(() -> {
                if (algorithms == null) return;
                
                // H-Index hesapla
                int hIndex = algorithms.calculateHIndex(nodeId);
                int hMedian = algorithms.calculateHMedian(nodeId);
                List<Makale> hcore = algorithms.getHCore(nodeId);
                
                List<String> hcoreIds = hcore.stream()
                    .map(Makale::getId)
                    .collect(Collectors.toList());
                
                System.out.println("📊 H-Index: " + hIndex + ", H-Median: " + hMedian);
                System.out.println("📊 H-Core boyutu: " + hcoreIds.size());
                
                // Sonuçları JavaScript'e gönder
                sendHCoreResults(hIndex, hMedian, hcoreIds);
                
                // Event listener'lara bildir
                if (nodeClickListener != null) {
                    nodeClickListener.onNodeClicked(nodeId, hIndex, hMedian, hcoreIds);
                }
            });
        }
        
        /**
         * Debug amaçlı log
         */
        public void log(String message) {
            System.out.println("[JS] " + message);
        }
        
        /**
         * H-Core temizlendi callback
         */
        public void onHCoreClear() {
            System.out.println("🔄 H-Core temizlendi");
            if (hcoreClearListener != null) {
                SwingUtilities.invokeLater(() -> hcoreClearListener.onHCoreClear());
            }
        }
    }
    
    // Event listener için interface
    public interface NodeClickListener {
        void onNodeClicked(String nodeId, int hIndex, int hMedian, List<String> hcoreIds);
    }
    
    public interface HCoreClearListener {
        void onHCoreClear();
    }
    
    private NodeClickListener nodeClickListener;
    private HCoreClearListener hcoreClearListener;
    
    public void setNodeClickListener(NodeClickListener listener) {
        this.nodeClickListener = listener;
    }
    
    public void setHCoreClearListener(HCoreClearListener listener) {
        this.hcoreClearListener = listener;
    }
    
    /**
     * Veriyi yeniden yükle
     */
    public void refreshData() {
        sendDataToJS();
    }
}

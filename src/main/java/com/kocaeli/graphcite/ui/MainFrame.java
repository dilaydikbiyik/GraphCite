package com.kocaeli.graphcite.ui;

import com.kocaeli.graphcite.graph.GraphAlgorithms;
import com.kocaeli.graphcite.graph.GraphManager;
import com.kocaeli.graphcite.model.Makale;
import org.graphstream.graph.Graph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.view.ViewerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class MainFrame extends JFrame implements ViewerListener {
    private static final Logger logger = LoggerFactory.getLogger(MainFrame.class);

    private final GraphManager graphManager;
    private final List<Makale> makaleler;
    private final Graph graph;
    private final GraphAlgorithms algorithms;

    private final StatsPanel statsPanel;
    private final ArticleInfoPanel articleInfoPanel;
    private final ControlPanel controlPanel;

    private SpriteManager spriteManager;
    private Sprite hoverCard;
    private String lastHoverId;

    private final ViewerPipe pipe;
    private boolean viewReady = false;

    public MainFrame(List<Makale> makaleler) {
        this.makaleler = makaleler == null ? List.of() : makaleler;

        setTitle("GraphCite – Makale Graf Analiz Sistemi");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Core graph logic
        algorithms = new GraphAlgorithms(this.makaleler);
        graphManager = new GraphManager(this.makaleler);
        graph = graphManager.createGraph();

        // --- Hover card (sprite) init ---
        try {
            spriteManager = new SpriteManager(graph);
            hoverCard = spriteManager.addSprite("hoverCard");
            hoverCard.setAttribute("ui.class", "hoverCard");
            hoverCard.setAttribute("ui.hide");
        } catch (Exception ex) {
            logger.warn("SpriteManager/hoverCard init başarısız", ex);
            spriteManager = null;
            hoverCard = null;
        }

        // Apply any additional style (GraphManager already sets base stylesheet)
        applyGraphStyle(graph);

        // Viewer oluşturma (görselleştirme; başarısız olursa placeholder kullan)
        Viewer viewer = null;
        try {
            viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
            viewer.enableAutoLayout(new org.graphstream.ui.layout.springbox.implementations.SpringBox());
        } catch (Exception e) {
            logger.warn("Viewer veya AutoLayout başlatılamadı, görselleştirme sınırlı olabilir.", e);
            viewer = null;
        }

        Component viewComponent = null;
        if (viewer != null) {
            try {
                viewComponent = viewer.addDefaultView(false);
            } catch (Exception e) {
                logger.warn("ViewPanel oluşturulurken hata: fallback placeholder kullanılacak.", e);
                viewComponent = null;
            }
        }

        if (viewComponent == null) {
            viewComponent = new JPanel();
            viewComponent.setBackground(new Color(248, 250, 252));
            logger.warn("Gerçek ViewPanel oluşturulamadı; placeholder JPanel kullanılıyor.");
        } else {
            if (viewComponent instanceof JComponent) ((JComponent) viewComponent).setBackground(new Color(248, 250, 252));
        }

        // Fare / zoom / panning sadece gerçek ViewPanel için etkinleştirilecek
        setupZoom(viewComponent);
        setupMouseInteraction(viewComponent);
        enablePanning(viewComponent);

        add(viewComponent, BorderLayout.CENTER);

        // Sağ panel bileşenleri: önce Stats ve ArticleInfo, sonra ControlPanel (ControlPanel ArticleInfoPanel'e ihtiyaç duyuyor)
        statsPanel = new StatsPanel(this.makaleler);
        articleInfoPanel = new ArticleInfoPanel();
        controlPanel = new ControlPanel(algorithms, graph, graphManager, statsPanel, articleInfoPanel);

        // focus handler: ControlPanel'den gelen id'yi ViewPanel'e odaklar
        final Component viewFinal = viewComponent;
        controlPanel.setFocusHandler(id -> SwingUtilities.invokeLater(() -> {
            if (viewFinal instanceof ViewPanel) {
                centerOnNodeAnimated((ViewPanel) viewFinal, id, 0.12);
            } else {
                logger.debug("centerOnNodeAnimated çağrıldı fakat view gerçek ViewPanel değil.");
            }
        }));

        add(buildSidebar(this.makaleler), BorderLayout.EAST);

        // field: private final ViewerPipe pipe;
        ViewerPipe tempPipe = null;

        if (viewer != null) {
            try {
                tempPipe = viewer.newViewerPipe();
                tempPipe.addViewerListener(this);

                // local final referans timer içinde güvenle kullanılmak üzere
                final ViewerPipe pipeForTimer = tempPipe;
                new Timer(40, e -> {
                    try {
                        if (pipeForTimer != null) pipeForTimer.pump();
                    } catch (Exception ex) {
                        logger.warn("ViewerPipe pump sırasında hata: ", ex);
                    }
                }).start();
            } catch (Exception ex) {
                logger.warn("ViewerPipe oluşturulamadı, etkileşim sınırlı olabilir.", ex);
                tempPipe = null;
            }
        } else {
            tempPipe = null;
        }

        // Tek ve kesin atama
        pipe = tempPipe;

        // view hazır olana kadar kısa kilit
        new Timer(500, e -> viewReady = true).start();
    }

    /* ------------------ ZOOM ------------------ */
    private void setupZoom(Component viewComp) {
        if (!(viewComp instanceof ViewPanel)) return;
        ViewPanel view = (ViewPanel) viewComp;

        view.addMouseWheelListener(e -> {
            try {
                double minViewPercent = 0.02;
                double maxViewPercent = 4.0;
                double zoomFactor = e.getWheelRotation() < 0 ? 0.85 : 1.15;

                Point3 before = view.getCamera().transformPxToGu(e.getX(), e.getY());
                double newPercent = view.getCamera().getViewPercent() * zoomFactor;
                newPercent = Math.max(minViewPercent, Math.min(maxViewPercent, newPercent));
                view.getCamera().setViewPercent(newPercent);
                Point3 after = view.getCamera().transformPxToGu(e.getX(), e.getY());

                view.getCamera().setViewCenter(
                        view.getCamera().getViewCenter().x + (before.x - after.x),
                        view.getCamera().getViewCenter().y + (before.y - after.y),
                        0
                );
            } catch (Exception ex) {
                logger.debug("Zoom sırasında hata: ", ex);
            }
        });
    }

    /* ------------------ PANNING ------------------ */
    private void enablePanning(Component viewComp) {
        if (!(viewComp instanceof ViewPanel)) return;
        ViewPanel view = (ViewPanel) viewComp;

        final Point dragStart = new Point();
        final boolean[] dragging = {false};

        view.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                try {
                    if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0)) {
                        dragStart.setLocation(e.getX(), e.getY());
                        dragging[0] = true;
                        view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                } catch (Exception ex) { logger.debug("Panning mousePressed hata: ", ex); }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (dragging[0]) { dragging[0] = false; view.setCursor(Cursor.getDefaultCursor()); }
            }
        });

        view.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                try {
                    if (!dragging[0]) return;
                    Point3 guStart = view.getCamera().transformPxToGu(dragStart.x, dragStart.y);
                    Point3 guNow = view.getCamera().transformPxToGu(e.getX(), e.getY());
                    double dx = guStart.x - guNow.x;
                    double dy = guStart.y - guNow.y;
                    view.getCamera().setViewCenter(view.getCamera().getViewCenter().x + dx, view.getCamera().getViewCenter().y + dy, 0);
                    dragStart.setLocation(e.getX(), e.getY());
                } catch (Exception ex) { logger.debug("Panning mouseDragged hata: ", ex); }
            }
        });
    }

    /* ------------------ CENTER ON NODE (ANIMATED) ------------------ */
    private void centerOnNodeAnimated(ViewPanel view, String nodeId, double targetViewPercent) {
        if (view == null) { logger.warn("centerOnNodeAnimated: view null"); return; }
        org.graphstream.graph.Node n = graph.getNode(nodeId);
        if (n == null) { logger.debug("centerOnNodeAnimated: node bulunamadı: {}", nodeId); return; }
        Double nx = null, ny = null;

// ✅ 1) Önce xyz dene (GraphStream autolayout çoğu zaman bunu kullanır)
        try {
            double[] xyz = n.getAttribute("xyz");
            if (xyz != null && xyz.length >= 2) {
                nx = xyz[0];
                ny = xyz[1];
            }
        } catch (Exception ignored) {}

// ✅ 2) xyz yoksa x/y dene (senin manuel setlediğin olabilir)
        if (nx == null || ny == null) {
            try {
                Object ox = n.getAttribute("x");
                Object oy = n.getAttribute("y");
                if (ox instanceof Number && oy instanceof Number) {
                    nx = ((Number) ox).doubleValue();
                    ny = ((Number) oy).doubleValue();
                }
            } catch (Exception ignored) {}
        }

        if (nx == null || ny == null) {
            logger.warn("Düğüm koordinatları yok: {}", nodeId);
            return;
        }


        final double startX = view.getCamera().getViewCenter().x;
        final double startY = view.getCamera().getViewCenter().y;
        final double endX = nx; final double endY = ny;
        final double startPercent = view.getCamera().getViewPercent();
        final double endPercent = targetViewPercent;
        final int steps = 18; final int delay = 15;

        Timer anim = new Timer(delay, null);
        anim.addActionListener(new ActionListener() {
            int step = 0;
            @Override public void actionPerformed(ActionEvent e) {
                step++;
                double t = (double) step / steps;
                double ease = 0.5 - 0.5 * Math.cos(Math.PI * t);
                double cx = startX + (endX - startX) * ease;
                double cy = startY + (endY - startY) * ease;
                double cp = startPercent + (endPercent - startPercent) * ease;
                try { view.getCamera().setViewCenter(cx, cy, 0); view.getCamera().setViewPercent(cp); } catch (Exception ex) { anim.stop(); }
                if (step >= steps) anim.stop();
            }
        });
        anim.start();
    }

    /* ------------------ MOUSE HOVER / CLICK HANDLING (ViewPanel) ------------------ */
    private void setupMouseInteraction(Component viewComp) {
        if (!(viewComp instanceof ViewPanel)) return;
        ViewPanel view = (ViewPanel) viewComp;

        MouseAdapter adapter = new MouseAdapter() {
            String hoverId = null;
            @Override public void mouseClicked(MouseEvent e) {
                if (!viewReady) return;
                try {
                    GraphicElement ge = view.findNodeOrSpriteAt(e.getX(), e.getY());
                    if (ge == null) return;

                    String id = ge.getId();
                    // SADECE graph’ta node olan ID’lere izin ver
                    if (graph.getNode(id) == null) return;

                    buttonPushed(id);
                } catch (Exception ex) {
                    logger.debug("mouseClicked hata: ", ex);
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                if (!viewReady) return;
                try {
                    GraphicElement ge = view.findNodeOrSpriteAt(e.getX(), e.getY());
                    if (ge != null) {
                        String id = ge.getId();
                        if (graph.getNode(id) == null) return; // sadece node
                        if (!id.equals(hoverId)) {
                            if (hoverId != null) { mouseLeft(hoverId); hideHoverCard(); }
                            hoverId = id;
                            mouseOver(id);
                            showHoverCard(id);
                        }
                    } else if (hoverId != null) {
                        mouseLeft(hoverId);
                        hideHoverCard();
                        hoverId = null;
                    }
                } catch (Exception ex) { logger.debug("mouseMoved hata: ", ex); }
            }
        };

        view.addMouseListener(adapter);
        view.addMouseMotionListener(adapter);
    }

    /* ------------------ STYLE ------------------ */
    private void applyGraphStyle(Graph g) {
        // GraphManager.createGraph() zaten temel stylesheet'i koyuyor.
        // Burada ek/override gerekirse ekleyebilirsin; şu an aynı stylesheet'i tekrar set etmiyoruz.
        // Ancak uygulama başında kesin bir stylesheet istiyorsan GraphManager.createGraph()'ı kullan.
    }

    /* ------------------ SIDEBAR ------------------ */
    private JPanel buildSidebar(List<Makale> makaleler) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setPreferredSize(new Dimension(360, 0));

        JPanel header = new JPanel();
        header.setBackground(new Color(30, 41, 59));
        JLabel t = new JLabel("GraphCite");
        t.setForeground(Color.WHITE);
        t.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.add(t);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(statsPanel);
        content.add(controlPanel);
        content.add(articleInfoPanel);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(new JScrollPane(content), BorderLayout.CENTER);
        return wrapper;
    }

    /* ------------------ ViewerListener callbacks ------------------ */
    @Override public void buttonPushed(String id) {
        try {
            var m = algorithms.getMakale(id);
            if (m == null) return;
            controlPanel.showInfo(m); // yeterli
        } catch (Exception ex) {
            logger.debug("buttonPushed sırasında hata: ", ex);
        }
    }

    private void showHoverCard(String nodeId) {
        if (hoverCard == null) return;
        Makale m = algorithms.getMakale(nodeId);
        if (m == null) return;

        String authors = (m.getAuthors() == null) ? "-" : String.join(", ", m.getAuthors());
        String title = (m.getTitle() == null) ? "-" : m.getTitle();
        int citedBy = m.getCitationCount();

        String text =
                "ID: " + m.getId() + "\n" +
                        "Year: " + m.getYear() + "\n" +
                        "Authors: " + authors + "\n" +
                        "CitedBy: " + citedBy + "\n" +
                        "Title: " + title;

        try {
            hoverCard.attachToNode(nodeId);
            hoverCard.setAttribute("ui.label", text);
            hoverCard.removeAttribute("ui.hide");
        } catch (Exception ignored) {}
    }

    private void hideHoverCard() {
        if (hoverCard == null) return;
        try {
            hoverCard.setAttribute("ui.hide");
            hoverCard.setAttribute("ui.label", "");
        } catch (Exception ignored) {}
    }

    @Override public void buttonReleased(String id) {}
    @Override public void viewClosed(String viewName) {}

    /* ------------------ Hover helpers (used by setupMouseInteraction) ------------------ */
    private void mouseOver(String id) {
        try {
            var n = graph.getNode(id);
            if (n == null) return;
            var m = algorithms.getMakale(id);
            if (m == null) return;
            articleInfoPanel.update(m, algorithms.calculateHIndex(id), algorithms.calculateHMedian(id));
            String shortTitle = m.getTitle() == null ? "-" : (m.getTitle().length() > 40 ? m.getTitle().substring(0,40) + "..." : m.getTitle());
            if (!"selected".equals(n.getAttribute("ui.class"))) {
                n.setAttribute("ui.label", shortTitle);
            }
        } catch (Exception ex) {
            logger.debug("mouseOver hata: ", ex);
        }
    }

    private void mouseLeft(String id) {
        try {
            var n = graph.getNode(id);
            if (n != null) n.removeAttribute("ui.label");
        } catch (Exception ex) {
            logger.debug("mouseLeft hata: ", ex);
        }
    }
}

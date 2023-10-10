package cr.ac.tec.vizClone;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.model.CloneGraph;
import cr.ac.tec.vizClone.model.Fragment;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;

public class VizCloneToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**/
    // Dark colors
    private static final String GREEN = "#a0b4a1";
    private static final String YELLOW = "#8aa18b";
    private static final String ORANGE = "#5e7e60";
    private static final String RED = "#335c38";

    // Stroke widths
    private static final float NON_SELECTED_STROKE = 0.5F;
    private static final float SELECTED_STROKE = 2.5F;
    /**/
    /*
    // Light colors
    private static final String GREEN = "#8ff09d";
    private static final String YELLOW = "#f5c73d";
    private static final String ORANGE = "#f56b3d";
    private static final String RED = "#ff3838";
     */

    private static final String[] SIMILITUDE = { GREEN, YELLOW, ORANGE, RED };
    private static final int STRIPE_HEIGHT = 20; // pixels
    private static final int GRAPH_HEIGHT = 55;  // percent
    private static final int ZOOM_HEIGHT = 30;   // percent
    //private static final int BRACE_HEIGHT = 15;  // percent - implicit
    private static final int ZOOM_Y_OFFSET = 15; // percent
    private static final int ZOOM_Y_GROW = 5;    // percent
    private static final int ZOOM_Y_SCALE = 5;   // percent
    private static final int ZOOM_MAX_K = 7;     // horizontal increase factor - KEEP IT ODD

    private static final JBColor borderColor = new JBColor(Color.LIGHT_GRAY, Color.GRAY.darker());
    private static Graphics2D g2 = null;
    private static Rectangle codePanelRect = null;
    private static Rectangle edgePanelRect = null;
    private static Rectangle graphRect = null;
    private static Rectangle zoomedRect = null;
    private static Point[] bracePath =
            {new Point(),new Point(),new Point(),new Point(),new Point(),new Point(),new Point(),new Point()};
    private static Rectangle clonesPanelRect = null;
    private static Rectangle sliderRect = null;
    private static int firstZoomedClone = 0;
    private static final int numCodeFragments = 3000;
    private static final int numCodeClones = 3000;
    private static final int numZoomedClones = 200;

    private static CloneGraph graph = null;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        graph = new CloneGraph(numCodeFragments, numCodeClones);
        VizCloneToolWindowContent toolWindowContent = new VizCloneToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class VizCloneToolWindowContent {

        private final JPanel contentPanel = new JPanel();
        private final CodePanel codePanel = new CodePanel(STRIPE_HEIGHT);
        private final EdgePanel edgePanel = new EdgePanel();
        private final ClonePanel clonePanel = new ClonePanel(STRIPE_HEIGHT);

        public VizCloneToolWindowContent(ToolWindow toolWindow) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(codePanel, BorderLayout.NORTH);
            contentPanel.add(clonePanel, BorderLayout.SOUTH);
            contentPanel.add(edgePanel, BorderLayout.CENTER);
            edgePanel.setClonePanel(clonePanel);
            clonePanel.setEdgePanel(edgePanel);
        }

        public JPanel getContentPanel() { return contentPanel; }
    }

    private static class CodePanel extends JPanel implements ComponentListener {
        public CodePanel(int minHeight) {
            super();
            setOpaque(true);
            this.setPreferredSize(new Dimension(0, minHeight));
            this.addComponentListener(this);
        }

        public void componentMoved(ComponentEvent event) {

        }

        public void componentResized(ComponentEvent event) {
            codePanelRect = new Rectangle(0, 0, getWidth(), getHeight());
        }

        public void componentShown(ComponentEvent event) {

        }

        public void componentHidden(ComponentEvent event) {

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (codePanelRect != null && getX() >= 0 && getY() >= 0) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(codePanelRect);
                paintPanelBackground();
                for (int i = 0; i < codePanelRect.width; i++) {
                    int weight = graph.getFragments().get(i).getWeight();
                    int colorIndex = graph.getFragmentColorIndex(weight);
                    g2.setColor(Color.decode(SIMILITUDE[colorIndex]));
                    g2.drawLine(i, STRIPE_HEIGHT - weight, i, STRIPE_HEIGHT);
                }
                g2.dispose();
            }
        }

        private void paintPanelBackground() {
            g2.setColor(this.getParent().getBackground());
            g2.fillRect(codePanelRect.x, codePanelRect.y, codePanelRect.width, codePanelRect.height);
        }
    }

    private static class EdgePanel extends JPanel implements ComponentListener {

        private ClonePanel clonePanel = null;
        private int selectedClone = -1;
        private int hoveredClone = -1;

        public EdgePanel() {
            super();
            setOpaque(true);
            this.addComponentListener(this);
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    int pointedToClone = findPointedToClone(e);
                    if (pointedToClone != selectedClone)
                        selectedClone = pointedToClone;
                    else
                        selectedClone = -1;
                    repaint();
                }
            });
            this.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    int pointedToClone = findPointedToClone(e);
                    if (pointedToClone != hoveredClone) {
                        hoveredClone = pointedToClone;
                        repaint();
                    }
                }
            });
        }

        private int findPointedToClone(MouseEvent e) {
            int pointedToClone = -1;
            if (zoomedRect.contains(e.getPoint())) {
                int barWidth = zoomedRect.width / numZoomedClones;
                int cloneOffset = (e.getX() - zoomedRect.x + barWidth) / barWidth - 1;
                int barX = cloneOffset * barWidth + zoomedRect.x;
                int barY = zoomedRect.y;
                int clickedClone = cloneOffset + firstZoomedClone;
                int barHeight = zoomedRect.height;
                if (new Rectangle(barX, barY, barWidth, barHeight).contains(e.getPoint())) {
                    pointedToClone = clickedClone;
                }
            }
            return pointedToClone;
        }

        public void componentMoved(ComponentEvent event) {

        }

        public void componentResized(ComponentEvent event) {
            recalculate();
        }

        public void recalculate() {
            edgePanelRect = new Rectangle(0, 0, getWidth(), getHeight());
            graphRect = new Rectangle(0, 0, getWidth(), getHeight() * GRAPH_HEIGHT / 100);
            zoomedRect = new Rectangle(
                    getWidth() % numZoomedClones / 2,
                    getHeight() * GRAPH_HEIGHT / 100,
                    getWidth() / numZoomedClones * numZoomedClones,
                    getHeight() * ZOOM_HEIGHT / 100);

            int zbrx = zoomedRect.x + zoomedRect.width + 1; // zoomed bottom right
            int zblx = zoomedRect.x;                        // zoomed bottom left
            int strx = sliderRect.x + sliderRect.width + 1; // slider top right
            int stlx = sliderRect.x;                        // slider top left
            int zcpx = zoomedRect.width / 200;              // zoomed control point
            int scpx = sliderRect.width / 20;               // slider control point

            bracePath[0].x = zbrx;
            bracePath[1].x = zbrx + zcpx;
            bracePath[2].x = strx - scpx;
            bracePath[3].x = strx;
            bracePath[4].x = stlx;
            bracePath[5].x = stlx + scpx;
            bracePath[6].x = zblx - zcpx;
            bracePath[7].x = zblx - 1;

            int zby = zoomedRect.y + zoomedRect.height + 1;  // zoomed bottom
            int sty = edgePanelRect.height;                  // slider top
            int brh = sty - zby;                             // brace height

            bracePath[0].y = zby;
            bracePath[1].y = sty + brh / 2;
            bracePath[2].y = zby;
            bracePath[3].y = sty;
            bracePath[4].y = sty;
            bracePath[5].y = zby;
            bracePath[6].y = sty + brh / 2;
            bracePath[7].y = zby;
        }

        public void componentShown(ComponentEvent event) {

        }

        public void componentHidden(ComponentEvent event) {

        }

        public ClonePanel getClonePanel() {
            return clonePanel;
        }

        public void setClonePanel(ClonePanel clonePanel) {
            this.clonePanel = clonePanel;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (clonesPanelRect != null && edgePanelRect != null && getX() >= 0 && getY() >= 0) {
                Dimension size = getSize();
                g2 = (Graphics2D)g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = this.getParent().getBackground();
                g2.setColor(color);
                g2.setClip(edgePanelRect);
                g2.fillRect(clonesPanelRect.x, clonesPanelRect.y, clonesPanelRect.width, clonesPanelRect.height);
                paintZoomedSelection();
            }
        }

        private void paintZoomedSelection() {
            int barX = zoomedRect.x;
            int barY = zoomedRect.y;
            int barWidth = zoomedRect.width / numZoomedClones;
            int arcSize = barWidth * 2 / 3;

            // draw brace
            drawBrace();

            int cloneCenterX = zoomedRect.x + barWidth / 2;
            int cloneCenterY = barY;

            // draw arcs & zoomed-out clones
            if (selectedClone >= 0) {
                // clone selected
                // draw grayed arcs
                drawGrayedArcs(firstZoomedClone, selectedClone, cloneCenterX, cloneCenterY, barWidth);
                // paint grayed clones with selection
                drawZoomedOutClonesWithHighlight(selectedClone, firstZoomedClone, barX, barY, barWidth, arcSize);
            }
            else if (hoveredClone >= 0) {
                // clone hovered
                // draw grayed arcs
                drawGrayedArcs(firstZoomedClone, hoveredClone, cloneCenterX, cloneCenterY, barWidth);
                // paint grayed clones with hover
                drawZoomedOutClonesWithHighlight(hoveredClone, firstZoomedClone, barX, barY, barWidth, arcSize);
            }
            else {
                // no clone selected
                // draw all arcs
                drawArcs(firstZoomedClone, cloneCenterX, cloneCenterY, barWidth);
                // no clone selected - paint colored clones
                drawZoomedOutClones(firstZoomedClone, barX, barY, barWidth, arcSize);
            }
        }

        private void drawZoomedOutClones(int firstZoomedClone, int barX, int barY,int barWidth, int arcSize) {
            for (int i = 0; i < numZoomedClones; i++) {
                int idx = firstZoomedClone + i;
                int weight = graph.getClones().get(idx).getWeight();
                int barHeight = weight * zoomedRect.height / CloneGraph.MAX_WEIGHT;
                drawHighlightedClone(weight, barX, barY, barWidth, barHeight, arcSize);
                barX += barWidth;
            }
        }

        private void drawZoomedOutClonesWithHighlight(
                int highlightedClone, int firstZoomedClone,
                int barX, int barY, int barWidth, int arcSize) {
            int K_WIDTH = (ZOOM_MAX_K * ZOOM_MAX_K - 2 * ZOOM_MAX_K + 2);
            int K_WIDTH_2 = K_WIDTH / 2;
            for (int i = 0, cloneX = barX; i < numZoomedClones; i++, cloneX += barWidth) {
                int idx = firstZoomedClone + i;
                if (Math.abs(idx - highlightedClone + 1) > K_WIDTH_2) {
                    int weight = graph.getClones().get(idx).getWeight();
                    int barHeight = weight * zoomedRect.height / CloneGraph.MAX_WEIGHT;
                    g2.setColor(borderColor);
                    g2.fillRoundRect(cloneX, barY, barWidth, barHeight, arcSize, arcSize);
                    g2.setColor(borderColor);
                    g2.drawRoundRect(cloneX, barY, barWidth, barHeight, arcSize, arcSize);
                }
            }

            int leftCloneX = zoomedRect.x + barWidth * (highlightedClone - firstZoomedClone - K_WIDTH_2 - 1);
            int rightCloneX = leftCloneX + barWidth * (K_WIDTH - 1);
            int cloneWidth = barWidth;
            int yDelta = graphRect.height * ZOOM_Y_OFFSET / (ZOOM_MAX_K * 100);
            int cloneY = barY - yDelta;
            int highlightYFactor = zoomedRect.height * (100 + ZOOM_Y_OFFSET * 2 + ZOOM_Y_GROW) / (CloneGraph.MAX_WEIGHT * 100);

            int cloneX = zoomedRect.x + barWidth * (highlightedClone - firstZoomedClone - ZOOM_MAX_K / 2 - 1);
            int weight = graph.getClones().get(highlightedClone).getWeight();
            int barHeight = weight * highlightYFactor * (100 + ZOOM_MAX_K * ZOOM_Y_SCALE) / 100;

            // clear highlighted clones background
            paintHighlightedClones(true, highlightedClone, firstZoomedClone, barWidth, arcSize,
                    cloneWidth, cloneY, yDelta, leftCloneX, highlightYFactor, rightCloneX);

            // clear highlighted clone background
            clearHighlightedClone(cloneX, barY - yDelta * ZOOM_MAX_K, barWidth * ZOOM_MAX_K, arcSize);

            // paint colored highlithed clones
            paintHighlightedClones(false, highlightedClone, firstZoomedClone, barWidth, arcSize,
                    cloneWidth, cloneY, yDelta, leftCloneX, highlightYFactor, rightCloneX);

            int cloneCenterX = cloneX + barWidth * ZOOM_MAX_K / 2;
            int cloneCenterY = barY - yDelta * ZOOM_MAX_K;

            // paint colored central highlighted clone
            drawHighlightedClone(weight, cloneX, cloneCenterY, barWidth * ZOOM_MAX_K, barHeight, arcSize);

            // draw central highlighted clone selected arcs
            drawHighlightedArcs(highlightedClone, cloneCenterX, cloneCenterY);
        }

        private void paintHighlightedClones(boolean clearBackground,
                                                   int highlightedClone,
                                                   int firstZoomedClone,
                                                   int barWidth,
                                                   int arcSize,
                                                   int cloneWidth,
                                                   int cloneY,
                                                   int yDelta,
                                                   int leftCloneX,
                                                   int highlightYFactor,
                                                   int rightCloneX)
        {
            // paint colored highlighted clones
            for (int k1 = 1, k2 = ZOOM_MAX_K - 1; k1 < ZOOM_MAX_K - 1; k1++, k2--) {
                int leftHighlightedClone = highlightedClone - k2 + 1;
                int rightHighlightedClone = highlightedClone + k2 - 1;
                cloneWidth += barWidth;
                cloneY -= yDelta;

                // painting left highlighted clones
                if (leftHighlightedClone >= firstZoomedClone) {
                    drawHighlightedClone(leftHighlightedClone, clearBackground, leftCloneX, cloneY,
                            cloneWidth, arcSize, highlightYFactor, k1);
                }

                leftCloneX += k1 * barWidth;
                rightCloneX -= k1 * barWidth;

                // painting right highlighted clones
                if (rightHighlightedClone < firstZoomedClone + numZoomedClones) {
                    drawHighlightedClone(rightHighlightedClone, clearBackground, rightCloneX, cloneY,
                            cloneWidth, arcSize, highlightYFactor, k1);
                }
            }
        }

        private void drawHighlightedClone(int highlightedClone, boolean clearBackground, int cloneX, int cloneY,
                                          int cloneWidth, int arcSize, int highlightYFactor, int k) {
            Clone clone = graph.getClones().get(highlightedClone);
            if (clearBackground)
                clearHighlightedClone(cloneX, cloneY, cloneWidth, arcSize);
            else {
                drawHighlightedClone(clone.getWeight(), cloneX, cloneY, cloneWidth,
                        clone.getWeight() * highlightYFactor * (100 + k * ZOOM_Y_SCALE) / 100, arcSize);
                drawGrayHighlightedArcs(highlightedClone, cloneX + cloneWidth / 2, cloneY);
            }
        }

        private static void drawHighlightedClone(int weight, int barX, int barY, int barWidth, int barHeight, int arcSize) {
            int colorIndex = graph.getFragmentColorIndex(weight);
            // fill bar
            g2.setColor(Color.decode(SIMILITUDE[colorIndex]));
            g2.fillRoundRect(barX, barY, barWidth, barHeight, arcSize, arcSize);
            // outline bar
            g2.setColor(borderColor);
            g2.drawRoundRect(barX, barY, barWidth, barHeight, arcSize, arcSize);
        }

        private static void clearHighlightedClone(int barX, int barY, int barWidth, int arcSize) {
            // clear background
            g2.setColor(g2.getBackground());
            g2.fillRoundRect(barX, barY, barWidth, zoomedRect.y - barY + 2, arcSize, arcSize);
        }

        private void drawBrace() {
            GeneralPath zoomBrace = getZoomBrace();
            GradientPaint gradientPaint =
                    new GradientPaint(
                            edgePanelRect.width / 2f,
                            bracePath[3].y,
                            borderColor,
                            edgePanelRect.width / 2f,
                            bracePath[0].y,
                            getParent().getBackground());
            g2.setPaint(gradientPaint);
            g2.fill(zoomBrace);
        }

        private void drawArcs(int firstZoomedClone, int cloneCenterX, int cloneCenterY, int barWidth) {
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(NON_SELECTED_STROKE));
            for (int c = 0; c < numZoomedClones; c++) {
                int idx = firstZoomedClone + c;
                Clone clone = graph.getClones().get(idx);
                Color cloneColor = Color.decode(SIMILITUDE[graph.getFragmentColorIndex(clone.getWeight())]);
                drawCloneArcs(clone, cloneCenterX, cloneCenterY, cloneColor);
                cloneCenterX += barWidth;
            }
            g2.setStroke(stroke);
        }

        private void drawCloneArcs(Clone clone, int cloneCenterX, int cloneCenterY, Color cloneColor) {
            for (int f = 0; f < clone.getNumberOfFragments(); f++) {
                Fragment fragment = graph.getFragments().get(clone.getFragments().get(f).getFragment());
                if (fragment.getFragment() <= codePanelRect.width) {
                    Color fragmentColor = Color.decode(SIMILITUDE[graph.getFragmentColorIndex(fragment.getWeight())]);
                    GradientPaint gradientPaint =
                            new GradientPaint(
                                    0,
                                    cloneCenterY,
                                    cloneColor,
                                    0,
                                    0,
                                    fragmentColor);
                    drawFragmentArc(fragment, cloneCenterX, cloneCenterY, gradientPaint);
                }
            }
        }

        private static void drawFragmentArc(Fragment fragment, int cloneCenterX, int cloneCenterY, Paint gradientPaint) {
            Paint paint = g2.getPaint();
            g2.setPaint(gradientPaint);
            int ccY = cloneCenterY - cloneCenterY / 100;
            g2.drawLine(cloneCenterX, cloneCenterY, cloneCenterX, ccY);
            CubicCurve2D curve = new CubicCurve2D.Float();
            curve.setCurve(
                    cloneCenterX, ccY,
                    cloneCenterX, ccY / 2,
                    fragment.getFragment(), ccY / 2,
                    fragment.getFragment(), 0);
            g2.draw(curve);
            g2.setPaint(paint);
        }

        private void drawGrayedArcs(int firstZoomedClone, int highlightedClone, int cloneCenterX, int cloneCenterY, int barWidth) {
            int K_WIDTH = (ZOOM_MAX_K * ZOOM_MAX_K - 2 * ZOOM_MAX_K + 2);
            int K_WIDTH_2 = K_WIDTH / 2;
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(NON_SELECTED_STROKE));
            for (int c = 0; c < numZoomedClones; c++) {
                int idx = firstZoomedClone + c;
                if (Math.abs(idx - highlightedClone + 1) > K_WIDTH_2) {
                    Clone clone = graph.getClones().get(idx);
                    for (int f = 0; f < clone.getNumberOfFragments(); f++) {
                        Fragment fragment = graph.getFragments().get(clone.getFragments().get(f).getFragment());
                        if (fragment.getFragment() <= codePanelRect.width) {
                            drawFragmentArc(fragment, cloneCenterX, cloneCenterY, borderColor);
                        }
                    }
                }
                cloneCenterX += barWidth;
            }
            g2.setStroke(stroke);
        }

        private void drawHighlightedArcs(int highlightedClone, int cloneCenterX, int cloneCenterY) {
            int weight = graph.getClones().get(highlightedClone).getWeight();
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(SELECTED_STROKE));
            Clone clone = graph.getClones().get(highlightedClone);
            Color cloneColor = Color.decode(SIMILITUDE[graph.getFragmentColorIndex(weight)]);
            drawCloneArcs(clone, cloneCenterX, cloneCenterY, cloneColor);
            g2.setStroke(stroke);
        }

        private void drawGrayHighlightedArcs(int highlightedClone, int cloneCenterX, int cloneCenterY) {
            int weight = graph.getClones().get(highlightedClone).getWeight();
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(NON_SELECTED_STROKE));
            Clone clone = graph.getClones().get(highlightedClone);
            drawCloneArcs(clone, cloneCenterX, cloneCenterY, borderColor);
            g2.setStroke(stroke);
        }

        @NotNull
        private static GeneralPath getZoomBrace() {
            GeneralPath zoomBrace = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            zoomBrace.moveTo(bracePath[0].x, bracePath[0].y);
            zoomBrace.curveTo(bracePath[1].x, bracePath[1].y, bracePath[2].x, bracePath[2].y,bracePath[3].x, bracePath[3].y);
            zoomBrace.lineTo(bracePath[4].x, bracePath[4].y);
            zoomBrace.curveTo(bracePath[5].x, bracePath[5].y, bracePath[6].x, bracePath[6].y,bracePath[7].x, bracePath[7].y);
            zoomBrace.closePath();
            return zoomBrace;
        }

        @NotNull
        private static GeneralPath getZoomBraces() {
            GeneralPath zoomBrace = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            zoomBrace.moveTo(bracePath[0].x, bracePath[0].y);
            zoomBrace.curveTo(bracePath[1].x, bracePath[1].y, bracePath[2].x, bracePath[2].y,bracePath[3].x, bracePath[3].y);
            zoomBrace.lineTo(bracePath[4].x, bracePath[4].y);
            zoomBrace.curveTo(bracePath[5].x, bracePath[5].y, bracePath[6].x, bracePath[6].y,bracePath[7].x, bracePath[7].y);
            zoomBrace.closePath();
            return zoomBrace;
        }

        Color translucent(Color color, int alpha) {
            return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha),
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        }
    }

    private static class ClonePanel extends JPanel implements ComponentListener {

        private boolean dragOn = false;

        private EdgePanel edgePanel = null;

        public EdgePanel getEdgePanel() {
            return edgePanel;
        }

        public void setEdgePanel(EdgePanel edgePanel) {
            this.edgePanel = edgePanel;
        }

        public ClonePanel(int minHeight) {
            super();
            setOpaque(true);
            this.setPreferredSize(new Dimension(0, minHeight));
            this.addComponentListener(this);
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                    int pointedToClone = findPointedToClone(e);
                    if (pointedToClone != -1) {
                        dragOn = true;
                        firstZoomedClone = pointedToClone;
                        sliderRect = new Rectangle(firstZoomedClone, 0, numZoomedClones, STRIPE_HEIGHT - 1);
                        edgePanel.recalculate();
                        edgePanel.repaint();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    super.mouseReleased(e);
                    dragOn = false;
                }
            });
            this.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    super.mouseClicked(e);
                    if (dragOn) {
                        int draggedToClone = findDraggedToClone(e);
                        if (draggedToClone != -1 && draggedToClone != firstZoomedClone) {
                            firstZoomedClone = draggedToClone;
                            sliderRect = new Rectangle(firstZoomedClone, 0, numZoomedClones, STRIPE_HEIGHT - 1);
                            edgePanel.recalculate();
                            edgePanel.repaint();
                            repaint();
                        }
                    }
                }
            });
        }

        private int findPointedToClone(MouseEvent e) {
            int pointedToClone = -1;
            if (clonesPanelRect.contains(e.getPoint())) {
                pointedToClone = e.getPoint().x - numZoomedClones / 2;
                pointedToClone = Math.max(pointedToClone, 0);
                pointedToClone = Math.min(pointedToClone, clonesPanelRect.width - numZoomedClones);
            }
            return pointedToClone;
        }

        private int findDraggedToClone(MouseEvent e) {
            int draggedToClone = e.getPoint().x - numZoomedClones / 2;
            draggedToClone = Math.max(draggedToClone, 0);
            draggedToClone = Math.min(draggedToClone, clonesPanelRect.width - numZoomedClones);
            return draggedToClone;
        }

        public void componentMoved(ComponentEvent event) {

        }

        public void componentResized(ComponentEvent event) {
            sliderRect = new Rectangle(firstZoomedClone, 0, numZoomedClones, STRIPE_HEIGHT - 1);
            clonesPanelRect = new Rectangle(0, 0, getWidth(), getHeight());
        }

        public void componentShown(ComponentEvent event) {

        }

        public void componentHidden(ComponentEvent event) {

        }

        Color translucent(Color color, int alpha) {
            return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha),
                               new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (clonesPanelRect != null && getX() >= 0 && getY() >= STRIPE_HEIGHT) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(clonesPanelRect);
                paintPanelBackground();
                for (int i = 0; i < clonesPanelRect.width; i++) {
                    int height = graph.getClones().get(i).getWeight();
                    int colorIndex = graph.getFragmentColorIndex(height);
                    if (firstZoomedClone <= i && i < firstZoomedClone + numZoomedClones)
                        g2.setColor(Color.decode(SIMILITUDE[colorIndex]));
                    else
                        g2.setColor(translucent(Color.decode(SIMILITUDE[colorIndex]), 64));
                    g2.drawLine(i, 0, i, height);
                }
                paintSlider();
                g2.dispose();
            }
        }

        private void paintPanelBackground() {
            g2.setColor(this.getParent().getBackground());
            g2.fillRect(clonesPanelRect.x, clonesPanelRect.y, clonesPanelRect.width, clonesPanelRect.height);
        }

        private void paintSlider() {
            g2.setColor(borderColor);
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(3L));
            g2.drawRect(sliderRect.x, sliderRect.y, sliderRect.width, sliderRect.height);
            g2.setStroke(stroke);
        }
    }
}

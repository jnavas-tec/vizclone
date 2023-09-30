package cr.ac.tec.vizClone;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import cr.ac.tec.vizClone.model.CloneGraph;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.GeneralPath;
import java.util.Random;

public class VizCloneToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**/
    // Dark colors
    private static final String GREEN = "#a0b4a1";
    private static final String YELLOW = "#8aa18b";
    private static final String ORANGE = "#5e7e60";
    private static final String RED = "#335c38";
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
    private static final int GRAPH_HEIGHT = 60;  // percent
    private static final int ZOOM_HEIGHT = 20;   // percent
    private static final int BRACE_HEIGHT = 20;  // percent
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
    private static int zoomCenter = 0;
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
            contentPanel.add(edgePanel, BorderLayout.CENTER);
            contentPanel.add(clonePanel, BorderLayout.SOUTH);
            edgePanel.setClonePanel(clonePanel);
        }

        public JPanel getContentPanel() { return contentPanel; }
    }

    private static class CodePanel extends JPanel implements ComponentListener {

        Random r = new Random();

        int[] heights = null;
        Color[] colors = null;



        public CodePanel(int minHeight) {
            super();
            setOpaque(false);
            this.setPreferredSize(new Dimension(0, minHeight));
            this.addComponentListener(this);
        }

        public void componentMoved(ComponentEvent event) {

        }

        public void componentResized(ComponentEvent event) {

            if (heights == null) {
                heights = new int[numCodeFragments];
                colors = new Color[numCodeFragments];
                for (int i = 0; i < numCodeFragments; i++) {
                    int colorIdx = getNextRandom(0, 4);
                    colors[i] = Color.decode(SIMILITUDE[colorIdx]);
                    heights[i] = getNextRandom(1, 5) + colorIdx * 5;
                }
            }


            codePanelRect = new Rectangle(0, 0, getWidth(), getHeight());
        }

        public void componentShown(ComponentEvent event) {

        }

        public void componentHidden(ComponentEvent event) {

        }

        int getNextRandom(int low, int high) {
            return r.nextInt(high - low) + low;
        }

        Color translucent(Color color, int alpha) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (codePanelRect != null && getX() >= 0 && getY() >= 0) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(null);
                //codePanelRect = new Rectangle(0, 0, getWidth(), getHeight());
                paintPanelBackground();
                for (int i = 0; i < codePanelRect.width; i++) {
                    /*
                    int colorIdx = getNextRandom(0, 4);
                    colors[i] = Color.decode(SIMILITUDE[colorIdx]);
                    heights[i] = getNextRandom(1, 5) + colorIdx * 5;
                    */
                    int height = graph.getFragments().get(i).getWeight();
                    g2.setColor(Color.decode(SIMILITUDE[(height - 1) / 4]));
                    g2.drawLine(i, STRIPE_HEIGHT - height, i, STRIPE_HEIGHT);
                }
                //super.paintComponent(g);
            }
        }

        private void paintPanelBackground() {
            g2.setColor(this.getParent().getBackground());
            g2.fillRect(codePanelRect.x, codePanelRect.y, codePanelRect.width, codePanelRect.height);
        }
    }

    private static class EdgePanel extends JPanel implements ComponentListener {

        private ClonePanel clonePanel = null;

        public EdgePanel() {
            super();
            setOpaque(false);
            this.addComponentListener(this);
        }

        public void componentMoved(ComponentEvent event) {

        }

        public void componentResized(ComponentEvent event) {
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
            if (clonesPanelRect != null && edgePanelRect != null && getX() >= 0 && getY() >= 0) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = this.getParent().getBackground();
                g2.setColor(color);
                g2.setClip(null);
                g2.fillRect(clonesPanelRect.x, clonesPanelRect.y, clonesPanelRect.width, clonesPanelRect.height);
                paintZoomedSelection();
                super.paintComponent(g);
            }
        }

        private void paintZoomedSelection() {
            int firstZoomedClone = zoomCenter - (numZoomedClones / 2);
            int barX = zoomedRect.x;
            int barY = zoomedRect.y;
            int barWidth = zoomedRect.width / numZoomedClones;
            int arcSize = barWidth * 2 / 3;

            // draw zoomed-out clones
            for (int i = 0; i < numZoomedClones; i++) {
                int idx = firstZoomedClone + i;
                int barHeight = clonePanel.heights[idx] * zoomedRect.height / STRIPE_HEIGHT;
                Color color = clonePanel.colors[idx];
                g2.setColor(color);
                g2.fillRoundRect(barX, barY, barWidth, barHeight, arcSize, arcSize);
                g2.setColor(borderColor);
                g2.drawRoundRect(barX, barY, barWidth, barHeight, arcSize, arcSize);
                barX += barWidth;
            }

            // draw brace
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
    }

    private static class ClonePanel extends JPanel implements ComponentListener {

        Random r = new Random();
        int[] heights = null;
        Color[] colors = null;

        public ClonePanel(int minHeight) {
            super();
            setOpaque(false);
            this.setPreferredSize(new Dimension(0, minHeight));
            this.addComponentListener(this);
        }

        public void componentMoved(ComponentEvent event) {

        }

        public void componentResized(ComponentEvent event) {
            sliderRect = new Rectangle((getWidth() - numZoomedClones) / 2, 0, numZoomedClones, STRIPE_HEIGHT - 1);
            if (heights == null) {
                heights = new int[numCodeFragments];
                colors = new Color[numCodeFragments];
                for (int i = 0; i < numCodeFragments; i++) {
                    int colorIdx = getNextRandom(0, 4);
                    if (Math.abs(getWidth() / 2 - i) > 100)
                        colors[i] = translucent(Color.decode(VizCloneToolWindowFactory.SIMILITUDE[colorIdx]), 64);
                    else
                        colors[i] = Color.decode(VizCloneToolWindowFactory.SIMILITUDE[colorIdx]);
                    heights[i] = getNextRandom(1, 5) + colorIdx * 5;
                }
            }
            zoomCenter = getWidth() / 2;
            clonesPanelRect = new Rectangle(0, 0, getWidth(), getHeight());
        }

        public void componentShown(ComponentEvent event) {

        }

        public void componentHidden(ComponentEvent event) {

        }

        int getNextRandom(int low, int high) {
            return r.nextInt(high - low) + low;
        }

        Color translucent(Color color, int alpha) {
            return new JBColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha),
                               new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (clonesPanelRect != null && getX() >= 0 && getY() >= STRIPE_HEIGHT) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(null);
                paintPanelBackground();
                for (int i = 0; i < getWidth(); i++) {
                    g2.setColor(colors[i]);
                    g2.drawLine(i, 0, i, heights[i]);
                }
                paintSlider();
                super.paintComponent(g);
            }
        }

        private void paintPanelBackground() {
            g2.setColor(this.getParent().getBackground());
            g2.fillRect(clonesPanelRect.x, clonesPanelRect.y, clonesPanelRect.width, clonesPanelRect.height);
        }

        private void paintSlider() {
            g2.setColor(borderColor); //this.getParent().getForeground().darker()); //Color.GRAY.brighter());
            g2.drawRect(sliderRect.x, sliderRect.y, sliderRect.width, sliderRect.height);
        }
    }
}

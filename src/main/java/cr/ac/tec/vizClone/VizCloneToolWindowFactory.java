package cr.ac.tec.vizClone;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.GeneralPath;
import java.util.Random;

public class VizCloneToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final String GREEN = "#638409"; //"#8CD47E";
    private static final String YELLOW = "#b59d32"; //"#F8D66D";
    private static final String ORANGE = "#d67328"; //""#FFB54C";
    private static final String RED = "#890c16"; //"#FF6961";
    private static final String[] SIMILITUDE = { GREEN, YELLOW, ORANGE, RED };
    private static final int STRIPE_HEIGHT = 20;
    private static final JBColor borderColor = new JBColor(Color.LIGHT_GRAY, Color.GRAY);
    private static Graphics2D g2 = null;
    private static Rectangle zoomedRect = null;
    private static Rectangle sliderRect = null;
    private static Rectangle clonesPanelRect = null;
    private static Rectangle codePanelRect = null;
    private static Rectangle edgePanelRect = null;
    private static int zoomCenter = 0;
    private static int numCodeFragments = 3000;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
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
            //contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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
                    g2.setColor(colors[i]);
                    g2.drawLine(i, STRIPE_HEIGHT - heights[i], i, STRIPE_HEIGHT);
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

        private Point getZoomFrom() {
            Point loc = new Point();
            loc.setLocation(getWidth() % 200 / 2, getHeight() / 5 * 3);
            return loc;
        }

        private Dimension getZoomTo() {
            Dimension dim = new Dimension();
            dim.setSize(getWidth(), getHeight() / 5);
            return dim;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (clonesPanelRect != null && edgePanelRect != null && getX() >= 0 && getY() >= 0) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = this.getParent().getBackground();
                g2.setColor(color);
                g2.setClip(null);
                g2.fillRect(0, 0, getWidth(), getHeight());
                zoomedRect = new Rectangle(getWidth() % 200 / 2, getHeight() * 3 / 5, getWidth() / 200 * 200, getHeight() * 3 / 10);
                paintZoomedSelection();
                super.paintComponent(g);
            }
        }

        private void paintZoomedSelection() {
            g2.setColor(borderColor);
            g2.drawRect(zoomedRect.x - 1, zoomedRect.y - 1, zoomedRect.width + 2, zoomedRect.height + 2);
            for (int i = 0; i < 200; i++) {
                int idx = zoomCenter - 100 + i;
                Color color = clonePanel.colors[idx];
                int height = clonePanel.heights[idx];
                int barWidth = zoomedRect.width / 200;
                g2.setColor(color);
                g2.fillRect(zoomedRect.x + i * barWidth, zoomedRect.y, barWidth, height * zoomedRect.height / STRIPE_HEIGHT);
                g2.setColor(borderColor);
                g2.drawRect(zoomedRect.x + i * barWidth, zoomedRect.y, barWidth, height * zoomedRect.height / STRIPE_HEIGHT);
            }
            int[] xPoints = {
                zoomedRect.x - 1,
                zoomedRect.x + zoomedRect.width + 1,
                zoomedRect.x + zoomedRect.width + 1,
                sliderRect.x + sliderRect.width + 1,
                sliderRect.x + sliderRect.width + 1,
                sliderRect.x,
                sliderRect.x,
                zoomedRect.x
            };
            int[] yPoints = {
                zoomedRect.y + zoomedRect.height + 1,
                zoomedRect.y + zoomedRect.height + 1,
                (zoomedRect.y + zoomedRect.height + 1 + edgePanelRect.height) / 2,
                (zoomedRect.y + zoomedRect.height + 1 + edgePanelRect.height) / 2,
                edgePanelRect.height - 1,
                edgePanelRect.height - 1,
                (zoomedRect.y + zoomedRect.height + 1 + edgePanelRect.height) / 2,
                (zoomedRect.y + zoomedRect.height + 1 + edgePanelRect.height) / 2
            };
            for (int i = 0; i < 8; i++) {
                System.out.printf("(%d, %d)\n", xPoints[i], yPoints[i]);
            }
            System.out.printf("edgePanelRect (%d, %d)\n", edgePanelRect.width, edgePanelRect.height);
            System.out.printf("zoomedRect (%d, %d)\n", zoomedRect.width, zoomedRect.height);
            GeneralPath zoomBrace = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            //zoomBrace.moveTo(xPoints[0], yPoints[0]);
            zoomBrace.moveTo(xPoints[1], yPoints[1]);
            zoomBrace.curveTo(xPoints[2], yPoints[2], xPoints[3], yPoints[3], xPoints[4], yPoints[4]);
            zoomBrace.lineTo(xPoints[5], yPoints[5]);
            zoomBrace.curveTo(xPoints[6], yPoints[6], xPoints[7], yPoints[7], xPoints[0], yPoints[0]);
            zoomBrace.closePath();
            GradientPaint gradientPaint =
                    new GradientPaint(
                            edgePanelRect.width / 2f,
                            edgePanelRect.height,
                            getParent().getBackground(),
                            edgePanelRect.width / 2f,
                            yPoints[0],
                            borderColor);
            g2.setPaint(gradientPaint);
            g2.fill(zoomBrace);
            //g2.setPaint(Color.BLACK);
            //g2.draw(zoomBrace);
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
                sliderRect = new Rectangle(zoomCenter - 100, 0, 200, STRIPE_HEIGHT - 1);
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

package cr.ac.tec.vizClone;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Random;

public class VizCloneToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final String GREEN = "#638409"; //"#8CD47E";
    private static final String YELLOW = "#b59d32"; //"#F8D66D";
    private static final String ORANGE = "#d67328"; //""#FFB54C";
    private static final String RED = "#890c16"; //"#FF6961";
    private static final String[] SIMILITUDE = { GREEN, YELLOW, ORANGE, RED };
    private static final int STRIPE_HEIGHT = 20;
    private static Graphics2D g2 = null;
    private static Rectangle zoomedRect = null;
    private static Rectangle sliderRect = null;
    private static Rectangle clonesPanelRect = null;
    private static Rectangle codePanelRect = null;
    private static Rectangle edgePanelRect = null;
    private static int zoomCenter = 0;

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

            // Looks like this is triggered lots of times, be careful!!!
            System.out.println(event.getComponent().getClass().getName());
            heights = new int[getWidth()];
            colors = new Color[getWidth()];
            for (int i = 0; i < getWidth(); i++) {
                int colorIdx = getNextRandom(0, 4);
                colors[i] = Color.decode(SIMILITUDE[colorIdx]);
                heights[i] = getNextRandom(1, 5) + colorIdx * 5;
            }
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
            if (getX() >= 0 && getY() >= 0) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(null);
                g2.setColor(this.getParent().getBackground());
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (heights == null) {
                    heights = new int[getWidth()];
                    colors = new Color[getWidth()];
                    for (int i = 0; i < getWidth(); i++) {
                        int colorIdx = getNextRandom(0, 4);
                        colors[i] = Color.decode(SIMILITUDE[colorIdx]);
                        heights[i] = getNextRandom(1, 5) + colorIdx * 5;
                    }
                }
                for (int i = 0; i < getWidth(); i++) {
                    g2.setColor(colors[i]);
                    g2.drawLine(i, STRIPE_HEIGHT - heights[i], i, STRIPE_HEIGHT);
                }
                //super.paintComponent(g);
            }
        }
    }

    private static class EdgePanel extends JPanel {

        private ClonePanel clonePanel = null;

        public EdgePanel() {
            super();
            setOpaque(false);
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
            if (getX() >= 0 && getY() >= 0) {
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = this.getParent().getBackground();
                //GradientPaint gradientPaint = new GradientPaint(0, 0, Color.decode("#1CB5E0"), 0, getHeight(), Color.decode("#000046"));
                //g2.setPaint(gradientPaint);
                g2.setColor(color);
                g2.setClip(null);
                g2.fillRect(0, 0, getWidth(), getHeight());
                zoomedRect = new Rectangle(getWidth() % 200 / 2, getHeight() * 3 / 5, getWidth() / 200 * 200, getHeight() * 3 / 10);
                paintZoomedSelection();
                super.paintComponent(g);
            }
        }

        private void paintZoomedSelection() {
            g2.setColor(Color.GRAY);
            g2.setClip(null);
            g2.drawRect(zoomedRect.x, zoomedRect.y, zoomedRect.width, zoomedRect.height);
        }
    }

    private static class ClonePanel extends JPanel {

        public ClonePanel(int minHeight) {
            super();
            setOpaque(false);
            this.setPreferredSize(new Dimension(0, minHeight));
        }

        Random r = new Random();
        int[] heights = null;
        Color[] colors = null;

        int getNextRandom(int low, int high) {
            return r.nextInt(high - low) + low;
        }

        Color translucent(Color color, int alpha) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getX() >= 0 && getY() >= STRIPE_HEIGHT) {
                zoomCenter = getWidth() / 2;
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(null);
                clonesPanelRect = new Rectangle(0, 0, getWidth(), getHeight());
                paintPanelBackground();
                if (heights == null) {
                    heights = new int[getWidth()];
                    colors = new Color[getWidth()];
                    for (int i = 0; i < getWidth(); i++) {
                        int colorIdx = getNextRandom(0, 4);
                        if (Math.abs(zoomCenter - i) > 100)
                            colors[i] = translucent(Color.decode(SIMILITUDE[colorIdx]), 64);
                        else
                            colors[i] = Color.decode(SIMILITUDE[colorIdx]);
                        heights[i] = getNextRandom(1, 5) + colorIdx * 5;                    }
                }
                for (int i = 0; i < getWidth(); i++) {
                    g2.setColor(colors[i]);
                    g2.drawLine(i, 0, i, heights[i]);
                }
                sliderRect = new Rectangle(zoomCenter - 100, 0, 200, STRIPE_HEIGHT - 1);
                super.paintComponent(g);
            }
        }

        private void paintPanelBackground() {
            g2.setColor(this.getParent().getBackground());
            g2.fillRect(clonesPanelRect.x, clonesPanelRect.y, clonesPanelRect.width, clonesPanelRect.height);
        }

        private void paintSlider() {
            g2.setColor(Color.GRAY);
            g2.setClip(null);
            g2.drawRect(sliderRect.x, sliderRect.y, sliderRect.width, sliderRect.height);
        }
    }
}

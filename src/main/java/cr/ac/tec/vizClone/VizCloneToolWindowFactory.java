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
import java.util.Random;

public class VizCloneToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final String GREEN = "#8CD47E";
    private static final String YELLOW = "#F8D66D";
    private static final String ORANGE = "#FFB54C";
    private static final String RED = "#FF6961";

    private static final String[] SIMILITUDE = { GREEN, YELLOW, ORANGE, RED };

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        VizCloneToolWindowContent toolWindowContent = new VizCloneToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class VizCloneToolWindowContent {

        private final JPanel contentPanel = new JPanel();
        private final CodePanel codePanel = new CodePanel(20);
        private final EdgePanel edgePanel = new EdgePanel();
        private final ClonePanel clonePanel = new ClonePanel(20);

        public VizCloneToolWindowContent(ToolWindow toolWindow) {
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(codePanel, BorderLayout.NORTH);
            contentPanel.add(edgePanel, BorderLayout.CENTER);
            contentPanel.add(clonePanel, BorderLayout.SOUTH);
            //contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        public JPanel getContentPanel() { return contentPanel; }
    }

    private static class CodePanel extends JPanel {

        public CodePanel(int minHeight) {
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

        @Override
        protected void paintComponent(Graphics g) {
            if (getX() >= 0 && getY() >= 0) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                //Color color = Color.BLUE;// this.getParent().getBackground();
                //GradientPaint gradientPaint = new GradientPaint(0, 0, Color.decode("#1CB5E0"), 0, getHeight(), Color.decode("#000046"));
                //g2.setPaint(gradientPaint);
                //g2.setColor(color.darker());
                g2.setClip(null);
                if (heights == null) {
                    heights = new int[getWidth()];
                    colors = new Color[getWidth()];
                    for (int i = 0; i < getWidth(); i++) {
                        int colorIdx = getNextRandom(0, 4);
                        colors[i] = Color.decode(VizCloneToolWindowFactory.SIMILITUDE[colorIdx]);
                        heights[i] = getNextRandom(1, 5) + colorIdx * 5;                    }
                }
                for (int i = 0; i < getWidth(); i++) {
                    g2.setColor(colors[i]);
                    g2.drawLine(i, 20-heights[i], i, 20);
                }
                //g2.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        }
    }

    private static class EdgePanel extends JPanel {

        public EdgePanel() {
            super();
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getX() >= 0 && getY() >= 0) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = Color.RED;// this.getParent().getBackground();
                //GradientPaint gradientPaint = new GradientPaint(0, 0, Color.decode("#1CB5E0"), 0, getHeight(), Color.decode("#000046"));
                //g2.setPaint(gradientPaint);
                g2.setColor(color.darker());
                g2.setClip(null);
                g2.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        }
    }

    private static class ClonePanel extends JPanel {

        public ClonePanel(int minHeight) {
            super();
            setOpaque(false);
            this.setPreferredSize(new Dimension(0, minHeight));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getX() >= 0 && getY() >= 20) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = Color.GREEN;// this.getParent().getBackground();
                //GradientPaint gradientPaint = new GradientPaint(0, 0, Color.decode("#1CB5E0"), 0, getHeight(), Color.decode("#000046"));
                //g2.setPaint(gradientPaint);
                g2.setColor(color.darker());
                g2.setClip(null);
                g2.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        }
    }
}

package cr.ac.tec.vizClone;

import com.intellij.execution.services.ServiceViewToolWindowFactory;
import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.*;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import cr.ac.tec.vizClone.model.Clone;
import cr.ac.tec.vizClone.model.CloneGraph;
import cr.ac.tec.vizClone.model.ClonePair;
import cr.ac.tec.vizClone.model.Fragment;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.function.BiConsumer;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

@Service(Service.Level.PROJECT)
public final class VizCloneToolWindowFactory implements DumbAware {
    private Project myProject;
    private ContentManager myContentManager;

    // Dark colors
    private static final String GREEN = "#a0b4a1";
    private static final String YELLOW = "#8aa18b";
    private static final String ORANGE = "#5e7e60";
    private static final String RED = "#335c38";

    // Stroke widths
    private static final float NON_SELECTED_STROKE = 0.5F;
    private static final float SELECTED_STROKE = 2.5F;

    private static final String[] SIMILITUDE = { GREEN, YELLOW, ORANGE, RED };
    private static final int STRIPE_HEIGHT = 20;   // pixels
    private static final int GRAPH_HEIGHT = 55;    // percent
    private static final int ZOOM_HEIGHT = 30;     // percent
    //private static final int BRACE_HEIGHT = 15;  // percent - implicit
    private static final int ZOOM_Y_OFFSET = 15;   // percent
    private static final int ZOOM_Y_GROW = 5;      // percent
    private static final int ZOOM_Y_SCALE = 4;     // percent
    private static final int ZOOM_MAX_K = 3;       // horizontal increase factor - KEEP IT ODD
    private static final int MIN_ZOOMED_CLONES = 100;
    private static final int MAX_ZOOMED_CLONES = 200;
    private static final int SLIDER_CONTROL_POINT_OFFSET = 20;
    private static final int MAX_BAR_WIDTH = 25;
    private static final int MAX_ZOOMED_BAR_WIDTH = 50;
    private static final int HORIZONTAL_PAD = 3;
    private static final int VERTICAL_GAP_FACTOR = 20;
    private static final int VERTICAL_PAD = 2;
    private static final int LABEL_ARC_SIZE = 7;

    private static final JBColor borderColor = new JBColor(Color.LIGHT_GRAY, Color.GRAY.darker());
    private static final JBColor labelColor = new JBColor(Color.GRAY, Color.LIGHT_GRAY);
    private static Graphics2D g2 = null;
    private static Rectangle codePanelRect = null;
    private static Rectangle edgePanelRect = null;
    private static Rectangle graphRect = null;
    private static Rectangle zoomedRect = null;
    private static Point[] bracePath =
            {new Point(),new Point(),new Point(),new Point(),new Point(),new Point(),new Point(),new Point()};
    private static Rectangle clonesPanelRect = null;
    private static Rectangle sliderRect = null;
    private static int firstZoomedCloneX = 0;
    private static int firstZoomedClone = 0;
    private static int numCodeFragments = 150;
    private static int numCodeClones = 150;
    private static int numZoomedClones = MAX_ZOOMED_CLONES;
    private static int codePanelWidth;
    private static int codePanelBars;
    private static int codePanelBarWidth;
    private static int codePanelFragmentsPerBar;
    private static int clonePanelWidth;
    private static int clonePanelBars;
    private static int clonePanelBarWidth;
    private static int clonePanelClonesPerBar;

    private static CloneGraph graph = null;
    private CloneCollector myCollector;

    public VizCloneToolWindowFactory(@NotNull Project p) {
        super();
        this.myProject = p;
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(this.myProject);
        ToolWindow toolWindow = toolWindowManager.registerToolWindow("VizClones", builder -> {
            builder.contentFactory = new ServiceViewToolWindowFactory();
            builder.icon = AllIcons.Actions.ToggleVisibility;
            builder.hideOnEmptyContent = false;
            builder.canCloseContent = true;
            builder.anchor = ToolWindowAnchor.BOTTOM;
            builder.stripeTitle = () -> {
                return "Clones Graph";
            };
            return Unit.INSTANCE;
        });
        this.myContentManager = toolWindow.getContentManager();
        ContentManagerWatcher.watchContentManager(toolWindow, this.myContentManager);
    }

    public static VizCloneToolWindowFactory getInstance(@NotNull Project project, CloneCollector collector, ArrayList<Clone> clones, ArrayList<Fragment> fragments) {
        VizCloneToolWindowFactory factory = (VizCloneToolWindowFactory)project.getService(VizCloneToolWindowFactory.class);
        factory.myProject = project;
        factory.myCollector = collector;
        factory.numCodeClones = clones.size();
        factory.numCodeFragments = fragments.size();
        factory.graph = new CloneGraph(clones, fragments);
        factory.graph.setMinWeight(75);
        factory.graph.setMaxWeight(100);
        factory.graph.setNumWeightLevels(4);
        factory.graph.fixWeights();
        return factory;
    }

    public void showVizClones() {
        PsiDocumentManager.getInstance(this.myProject).commitAllDocuments();
        final Runnable showVizClonesRunnable = () -> {
            ToolWindowManager.getInstance(this.myProject).invokeLater(() -> {
                if (graph != null && graph.getNumClones() > 0 && graph.getNumFragments() > 0) {
                    this.showClones();
                } else
                    Messages.showInfoMessage(this.myProject, "No code duplicates found.", "VizClones");
            });
        };
        showVizClonesRunnable.run();
    }

    private void showClones() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(this.myProject);
        VizCloneToolWindowContent vizCloneToolWindowContent = new VizCloneToolWindowContent(this.myProject, this.myCollector);
        Content content = ContentFactory.getInstance().createContent(vizCloneToolWindowContent.getContentPanel(), "Clones Graph", false);
        this.myContentManager.addContent(content);
        this.myContentManager.setSelectedContent(content);
        toolWindowManager.getToolWindow("VizClones").activate(new Runnable() {
            @Override
            public void run() {
                vizCloneToolWindowContent.getContentPanel().repaint();
            }
        });
    }

    private static class VizCloneToolWindowContent {

        private Project myProject;
        private CloneCollector myCollector;
        private JPanel contentPanel;
        private CodePanel codePanel;
        private EdgePanel edgePanel;
        private ClonePanel clonePanel;

        public VizCloneToolWindowContent(Project project, CloneCollector collector) {
            myProject = project;
            myCollector = collector;
            contentPanel = new JPanel();
            codePanel = new CodePanel(STRIPE_HEIGHT);
            edgePanel = new EdgePanel(myProject, myCollector);
            clonePanel = new ClonePanel(STRIPE_HEIGHT);
            numZoomedClones = numCodeClones / 5;
            if (numZoomedClones >= MAX_ZOOMED_CLONES) {
                numZoomedClones = MAX_ZOOMED_CLONES;
            }
            else if (numZoomedClones < MIN_ZOOMED_CLONES) {
                numZoomedClones = Math.min(numCodeClones, MIN_ZOOMED_CLONES);
            }
            contentPanel.setLayout(new BorderLayout());
            contentPanel.add(codePanel, BorderLayout.NORTH);
            contentPanel.add(edgePanel, BorderLayout.CENTER);
            contentPanel.add(clonePanel, BorderLayout.SOUTH);
            edgePanel.setCodePanel(codePanel);
            clonePanel.setEdgePanel(edgePanel);
            edgePanel.setClonePanel(clonePanel);
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
            if (getWidth() <= numCodeFragments) {
                codePanelBarWidth = 1;
                codePanelFragmentsPerBar = (numCodeFragments + getWidth() - 1) / getWidth();
                codePanelBars = (numCodeFragments + codePanelFragmentsPerBar - 1) / codePanelFragmentsPerBar;
                codePanelWidth = codePanelBars;
            }
            else {
                codePanelBarWidth = Math.min(MAX_BAR_WIDTH,getWidth() / numCodeFragments);
                codePanelFragmentsPerBar = 1;
                codePanelBars = numCodeFragments;
                codePanelWidth = codePanelBarWidth * codePanelBars;
            }
            codePanelRect = new Rectangle((getWidth() - codePanelWidth) / 2, 0, codePanelWidth, getHeight());
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
                drawFragments();
                g2.dispose();
            }
        }

        public void drawFragments() {
            BiConsumer<Rectangle, Color> drawFragment;
            if (codePanelRect.width <= numCodeFragments) {
                drawFragment = (rectangle, color) -> {
                    g2.setColor(color);
                    g2.drawLine(rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height);
                };
            }
            else {
                drawFragment = (rectangle, color) -> {
                    g2.setColor(color);
                    g2.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                    g2.setColor(borderColor);
                    g2.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                };
            }
            if (graph.isSelected()) {
                for (int i = 0; i < numCodeFragments; i++) {
                    boolean grayedOut = (graph.getSelectedClone() == graph.getFragments().get(i).getClone().getIdx()) ? false : true;
                    drawFragment(drawFragment, i, grayedOut);
                }
            }
            else {
                for (int i = 0; i < numCodeFragments; i++) {
                    drawFragment(drawFragment, i, false);
                }
            }
        }

        public void drawFragment(BiConsumer<Rectangle, Color> drawFragment, int fragmentIdx, boolean grayedOut) {
            int weight = graph.getFragments().get(fragmentIdx).getClonePair().getWeight();
            int colorIndex = graph.getFragmentColorIndex(weight);
            drawFragment.accept(
                new Rectangle(
                    fragmentIdx * codePanelBarWidth + codePanelRect.x,
                    STRIPE_HEIGHT - weight,
                    codePanelBarWidth,
                    weight),
                grayedOut ? borderColor : Color.decode(SIMILITUDE[colorIndex]));
        }

        private void paintPanelBackground() {
            g2.setColor(this.getParent().getBackground());
            g2.fillRect(codePanelRect.x, codePanelRect.y, codePanelRect.width, codePanelRect.height);
        }

        public Point getFragmentLocation(int fragment) {
            Point p = new Point();
            p.x = fragment * codePanelBarWidth + codePanelRect.x + codePanelBarWidth / 2;
            p.y = codePanelRect.y + codePanelRect.height;
            return p;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private static class EdgePanel extends JPanel implements ComponentListener {

        private CodePanel codePanel = null;
        private ClonePanel clonePanel = null;
        private int selectedClone = -1;
        private int hoveredClone = -1;
        private Project myProject;
        private CloneCollector myCollector;

        public EdgePanel(Project project, CloneCollector collector) {
            super();
            myProject = project;
            myCollector = collector;
            setOpaque(true);
            this.addComponentListener(this);
            this.addMouseListener(new MouseAdapter() {
                Project project = myProject;
                CloneCollector collector = myCollector;

                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    int clone = findPointedToClone(e);
                    if (isLeftMouseButton(e)) {
                        if (clone != selectedClone) {
                            selectedClone = clone;
                            graph.setSelected(true);
                            graph.setSelectedClone(selectedClone);
                            codePanel.repaint();
                        } else {
                            selectedClone = -1;
                            if (graph.isSelected()) {
                                graph.setSelected(false);
                                codePanel.repaint();
                            }
                        }
                        repaint();
                    }
                    else if (isRightMouseButton(e)) {
                        if (clone != -1) {
                            DiffCloneManager
                                .getInstance(project)
                                .showDiffClones(collector.getClones().subList(clone, clone + 1));
                        }
                    }
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
                    if (selectedClone == -1) {
                        if (hoveredClone != -1) {
                            if (!graph.isSelected() || hoveredClone != graph.getSelectedClone()) {
                                graph.setSelected(true);
                                graph.setSelectedClone(hoveredClone);
                                codePanel.repaint();
                            }
                        }
                        else if (graph.isSelected()) {
                            graph.setSelected(false);
                            codePanel.repaint();
                        }
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
            if (MAX_ZOOMED_BAR_WIDTH < getWidth() / numZoomedClones) {
                int zoomedRectWidth = MAX_ZOOMED_BAR_WIDTH * numZoomedClones;
                zoomedRect = new Rectangle(
                    (getWidth() - zoomedRectWidth) / 2,
                    getHeight() * GRAPH_HEIGHT / 100,
                    zoomedRectWidth,
                    getHeight() * ZOOM_HEIGHT / 100);
            }
            else {
                zoomedRect = new Rectangle(
                    getWidth() % numZoomedClones / 2,
                    getHeight() * GRAPH_HEIGHT / 100,
                    getWidth() / numZoomedClones * numZoomedClones,
                    getHeight() * ZOOM_HEIGHT / 100);
            }

            int zbrx = zoomedRect.x + zoomedRect.width + 1;    // zoomed bottom right
            int zblx = zoomedRect.x;                           // zoomed bottom left
            int strx = sliderRect.x + sliderRect.width + 1;    // slider top right
            int stlx = sliderRect.x;                           // slider top left
            int zcpx = zoomedRect.width / numZoomedClones;     // zoomed control point
            int scpx = sliderRect.width / SLIDER_CONTROL_POINT_OFFSET;  // slider control point

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

        public CodePanel getCodePanel() {
            return this.codePanel;
        }

        public void setCodePanel(final CodePanel codePanel) {
            this.codePanel = codePanel;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (clonesPanelRect != null && codePanelRect != null && edgePanelRect != null && getX() >= 0 && getY() >= 0) {
                Dimension size = getSize();
                g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = this.getParent().getBackground();
                g2.setColor(color);
                g2.setClip(edgePanelRect);
                g2.fillRect(clonesPanelRect.x, clonesPanelRect.y, clonesPanelRect.width, clonesPanelRect.height);
                paintZoomedInSelection();
            }
        }

        private void drawFragmentLabel(Clone clone, Fragment fragment, int fragmentOrder) {
            String label = String.format("%s: %d%% [%d-%d]", fragment.getCMethod().getName(),
                fragment.getClonePair().getSim(), fragment.getFromLineColumn().line, fragment.getToLineColumn().line);
            Point fragmentLocation = codePanel.getFragmentLocation(fragment.getIdx());
            fragmentLocation.y = 0;
            int numFragments = clone.getClonePairs().size() * 2;
            Point labelLocation = getLabelLocation(numFragments, fragmentOrder, fragmentLocation);
            drawLabelText(label, labelLocation, fragmentLocation,
                isLeftFragment(numFragments, fragmentOrder), fragment.getClonePair().getWeight());
        }

        private boolean isLeftFragment(int numFragments, int fragmentIdx) {
            return fragmentIdx < (numFragments / 2);
        }

        private Point getLabelLocation(int numFragments, int fragmentIdx, Point fragmentLocation) {
            Point p = new Point();
            FontMetrics fm = g2.getFontMetrics();
            int lineGap = fm.getHeight() / VERTICAL_GAP_FACTOR;
            int lineHeight = lineGap + fm.getHeight() + 3 * VERTICAL_PAD;
            boolean isLeft = fragmentIdx < (numFragments / 2);
            int maxLeft = numFragments / 2 - 1;
            int minRight = maxLeft + 1;
            p.x = fragmentLocation.x + (isLeft ? -codePanelBarWidth / 2 : codePanelBarWidth / 2);
            p.y = fragmentLocation.y + VERTICAL_PAD    
                + (lineGap + lineHeight) * (isLeft ? fragmentIdx : maxLeft - (fragmentIdx % minRight));
            return p;
        }

        private void drawLabelText(String label, Point labelLocation, Point fragmentLocation, boolean isLeftFragment, int weight) {
            int x2 = labelLocation.x;
            int y1 = labelLocation.y;
            int x0 = fragmentLocation.x;
            int y0 = fragmentLocation.y;

            FontMetrics fm = g2.getFontMetrics();
            int lineHeight = (fm.getHeight()) + VERTICAL_PAD + 2 * fm.getHeight() / VERTICAL_GAP_FACTOR;
            int labelWidth = fm.stringWidth(label);

            int rx = x2 + (isLeftFragment ? -labelWidth - HORIZONTAL_PAD : -HORIZONTAL_PAD);
            int ry = y1;
            int rw = labelWidth + 2 * HORIZONTAL_PAD;
            int rh = lineHeight;

            // fix label column to Panel content
            rx = Math.max(0, Math.min(rx, getWidth() - rw));

            // draw label background
            g2.setColor(g2.getBackground());
            g2.fillRoundRect(rx, ry, rw, rh, LABEL_ARC_SIZE, LABEL_ARC_SIZE);
            g2.setColor(labelColor);
            g2.drawRoundRect(rx, ry, rw, rh, LABEL_ARC_SIZE, LABEL_ARC_SIZE);

            // draw label
            int lx = rx + HORIZONTAL_PAD;
            int ly1 = ry + fm.getHeight();
            g2.setColor(labelColor);
            g2.drawString(label, lx, ly1);
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(NON_SELECTED_STROKE));
            g2.drawLine(x2, y1, x0, y0);
            g2.setStroke(stroke);
        }

        private void paintZoomedInSelection() {
            int barX = zoomedRect.x;
            int barY = zoomedRect.y;
            int barWidth = zoomedRect.width / numZoomedClones;
            int arcSize = Math.min(barWidth * 2 / 3, 5);

            // draw brace
            drawBrace();

            int cloneCenterX = zoomedRect.x + barWidth / 2;
            int cloneCenterY = barY;

            // draw arcs & zoomed-out clones
            if (selectedClone >= 0) {
                // draw grayed arcs
                drawGrayedArcs(firstZoomedClone, selectedClone, cloneCenterX, cloneCenterY, barWidth);
                // paint grayed clones with selection
                drawZoomedInClonesWithHighlight(selectedClone, firstZoomedClone, barX, barY, barWidth, arcSize);
                // draw selected Clone's fragment labels
                Clone clone = graph.getClones().get(selectedClone);
                int numFragments = clone.getNumberOfClonePairs() * 2;
                ArrayList<Fragment> sortedFragments = clone.getSortedFragments();
                for (int f = 0; f < numFragments; f++) {
                    drawFragmentLabel(clone, sortedFragments.get(f), f);
                }
            }
            else if (hoveredClone >= 0) {
                // draw grayed arcs
                drawGrayedArcs(firstZoomedClone, hoveredClone, cloneCenterX, cloneCenterY, barWidth);
                // paint grayed clones with selection
                drawZoomedInClonesWithHighlight(hoveredClone, firstZoomedClone, barX, barY, barWidth, arcSize);
                // draw selected Clone's fragment labels
                Clone clone = graph.getClones().get(hoveredClone);
                int numFragments = clone.getNumberOfClonePairs() * 2;
                ArrayList<Fragment> sortedFragments = clone.getSortedFragments();
                for (int f = 0; f < numFragments; f++) {
                    drawFragmentLabel(clone, sortedFragments.get(f), f);
                }
            }
            else {
                // no clone selected
                // draw all arcs
                drawArcs(firstZoomedClone, cloneCenterX, cloneCenterY, barWidth);
                // no clone selected - paint colored clones
                drawZoomedInClones(firstZoomedClone, barX, barY, barWidth, arcSize);
            }
        }

        private void drawZoomedInClones(int firstZoomedClone, int barX, int barY, int barWidth, int arcSize) {
            for (int i = 0; i < numZoomedClones; i++) {
                int idx = firstZoomedClone + i;
                int weight = graph.getClones().get(idx).getMaxWeight();
                int barHeight = weight * zoomedRect.height / CloneGraph.MAX_WEIGHT;
                drawHighlightedClone(idx, weight, barX, barY, barWidth, barHeight, arcSize);
                barX += barWidth;
            }
        }

        private void drawZoomedInClonesWithHighlight(
                int highlightedClone, int firstZoomedClone,
                int barX, int barY, int barWidth, int arcSize) {
            int K_WIDTH = (ZOOM_MAX_K * ZOOM_MAX_K - 2 * ZOOM_MAX_K + 2);
            int K_WIDTH_2 = K_WIDTH / 2;

            for (int i = 0, cloneX = barX; i < numZoomedClones; i++, cloneX += barWidth) {
                int idx = firstZoomedClone + i;
                if (Math.abs(idx - highlightedClone) > K_WIDTH_2) {
                    int weight = graph.getClones().get(idx).getMaxWeight();
                    int barHeight = weight * zoomedRect.height / CloneGraph.MAX_WEIGHT;
                    g2.setColor(borderColor);
                    g2.fillRoundRect(cloneX, barY, barWidth, barHeight, arcSize, arcSize);
                    g2.setColor(borderColor);
                    g2.drawRoundRect(cloneX, barY, barWidth, barHeight, arcSize, arcSize);
                }
            }

            int leftCloneX = zoomedRect.x + barWidth * (highlightedClone - firstZoomedClone - K_WIDTH_2);// - 1);
            int rightCloneX = leftCloneX + barWidth * (K_WIDTH);// - 1);
            int cloneWidth = barWidth;
            int yDelta = graphRect.height * ZOOM_Y_OFFSET / (ZOOM_MAX_K * 100);
            int cloneY = barY - yDelta;
            int highlightYFactor = zoomedRect.height * (100 + ZOOM_Y_OFFSET * 2 + ZOOM_Y_GROW) / (CloneGraph.MAX_WEIGHT * 100);

            int cloneX = zoomedRect.x + barWidth * (highlightedClone - firstZoomedClone - ZOOM_MAX_K / 2);// - 1);
            int weight = graph.getClones().get(highlightedClone).getMaxWeight();
            int barHeight = weight * highlightYFactor * (100 + ZOOM_MAX_K * ZOOM_Y_SCALE) / 100;

            // clear highlighted clones background
            paintHighlightedClones(true, highlightedClone, firstZoomedClone, barWidth, arcSize,
                    cloneWidth, cloneY, yDelta, leftCloneX, highlightYFactor, rightCloneX);

            // clear highlighted clone background
            clearHighlightedClone(cloneX, barY - yDelta * ZOOM_MAX_K, barWidth * ZOOM_MAX_K, arcSize);

            // paint colored highlighted clones
            paintHighlightedClones(false, highlightedClone, firstZoomedClone, barWidth, arcSize,
                    cloneWidth, cloneY, yDelta, leftCloneX, highlightYFactor, rightCloneX);

            int cloneCenterX = cloneX + barWidth * ZOOM_MAX_K / 2;
            int cloneCenterY = barY - yDelta * ZOOM_MAX_K;

            // paint colored central highlighted clone
            drawHighlightedClone(highlightedClone, weight, cloneX, cloneCenterY, barWidth * ZOOM_MAX_K, barHeight, arcSize);
            drawHighlightedCloneLabel(highlightedClone, weight, cloneCenterX, cloneCenterY, barWidth);

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
            //if (clearBackground) {
            //    leftCloneX -= barWidth;
            //    rightCloneX += barWidth;
            //}

            rightCloneX -= barWidth;
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
                drawHighlightedClone(highlightedClone, clone.getMaxWeight(), cloneX, cloneY, cloneWidth,
                        clone.getMaxWeight() * highlightYFactor * (100 + k * ZOOM_Y_SCALE) / 100, arcSize);
                drawGrayHighlightedArcs(highlightedClone, cloneX + cloneWidth / 2, cloneY);
            }
        }

        private static void drawHighlightedClone(int highlightedClone, int weight, int barX, int barY,
                                                 int barWidth, int barHeight, int arcSize) {
            int colorIndex = graph.getFragmentColorIndex(weight);
            // fill bar
            g2.setColor(Color.decode(SIMILITUDE[colorIndex]));
            g2.fillRoundRect(barX, barY, barWidth, barHeight, arcSize, arcSize);

            // outline bar
            g2.setColor(borderColor);
            g2.drawRoundRect(barX, barY, barWidth, barHeight, arcSize, arcSize);
        }

        private static void drawHighlightedCloneLabel(int highlightedClone, int weight, int barX, int barY, int barWidth) {
            // Clone label
            String label = String.format("Clone #%d %d%%", highlightedClone,
                graph.getClones().get(highlightedClone).getMaxSim());

            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            int lineHeight = (fm.getHeight()) + VERTICAL_PAD * 2;

            int x1 = barX - labelWidth / 2 - HORIZONTAL_PAD;
            int y1 = barY + VERTICAL_PAD;
            int rw = labelWidth + 2 * HORIZONTAL_PAD;
            int rh = lineHeight;

            // draw label background
            //g2.setColor(g2.getBackground());
            //g2.fillRoundRect(x1, y1, rw, rh, LABEL_ARC_SIZE, LABEL_ARC_SIZE);
            //g2.setColor(labelColor);
            //g2.drawRoundRect(x1, y1, rw, rh, LABEL_ARC_SIZE, LABEL_ARC_SIZE);

            // draw label
            int lx = x1 + HORIZONTAL_PAD;
            int ly1 = y1 + fm.getHeight();
            g2.setColor(Color.WHITE);
            g2.drawString(label, lx, ly1);
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

        private int findZoomedClone(int zoomedCloneX) {
            return (zoomedCloneX - clonesPanelRect.x) * clonePanelClonesPerBar / clonePanelBarWidth;
        }

        private void drawArcs(int firstZoomedClone, int cloneCenterX, int cloneCenterY, int barWidth) {
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(NON_SELECTED_STROKE));
            for (int c = 0; c < numZoomedClones; c++) {
                int idx = firstZoomedClone + c;
                if (idx == 500) {
                    int stopHere = 1;
                }
                Clone clone = graph.getClones().get(idx);
                Color cloneColor = Color.decode(SIMILITUDE[graph.getFragmentColorIndex(clone.getMaxWeight())]);
                drawCloneArcs(clone, cloneCenterX, cloneCenterY, cloneColor);
                cloneCenterX += barWidth;
            }
            g2.setStroke(stroke);
        }

        private void drawCloneArcs(Clone clone, int cloneCenterX, int cloneCenterY, Color cloneColor) {
            for (int f = 0; f < clone.getNumberOfClonePairs(); f++) {
                ClonePair clonePair = clone.getClonePairs().get(f);
                Fragment fragment0 = clonePair.getFragments().get(0);
                Fragment fragment1 = clonePair.getFragments().get(1);
                if (fragment0.getIdx() <= codePanelRect.width) {
                    Color fragmentColor = Color.decode(SIMILITUDE[graph.getFragmentColorIndex(clonePair.getWeight())]);
                    GradientPaint gradientPaint =
                            new GradientPaint(
                                    0,
                                    cloneCenterY,
                                    cloneColor,
                                    0,
                                    0,
                                    fragmentColor);
                    drawFragmentArc(fragment0, cloneCenterX, cloneCenterY, gradientPaint);
                }
                if (fragment1.getIdx() <= codePanelRect.width) {
                    Color fragmentColor = Color.decode(SIMILITUDE[graph.getFragmentColorIndex(clonePair.getWeight())]);
                    GradientPaint gradientPaint =
                            new GradientPaint(
                                    0,
                                    cloneCenterY,
                                    cloneColor,
                                    0,
                                    0,
                                    fragmentColor);
                    drawFragmentArc(fragment1, cloneCenterX, cloneCenterY, gradientPaint);
                }
            }
        }

        @SuppressWarnings("DuplicatedCode")
        private void drawGrayedCloneArcs(Clone clone, int cloneCenterX, int cloneCenterY) {
            //noinspection DuplicatedCode
            for (int f = 0; f < clone.getNumberOfClonePairs(); f++) {
                ClonePair clonePair = clone.getClonePairs().get(f);
                Fragment fragment0 = clonePair.getFragments().get(0);
                Fragment fragment1 = clonePair.getFragments().get(1);
                if (fragment0.getIdx() <= codePanelRect.width) {
                    drawFragmentArc(fragment0, cloneCenterX, cloneCenterY, borderColor);
                }
                if (fragment1.getIdx() <= codePanelRect.width) {
                    drawFragmentArc(fragment1, cloneCenterX, cloneCenterY, borderColor);
                }
            }
        }

        private void drawFragmentArc(Fragment fragment, int cloneCenterX, int cloneCenterY, Paint gradientPaint) {
            Paint paint = g2.getPaint();
            g2.setPaint(gradientPaint);
            int ccY = cloneCenterY - cloneCenterY / 100;
            g2.drawLine(cloneCenterX, cloneCenterY, cloneCenterX, ccY);
            CubicCurve2D curve = new CubicCurve2D.Float();
            Point fragmentPoint = codePanel.getFragmentLocation(fragment.getIdx());
            curve.setCurve(
                    cloneCenterX, ccY,
                    cloneCenterX, ccY / 2,
                    fragmentPoint.x, ccY / 2,
                    fragmentPoint.x, 0);
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
                if (Math.abs(idx - highlightedClone) > K_WIDTH_2) {
                    Clone clone = graph.getClones().get(idx);
                    for (int f = 0; f < clone.getNumberOfClonePairs(); f++) {
                        ClonePair clonePair = clone.getClonePairs().get(f);
                        Fragment fragment0 = clonePair.getFragments().get(0);
                        Fragment fragment1 = clonePair.getFragments().get(1);
                        if (fragment0.getIdx() <= codePanelRect.width) {
                            drawFragmentArc(fragment0, cloneCenterX, cloneCenterY, borderColor);
                        }
                        if (fragment1.getIdx() <= codePanelRect.width) {
                            drawFragmentArc(fragment1, cloneCenterX, cloneCenterY, borderColor);
                        }
                    }
                }
                cloneCenterX += barWidth;
            }
            g2.setStroke(stroke);
        }

        private void drawHighlightedArcs(int highlightedClone, int cloneCenterX, int cloneCenterY) {
            int weight = graph.getClones().get(highlightedClone).getMaxWeight();
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(SELECTED_STROKE));
            Clone clone = graph.getClones().get(highlightedClone);
            Color cloneColor = Color.decode(SIMILITUDE[graph.getFragmentColorIndex(weight)]);
            drawCloneArcs(clone, cloneCenterX, cloneCenterY, cloneColor);
            g2.setStroke(stroke);
        }

        private void drawGrayHighlightedArcs(int highlightedClone, int cloneCenterX, int cloneCenterY) {
            int weight = graph.getClones().get(highlightedClone).getMaxWeight();
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(NON_SELECTED_STROKE));
            Clone clone = graph.getClones().get(highlightedClone);
            drawGrayedCloneArcs(clone, cloneCenterX, cloneCenterY);
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
                    int pointedToCloneX = findPointedToCloneX(e);
                    if (pointedToCloneX != -1) {
                        dragOn = true;
                        firstZoomedCloneX = pointedToCloneX;
                        firstZoomedClone = findFirstZoomedClone(pointedToCloneX);
                        if (edgePanel.selectedClone < firstZoomedClone || edgePanel.selectedClone >= firstZoomedClone + numZoomedClones)
                            edgePanel.selectedClone = -1;
                        if (edgePanel.hoveredClone < firstZoomedClone || edgePanel.hoveredClone >= firstZoomedClone + numZoomedClones)
                            edgePanel.hoveredClone = -1;
                        sliderRect = new Rectangle(firstZoomedCloneX, 0, numZoomedClones * clonePanelBarWidth / clonePanelClonesPerBar, STRIPE_HEIGHT - 1);
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
                    super.mouseDragged(e);
                    if (dragOn) {
                        int draggedToCloneX = findDraggedToCloneX(e);
                        if (draggedToCloneX != -1 && draggedToCloneX != firstZoomedCloneX) {
                            firstZoomedCloneX = draggedToCloneX;
                            firstZoomedClone = findFirstZoomedClone(firstZoomedCloneX);
                            sliderRect = new Rectangle(firstZoomedCloneX, 0, numZoomedClones * clonePanelBarWidth / clonePanelClonesPerBar, STRIPE_HEIGHT - 1);
                            edgePanel.recalculate();
                            edgePanel.repaint();
                            repaint();
                        }
                    }
                }
            });
        }

        private int findFirstZoomedClone(int firstZoomedCloneX) {
            int firstZoomedClone = 0;
            if (firstZoomedCloneX != -1) {
                firstZoomedClone = (firstZoomedCloneX - clonesPanelRect.x) * clonePanelClonesPerBar/ clonePanelBarWidth;
            }
            return firstZoomedClone;
        }

        private int findPointedToCloneX(MouseEvent e) {
            int pointedToCloneX = -1;
            if (clonesPanelRect.contains(e.getPoint())) {
                pointedToCloneX = e.getPoint().x - numZoomedClones * clonePanelBarWidth / (clonePanelClonesPerBar * 2);
                pointedToCloneX = Math.max(pointedToCloneX, clonesPanelRect.x);
                pointedToCloneX = Math.min(pointedToCloneX, clonesPanelRect.x + clonesPanelRect.width - numZoomedClones * clonePanelBarWidth / clonePanelClonesPerBar);
            }
            return pointedToCloneX;
        }

        private int findDraggedToCloneX(MouseEvent e) {
            int draggedToCloneX = e.getPoint().x - numZoomedClones * clonePanelBarWidth / (clonePanelClonesPerBar * 2);
            draggedToCloneX = Math.max(draggedToCloneX, clonesPanelRect.x);
            draggedToCloneX = Math.min(draggedToCloneX, clonesPanelRect.x + clonesPanelRect.width - numZoomedClones * clonePanelBarWidth / clonePanelClonesPerBar);
            return draggedToCloneX;
        }

        public void componentMoved(ComponentEvent event) {

        }

        public void componentResized(ComponentEvent event) {
            if (getWidth() <= numCodeClones) {
                clonePanelBarWidth = 1;
                clonePanelClonesPerBar = (numCodeClones + getWidth() - 1) / getWidth();
                clonePanelBars = (numCodeClones + clonePanelClonesPerBar - 1) / clonePanelClonesPerBar;
                clonePanelWidth = clonePanelBars;
            }
            else {
                clonePanelBarWidth = Math.min(MAX_BAR_WIDTH, getWidth() / numCodeClones);
                clonePanelClonesPerBar = 1;
                clonePanelBars = numCodeClones;
                clonePanelWidth = clonePanelBarWidth * clonePanelBars;
            }
            clonesPanelRect = new Rectangle((getWidth() - clonePanelWidth) / 2, 0, clonePanelWidth, getHeight());
            sliderRect = new Rectangle(
                    clonesPanelRect.x + firstZoomedCloneX * clonePanelBarWidth / clonePanelClonesPerBar,
                    0,
                    numZoomedClones * clonePanelBarWidth / clonePanelClonesPerBar,
                    getHeight());

            //sliderRect = new Rectangle(firstZoomedClone, 0, numZoomedClones, STRIPE_HEIGHT - 1);
            //clonesPanelRect = new Rectangle(0, 0, getWidth(), getHeight());
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
                BiConsumer<Rectangle, Color> drawClone;
                if (clonesPanelRect.width <= numCodeClones) {
                    drawClone = (rectangle, color) -> {
                        g2.setColor(color);
                        g2.drawLine(rectangle.x, rectangle.y, rectangle.x + rectangle.width, rectangle.y + rectangle.height);
                    };
                }
                else {
                    drawClone = (rectangle, color) -> {
                        g2.setColor(color);
                        g2.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                        g2.setColor(borderColor);
                        g2.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                    };
                }
                int first = firstZoomedClone;
                int last = first + numZoomedClones;
                int cloneWidth = clonePanelBarWidth / clonePanelClonesPerBar;
                for (int i = 0, x = clonesPanelRect.x; i < numCodeClones; i++, x += cloneWidth) {
                    int height = graph.getClones().get(i).getMaxWeight();
                    int colorIndex = graph.getFragmentColorIndex(height);
                    Color color;
                    if (first <= i && i < last)
                        color = Color.decode(SIMILITUDE[colorIndex]);
                    else
                        color = translucent(Color.decode(SIMILITUDE[colorIndex]), 64);
                    //g2.drawLine(x, 0, x, height);
                    int x2 = clonesPanelRect.x + i * clonePanelBarWidth / clonePanelClonesPerBar;
                    drawClone.accept(
                            new Rectangle(
                                    x2,
                                    0, //STRIPE_HEIGHT - height,
                                    cloneWidth,
                                    height),
                            color);
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
            g2.drawRect(sliderRect.x, sliderRect.y, sliderRect.width - 2, sliderRect.height);
            g2.setStroke(stroke);
        }
    }
}

package cr.ac.tec.vizClone;

import cr.ac.tec.vizClone.model.*;

import java.util.ArrayList;
import java.util.List;

public class SmithWatermanGotoh {
    private int m;                // number of rows for sentences
    private int n;                // number of columns for sentences
    private CMethod methodA;      // left side method
    private CMethod methodB;      // upper side method
    private List<CStatement> a;   // rows (left side) statements
    private List<CStatement> b;   // columns (upper side) statements
    private int alpha1;           // inter sentences gap opening penalty (may be negative: reward)
    private int beta1;            // inter sentences gap penalty (may be negative: reward)
    private int reward1;          // inter sentences match reward (must be positive)
    private int penalty1;         // inter sentences mismatch penalty (must be positive)
    private int[][] h;            // score matrix for statements
    private int[][] p;            // vertical gap score matrix for statements
    private int[][] q;            // horizontal gap score matrix for statements
    private int[][] a0;           // vertical start of clone
    private int[][] b0;           // horizontal start of clone
    private boolean[][] g;        // false-match has no gaps, true-match has gaps
    private int[][] at;           // number of tokens in left sentences
    private int[][] bt;           // number of tokens in upper sentences
    private double[][] s;         // similitude in range 0..1000 for sentences
    private int maxt;             // maximum number of tokens in any sentence
    private int m2;               // number of rows for tokens
    private int n2;               // number of columns for tokens
    private int alpha2;           // inter tokens gap opening penalty (may be negative: reward)
    private int beta2;            // inter tokens gap penalty (may be negative: reward)
    private int reward2;          // inter tokens match reward (must be positive)
    private int penalty2;         // inter tokens mismatch penalty (must be positive)
    private int[][] h2;           // score matrix for tokens
    private int[][] p2;           // vertical gap score matrix for tokens
    private int[][] q2;           // horizontal gap score matrix for tokens
    private double[][] s2;        // similitude in range 0..1000 for tokens
    private double minSim = MIN_SIM;
    private double minSentSim = MIN_SENT_SIM;
    private int minSent = MIN_SENT;
    private int minTokens = MIN_TOKENS;

    private int minWeight = (int)(MIN_SIM * 100);
    private int weightRange = 100 - minWeight;
    private int numWeightLevels = 4;

    private Boolean configured;

    public SmithWatermanGotoh()
    {
        configured = false;
    }

    public static final double ALPHA1 = 1.0 / 1.0; // 1.0
    public static final double BETA1 = 1.0 / 3.0; // 3.0;
    public static final double REWARD1 = 2.5; // 2.0;
    public static final double PENALTY1 = 1.0 / 3.0; // 3.0;
    public static final double ALPHA2 = 1.0 / 1.0; // 1.0
    public static final double BETA2 = 1.0 / 3.0; // 3.0;
    public static final double REWARD2 = 2.5; // 2.0;
    public static final double PENALTY2 = 1.0 / 3.0; // 3.0;
    public static final double MIN_SIM = CloneCollector.MIN_SIM;
    public static final double MIN_SENT_SIM = CloneCollector.MIN_SENT_SIM;
    public static final int MIN_SENT = CloneCollector.MIN_SENT;
    public static final int MIN_TOKENS = CloneCollector.MIN_TOKENS;

    public void init()
    {
        this.init(ALPHA1, BETA1, REWARD1, PENALTY1, ALPHA2, BETA2, REWARD2, PENALTY2);
    }

    public void init(double alpha1, double beta1, double reward1, double penalty1,
                     double alpha2, double beta2, double reward2, double penalty2)
    {
        this.alpha1 = (int)(alpha1 * 1000.0);
        this.beta1 = (int)(beta1 * 1000.0);
        this.reward1 = (int)(reward1 * 1000.0);
        this.penalty1 = (int)(penalty1 * 1000.0);
        this.alpha2 = (int)(alpha2 * 1000.0);
        this.beta2 = (int)(beta2 * 1000.0);
        this.reward2 = (int)(reward2 * 1000.0);
        this.penalty2 = (int)(penalty2 * 1000.0);

        this.configured = true;
    }

    public boolean config(CMethod a, CMethod b) {
        return config(a, b, this.minSim, this.minSentSim, this.minTokens, this.minSent, this.numWeightLevels);
    }

    public boolean config(CMethod a, CMethod b, double minSim, double minSentSim, int minTokens, int minSent,
                          int numWeightLevels) {
        this.methodA = a;
        this.methodB = b;
        this.minSim = minSim;
        this.minWeight = (int)(minSim * 100);
        this.weightRange = 100 - this.minWeight;
        this.numWeightLevels = numWeightLevels;
        this.minSentSim = minSentSim;
        this.minTokens = minTokens;
        this.minSent = minSent;
        this.a = a.getCStatements();
        this.b = b.getCStatements();
        this.m = this.a.size() + 1;
        this.n = this.b.size() + 1;

        if (minTokens > a.getNumTokens() || minTokens > b.getNumTokens()) return false;

        this.h  = new int[m][n];
        this.p  = new int[m][n];
        this.q  = new int[m][n];
        this.a0 = new int[m][n];
        this.b0 = new int[m][n];
        this.g  = new boolean[m][n];
        this.at = new int[m][n];
        this.bt = new int[m][n];
        this.s  = new double[m][n];

        for (int i = 0; i < this.m; i++) { this.h[i][0] = this.p[i][0] = this.q[i][0] = this.a0[i][0] = 0; this.g[i][0] = true; }
        for (int j = 0; j < this.n; j++) { this.h[0][j] = this.p[0][j] = this.q[0][j] = this.b0[0][j] = 0; this.g[0][j] = true; }
        this.g[0][0] = false;

        this.maxt = 0;
        for (int x = 0; x < this.a.size(); x++) this.maxt = Math.max(this.maxt,this.a.get(x).getTokens().size());
        for (int x = 0; x < this.b.size(); x++) this.maxt = Math.max(this.maxt, this.b.get(x).getTokens().size());
        this.maxt++;
        this.h2 = new int[this.maxt][this.maxt];
        this.p2 = new int[this.maxt][this.maxt];
        this.q2 = new int[this.maxt][this.maxt];
        this.s2 = new double[this.maxt][this.maxt];
        this.h2[0][0] = this.p2[0][0] = this.q2[0][0] = 0;
        this.h2[1][0] = this.h2[0][1] = -this.alpha2 - this.beta2;
        this.p2[1][0] = this.p2[0][1] = this.q2[1][0] = this.q2[0][1] = -this.alpha2 - this.beta2;
        for (int v = 2; v < this.maxt; v++)
        {
            this.h2[v][0] = this.p2[v][0] = this.q2[v][0] = this.h2[v - 1][0] - this.beta2;
            this.h2[0][v] = this.p2[0][v] = this.q2[0][v] = this.h2[0][v - 1] - this.beta2;
        }
        for (int r = 0; r < this.maxt; r++)
            for (int c = 0; c < this.maxt; c++)
            {
                this.s2[r][c] = 0;
            }

        for (int r = 0; r < this.m; r++)
            for (int c = 0; c < this.n; c++)
            {
                this.s[r][c] = 0;
                this.at[r][c] = 0;
                this.bt[r][c] = 0;
            }
        return true;
    }

    public void release()
    {
        if (configured)
        {
            this.a  = null;
            this.b  = null;
            this.h  = null;
            this.p  = null;
            this.q  = null;
            this.s  = null;
            this.a0 = null;
            this.b0 = null;
            this.g  = null;
            this.at = null;
            this.bt = null;
            this.h2 = null;
            this.p2 = null;
            this.q2 = null;
            this.s2 = null;

            this.configured = false;
        }
    }

    public Clone verifyAndGroupClone(Clone clone) {
        return verifyAndGroupClone(clone, this.minSim, this.minSentSim, this.minTokens, this.minSent, this.numWeightLevels, 50);
    }

    public Clone verifyAndGroupClone(Clone clone, double minSim, double minSentSim, int minTokens, int minSent,
                                           int numWeightLevels, int overlapPercentage)
    {
        int minsim = (int)(minSim * 1000);

        // verify clone pair
        this.init();
        ClonePair sourceClonePair = clone.getClonePairs().get(0);
        if (this.config(sourceClonePair.getFragments().get(0).getCMethod(), sourceClonePair.getFragments().get(1).getCMethod(),
            minSim, minSentSim, minTokens, minSent, numWeightLevels)) {
            CloneResult result = this.isClone();
            if (result.maxintsimvalue() >= minsim)
            {
                Fragment fragmentA = sourceClonePair.getFragments().get(0);
                Fragment fragmentB = sourceClonePair.getFragments().get(1);
                fragmentA.initFragment(this.methodA, this.a0[result.maxi()][result.maxj()], result.maxi() - 1);
                fragmentB.initFragment(this.methodB, this.b0[result.maxi()][result.maxj()], result.maxj() - 1);
                sourceClonePair.setSim(result.maxintsimvalue() / 10);
                sourceClonePair.setLevel(Math.min(this.numWeightLevels - 1,
                    ((sourceClonePair.getSim() - this.minWeight) * this.numWeightLevels / this.weightRange)));
                sourceClonePair.setWeight((sourceClonePair.getSim() - this.minWeight) * 100 / this.weightRange);
                sourceClonePair.setMaxNumberOfStatements(Math.max(fragmentA.getNumberOfStatements(), fragmentB.getNumberOfStatements()));
                sourceClonePair.setMaxCognitiveComplexity(Math.max(fragmentA.getCognitiveComplexity(), fragmentB.getCognitiveComplexity()));
                sourceClonePair.setCloneType(result.cloneType);
                this.release();
                return clone;
            }
        }
        this.release();
        return null;
    }

    /*
    public List<Clone> verifyAndGroupClone(Clone clone, double minSim, double minSentSim, int minTokens, int minSent,
                                           int numWeightLevels, int overlapPercentage)
    {
        List<Clone> targetClones = new ArrayList<>();
        List<ClonePair> sourceClonePairs = clone.getClonePairs();
        int minsim = (int)(minSim * 1000);

        // verify clone pairs
        int cp = 0;
        while (cp < sourceClonePairs.size()) {
            this.init();
            ClonePair sourceClonePair = sourceClonePairs.get(cp);
            if (this.config(sourceClonePair.getFragments().get(0).getCMethod(), sourceClonePair.getFragments().get(1).getCMethod(),
                            minSim, minSentSim, minTokens, minSent, numWeightLevels)) {
                CloneResult result = this.isClone();
                if (result.maxintsimvalue() >= minsim)
                {
                    Fragment fragmentA = sourceClonePair.getFragments().get(0);
                    Fragment fragmentB = sourceClonePair.getFragments().get(1);
                    fragmentA.initFragment(this.methodA, this.a0[result.maxi()][result.maxj()], result.maxi() - 1);
                    fragmentB.initFragment(this.methodB, this.b0[result.maxi()][result.maxj()], result.maxj() - 1);
                    sourceClonePair.setSim(result.maxintsimvalue() / 10);
                    sourceClonePair.setLevel(Math.min(this.numWeightLevels - 1,
                        ((sourceClonePair.getSim() - this.minWeight) * this.numWeightLevels / this.weightRange)));
                    sourceClonePair.setWeight((sourceClonePair.getSim() - this.minWeight) * 100 / this.weightRange);
                    sourceClonePair.setMaxNumberOfStatements(Math.max(fragmentA.getNumberOfStatements(), fragmentB.getNumberOfStatements()));
                    sourceClonePair.setMaxCognitiveComplexity(Math.max(fragmentA.getCognitiveComplexity(), fragmentB.getCognitiveComplexity()));
                    sourceClonePair.setCloneType(result.cloneType);
                    cp++;
                    this.release();
                    continue;
                }
            }
            this.release();
            sourceClonePairs.remove(cp);
        }

        // split non-overlapping clone pairs
        if (sourceClonePairs.size() > 0) {
            Clone nextClone = new Clone();
            nextClone.getClonePairs().add(sourceClonePairs.get(0));
            targetClones.add(nextClone);
            boolean found = false;
            for (cp = 1; cp < sourceClonePairs.size(); cp++) {
                ClonePair sourceClonePair = sourceClonePairs.get(cp);
                Fragment sf1a = sourceClonePair.getFragments().get(0);
                Fragment sf1b = sourceClonePair.getFragments().get(1);
                for (int c = 0; c < targetClones.size(); c++) {
                    Clone targetClone = targetClones.get(c);
                    List<ClonePair> targetClonePairs = targetClone.getClonePairs();
                    for (int p = 0; p < targetClonePairs.size(); p++) {
                        Fragment tf2a = targetClonePairs.get(p).getFragments().get(0);
                        Fragment tf2b = targetClonePairs.get(p).getFragments().get(1);
                        if (sf1a.overlaps(tf2a, overlapPercentage) || sf1a.overlaps(tf2b, overlapPercentage) ||
                            sf1b.overlaps(tf2a, overlapPercentage) || sf1b.overlaps(tf2b, overlapPercentage)) {
                            c = targetClones.size();
                            targetClonePairs.add(sourceClonePair);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    nextClone = new Clone();
                    nextClone.getClonePairs().add(sourceClonePair);
                    targetClones.add(nextClone);
                }
            }
        }

        return targetClones;
    }
    */

    private record CloneResult(int maxi, int maxj, int maxintsimvalue, int cloneType) {}

    private CloneResult isClone() {
        int maxi = 0;
        int maxj = 0;
        int minsim = (int)(minSim * 1000);
        int maxsim = 0;
        int minsentsim = (int)(minSentSim * 1000);
        int maxintsimvalue = 0;

        for (int i = 1; i < this.m; i++)
        {
            for (int j = 1; j < this.n; j++)
            {
                int maximum = 0;

                this.a0[i][j] = i - 1;
                this.b0[i][j] = j - 1;
                this.g[i][j] = false; // assume there is no gap in match

                // Vertical gap/deletion
                int p1 = this.h[i - 1][j] - this.alpha1 - this.beta1;
                int p2 = this.p[i - 1][j] - this.beta1;
                this.p[i][j] = Math.max(p1, p2);
                if (this.p[i][j] > maximum)
                {
                    if (this.h[i - 1][j] > 0) {
                        this.a0[i][j] = this.a0[i - 1][j];
                        this.b0[i][j] = this.b0[i - 1][j];
                        this.g[i][j] = true;
                    }
                    maximum = this.p[i][j];
                }

                // Horizontal gap/deletion
                int q1 = this.h[i][j - 1] - this.alpha1 - this.beta1;
                int q2 = this.q[i][j - 1] - this.beta1;
                this.q[i][j] = Math.max(q1, q2);
                if (this.q[i][j] > maximum)
                {
                    if (this.h[i][j-1] > 0) {
                        this.a0[i][j] = this.a0[i][j - 1];
                        this.b0[i][j] = this.b0[i][j - 1];
                        this.g[i][j] = true;
                    }
                    maximum = this.q[i][j];
                }

                // Match/Substitution value
                int comp_result = this.h[i - 1][j - 1];
                if (this.a.get(i - 1).getStatementId() == this.b.get(j - 1).getStatementId())
                {
                    int sent_sim = getSentencesSimilitude(i - 1, j - 1); // returns [0..1000] // * cam_sc_val / 100;

                    if (sent_sim >= minsentsim)
                        comp_result += sent_sim * this.reward1 / 1000;
                    else
                        comp_result -= this.penalty1;
                }
                else
                {
                    comp_result -= this.penalty1;
                }
                if (comp_result > maximum)
                {
                    if (this.h[i - 1][j - 1] > 0) {
                        this.a0[i][j] = this.a0[i - 1][j - 1];
                        this.b0[i][j] = this.b0[i - 1][j - 1];
                        this.g[i][j] = this.g[i-1][j-1];
                    }
                    maximum = comp_result;
                }

                this.h[i][j] = maximum;
                this.s[i][j] = simValue(maximum, i, j);

                this.at[i][j] = this.a.get(i - 1).getTokens().size() + (this.a0[i][j] < i - 1 ? this.at[i - 1][j] : 0);
                this.bt[i][j] = this.b.get(j - 1).getTokens().size() + (this.b0[i][j] < j - 1 ? this.bt[i][j - 1] : 0);

                // Check minimum sentences and similitude parameters
                int simnew = intSimValue(maximum, i, j);
                if (hasMinSentences(minSent, i, j) && hasMinTokens(minTokens, i, j) && (simnew >= minsim))
                {
                    int minsx = minSentences(maxi, maxj);
                    int mins = minSentences(i, j);
                    int mintx = minTokens(maxi, maxj);
                    int mint = minTokens(i, j);

                    if ((minsx <= mins) && (mintx <= mint) && (simnew >= maxintsimvalue))
                    {
                        maxsim = maximum;
                        maxi = i;
                        maxj = j;
                        maxintsimvalue = simnew;
                    }
                }
            }
        }
        int cloneType = this.g[maxi][maxj] ? 2 : isType1(maxi, maxj) ? 0 : 1;
        return new CloneResult(maxi, maxj, maxintsimvalue, cloneType);
    }

    private boolean isType1(int maxi, int maxj) {
        // if has different number of sentences
        if (maxi - this.a0[maxi][maxj] != maxj - this.b0[maxi][maxj])
            // clone is not type 1
            return false;
        // check if all sentences are equal
        for (int i = this.a0[maxi][maxj], j = this.b0[maxi][maxj]; i < maxi; i++, j++) {
            if (!this.a.get(i).getText().equals(this.b.get(j).getText()))
                // clone is not type 1
                return false;
        }
        // clone is type 1
        return true;
    }

    private int minSentences(int i, int j)
    {
        return Math.min(i - this.a0[i][j], j - this.b0[i][j]);
    }

    private int minTokens(int i, int j)
    {
        return Math.min(this.at[i][j], this.bt[i][j]);
    }

    private int gapSentences(int i, int j)
    {
        return Math.abs(i - this.a0[i][j] - j + this.b0[i][j]);
    }

    private Boolean hasMinSentences(int min, int i, int j)
    {
        return (Math.min(i - this.a0[i][j], j - this.b0[i][j]) >= min);
    }

    private Boolean hasMinTokens(int min, int i, int j)
    {
        return (Math.min(this.at[i][j], this.bt[i][j]) >= min);
    }

    private Double simValue(int sim, int i, int j)
    {
        int n = minSentences(i, j);
        int g = gapSentences(i, j);
        int wg = (g == 0 ? 0 : this.alpha1 + g * this.beta1 < 0 ? -this.alpha1 - g * this.beta1 : 0);
        int w = this.reward1 * n + wg;
        return (w == 0 ? 0.0 : sim * 1000 / (double)w); // returns [0..1000]
    }

    private int intSimValue(int sim, int i, int j)
    {
        int n = minSentences(i, j);
        int g = gapSentences(i, j);
        int wg = (g == 0 ? 0 : this.alpha1 + g * this.beta1 < 0 ? -this.alpha1 - g * this.beta1 : 0);
        int w = n * this.reward1 + wg;

        return (w == 0 ? 0 : sim * 1000 / w); // returns [0..1000]
    }

    // Needleman-Wunsch - Alineamiento Global de Secuencias
    private int getSentencesSimilitude(int v, int w)
    {
        int sim = 0;

        if (this.configured)
        {
            this.m2 = this.a.get(v).getTokens().size() + 1;
            this.n2 = this.b.get(w).getTokens().size() + 1;
            for (int i = 1; i < this.m2; i++)
            {
                for (int j = 1; j < this.n2; j++)
                {
                    // Vertical gap/deletion
                    int maxpq = this.p2[i][j] = Math.max(this.h2[i - 1][j] - this.alpha2 - this.beta2,
                            this.p2[i - 1][j] - this.beta2);

                    // Horizontal gap/deletion
                    this.q2[i][j] = Math.max(this.h2[i][j - 1] - this.alpha2 - this.beta2,
                            this.q2[i][j - 1] - this.beta2);

                    // Maximum between vertical and horizontal gaps
                    if (this.q2[i][j] > this.p2[i][j])
                    {
                        maxpq = this.q2[i][j];
                    }

                    // Match/mismatch
                    this.h2[i][j] = this.h2[i - 1][j - 1] + (this.a.get(v).getTokens().get(i - 1).equals(this.b.get(w).getTokens().get(j - 1)) ? this.reward2 : -this.penalty2);

                    // Maximum between gaps and match/mismatch
                    if (this.h2[i][j] <= maxpq)
                        this.h2[i][j] = maxpq;

                    int wi = this.alpha2 + Math.abs(i - j) * this.beta2;
                    double gk = ((double)this.reward2 * Math.min(i, j) + (i == j ? 0.0 : wi < 0 ? (double)-wi : 0.0));
                    this.s2[i][j] = ((double)this.h2[i][j]) / gk;
                }
            }

            int wk = this.alpha2 + Math.abs(this.m2 - this.n2) * this.beta2;
            int gapv = this.reward2 * Math.min(this.m2 - 1, this.n2 - 1) + ((this.m2 == this.n2) ? 0 : (wk < 0 ? -wk : 0));
            sim = (Math.max(0, this.h2[this.m2 - 1][this.n2 - 1])) * 1000 / gapv; // returns [0..1000]
        }

        return sim;
    }
}

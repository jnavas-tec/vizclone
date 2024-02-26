package cr.ac.tec.vizClone;

import cr.ac.tec.vizClone.model.*;
import cr.ac.tec.vizClone.utils.FragmentDict;

import java.util.List;

public class SmithWatermanGotoh {
    private int m;                // number of rows
    private int n;                // number of columns
    private CMethod methodA;      // left side method
    private CMethod methodB;      // upper side method
    private List<CStatement> a;   // rows (left side) statements
    private List<CStatement> b;   // columns (upper side) statements
    private int alpha1;           // inter sentences gap opening penalty (may be negative: reward)
    private int beta1;            // inter sentences gap penalty (may be negative: reward)
    private int reward1;          // inter sentences match reward (must be positive)
    private int penalty1;         // inter sentences mismatch penalty (must be positive)
    private int[][] h;            // score matrix
    private int[][] p;            // vertical gap score matrix
    private int[][] q;            // horizontal gap score matrix
    private int[][] a0;           // vertical start of clone
    private int[][] b0;           // horizontal start of clone
    //private char[][] d;
    private double[][] s;
    private int maxt;
    private int m2;
    private int n2;
    private int alpha2;
    private int beta2;
    private int reward2;
    private int penalty2;
    private int[][] h2;
    private int[][] p2;
    private int[][] q2;
    //private char[][] d2;
    private double[][] s2;
    //private int[] cam_sc_index;
    //private int[][] cam_sc_matrix;
    private double min_sim = 0.8;
    private double min_sent_sim = 0.8;
    private int min_sent = 10;

    private Boolean configured;

    public SmithWatermanGotoh()
    {
        configured = false;
    }

    public void init()
    {
        this.init(1.0, 1.0/3.0, 2.0, 1.0/3.0,
                1.0, 1.0/3.0, 2.0, 1.0/3.0);
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

        //initializeCAMSCIndexAndMatrix();

        this.configured = true;
    }

    public boolean config(CMethod a, CMethod b) {
        return config(a, b, this.min_sim, this.min_sent_sim, this.min_sent);
    }

    public boolean config(CMethod a, CMethod b, double minSim, double minSentSim, int minSent) {
        this.methodA = a;
        this.methodB = b;
        this.min_sim = minSim;
        this.min_sent_sim = minSentSim;
        this.min_sent = minSent;
        this.a = a.getCStatements();
        this.b = b.getCStatements();
        this.m = this.a.size() + 1;
        this.n = this.b.size() + 1;

        if (minSent > this.a.size() || minSent > this.b.size()) return false;

        this.h  = new int[m][n];
        this.p  = new int[m][n];
        this.q  = new int[m][n];
        this.a0 = new int[m][n];
        this.b0 = new int[m][n];
        //this.d  = new char[m][n];
        this.s  = new double[m][n];

        for (int i = 0; i < this.m; i++) { this.h[i][0] = this.p[i][0] = this.q[i][0] = this.a0[i][0] = 0; }
        for (int j = 0; j < this.n; j++) { this.h[0][j] = this.p[0][j] = this.q[0][j] = this.b0[0][j] = 0; }

        this.maxt = 0;
        for (int x = 0; x < this.a.size(); x++) this.maxt = Math.max(this.maxt,this.a.get(x).getTokens().size());
        for (int x = 0; x < this.b.size(); x++) this.maxt = Math.max(this.maxt, this.b.get(x).getTokens().size());
        this.maxt++;
        this.h2 = new int[this.maxt][this.maxt];
        this.p2 = new int[this.maxt][this.maxt];
        this.q2 = new int[this.maxt][this.maxt];
        //this.d2 = new char[this.maxt][this.maxt];
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
                //this.d2[r][c] = ' ';
                this.s2[r][c] = 0;
            }

        for (int r = 0; r < this.m; r++)
            for (int c = 0; c < this.n; c++)
            {
                //this.d[r][c] = ' ';
                this.s[r][c] = 0;
            }
        return true;
    }

    public void release()
    {
        if (configured)
        {
            this.a = null;
            this.b = null;
            this.h = null;
            this.p = null;
            this.q = null;
            //this.d = null;
            this.s = null;
            this.a0 = null;
            this.b0 = null;
            this.h2 = null;
            this.p2 = null;
            this.q2 = null;
            //this.d2 = null;
            this.s2 = null;

            //cam_sc_index = null;
           // cam_sc_matrix = null;

            this.configured = false;
        }
    }

    public Clone getClone() {
        return getClone(this.min_sim, this.min_sent_sim, this.min_sent);
    }

    public Clone getClone(double MinSim, double MinSentSim, int MinSent)
    {
        Clone clone = null;

        int maxsim = 0;
        int maxi = 0;
        int maxj = 0;
        int minsim = (int)(MinSim * 1000);
        int minsentsim = (int)(MinSentSim * 1000);
        int maxintsimvalue = 0;

        if (this.configured)
        {
            for (int i = 1; i < this.m; i++)
            {
                for (int j = 1; j < this.n; j++)
                {
                    int maximum = 0;

                    this.a0[i][j] = i - 1;
                    this.b0[i][j] = j - 1;

                    // Vertical gap/deletion
                    int p1 = this.h[i - 1][j] - this.alpha1 - this.beta1;
                    int p2 = this.p[i - 1][j] - this.beta1;
                    this.p[i][j] = Math.max(p1, p2);
                    if (this.p[i][j] > maximum)
                    {
                        if (this.h[i - 1][j] > 0) {
                            this.a0[i][j] = this.a0[i - 1][j];
                            this.b0[i][j] = this.b0[i - 1][j];
                        }

                        maximum = this.p[i][j];

                        //this.d[i][j] = '\u2191';
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
                        }

                        maximum = this.q[i][j];

                        //this.d[i][j] = '\u2190';
                    }

                    // Match/Substitution value
                    int comp_result = this.h[i - 1][j - 1];
                    //int cam_sc_val = cam_sc_matrix[cam_sc_index[(int)a.get(i - 1).getKind()]][cam_sc_index[(int)b.get(j - 1).getKind()]];
                    //if (cam_sc_val > 0)
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
                        }
                        maximum = comp_result;

                        //this.d[i][j] = '\u2196';
                    }

                    this.h[i][j] = maximum;
                    this.s[i][j] = simValue(maximum, i, j);

                    // Check minimum sentences and similitude parameters
                    int simnew = intSimValue(maximum, i, j);
                    if (hasMinSentences(MinSent, i, j) && (simnew >= minsim))
                    {
                        int minsx = minSentences(maxi, maxj);
                        int mins = minSentences(i, j);

                        if ((minsx <= mins) && (simnew >= maxintsimvalue))
                        {
                            maxsim = maximum;
                            maxi = i;
                            maxj = j;
                            maxintsimvalue = simnew;
                        }
                    }
                }
            }

            if (maxintsimvalue >= minsim) // (simValue(maxsim, maxi, maxj) >= MinSim)
            {
                clone = new Clone();
                ClonePair clonePair = new ClonePair();
                //clone.setWeight(simValue(maxsim, maxi, maxj));

                Fragment fragmentA = FragmentDict.getFragment(this.methodA, this.a0[maxi][maxj], maxi - 1);
                fragmentA.setNumberOfClones(1);

                Fragment fragmentB = FragmentDict.getFragment(this.methodB, this.b0[maxi][maxj], maxj - 1);
                fragmentB.setNumberOfClones(1);

                clonePair.getFragments().add(fragmentA);
                clonePair.getFragments().add(fragmentB);
                clonePair.setWeight(maxintsimvalue / 10);
                clonePair.setMaxNumberOfStatements(Math.max(fragmentA.getNumberOfStatements(), fragmentB.getNumberOfStatements()));
                clone.getClonePairs().add(clonePair);
                clone.setNumberOfClonePairs(1);
                clone.setMaxNumberOfStatements(clonePair.getMaxNumberOfStatements());
                clone.setMaxWeight(clonePair.getWeight());

                // weight: maxintsimvalue / 10

                // how to create the clones?
                // how to merge fragment pairs into one clone?
                // how to sort fragments and clones?
            }
        }

        return clone;
    }

    private int minSentences(int i, int j)
    {
        return Math.min(i - this.a0[i][j], j - this.b0[i][j]);
    }

    private int gapSentences(int i, int j)
    {
        return Math.abs(i - this.a0[i][j] - j + this.b0[i][j]);
    }

    private Boolean hasMinSentences(int min, int i, int j)
    {
        return (Math.min(i - this.a0[i][j], j - this.b0[i][j]) >= min);
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
                    //this.d2[i][j] = '\u2191';

                    // Horizontal gap/deletion
                    this.q2[i][j] = Math.max(this.h2[i][j - 1] - this.alpha2 - this.beta2,
                            this.q2[i][j - 1] - this.beta2);

                    // Maximum between vertical and horizontal gaps
                    if (this.q2[i][j] > this.p2[i][j])
                    {
                        maxpq = this.q2[i][j];
                        //this.d2[i][j] = '\u2190';
                    }

                    // Match/mismatch
                    this.h2[i][j] = this.h2[i - 1][j - 1] + (this.a.get(v).getTokens().get(i - 1).equals(this.b.get(w).getTokens().get(j - 1)) ? this.reward2 : -this.penalty2);

                    // Maximum between gaps and match/mismatch
                    if (this.h2[i][j] <= maxpq)
                        this.h2[i][j] = maxpq;
                    /*
                    if (this.h2[i][j] > maxpq)
                        this.d2[i][j] = '\u2196';
                    else
                        this.h2[i][j] = maxpq;
                     */

                    int wi = this.alpha2 + Math.abs(i - j) * this.beta2;
                    double gk = ((double)this.reward2 * Math.min(i, j) + (i == j ? 0.0 : wi < 0 ? (double)-wi : 0.0));
                    this.s2[i][j] = ((double)this.h2[i][j]) / gk;
                }
            }

            int wk = this.alpha2 + Math.abs(this.m2 - this.n2) * this.beta2;
            int gapv = this.reward2 * Math.min(this.m2 - 1, this.n2 - 1) + ((this.m2 == this.n2) ? 0 : (wk < 0 ? -wk : 0));
            sim = (Math.max(0, this.h2[this.m2 - 1][this.n2 - 1])) * 1000 / gapv; // returns [0..1000]

                /*
                Console.OutputEncoding = System.Text.Encoding.UTF8;
                Console.WriteLine("h2");
                Console.WriteLine(ToTokensStringD(h2, d2, m2, n2));
                Console.WriteLine("d2");
                Console.WriteLine(ToTokensStringC(d2, m2, n2));
                Console.WriteLine("h2");
                Console.WriteLine(ToTokensString(h2, d2, m2, n2));
                Console.WriteLine("s2");
                Console.WriteLine(ToTokensStringF(s2, d2, m2, n2));
                Console.WriteLine("Similitude = {0}, From (i, j) = ({1}, {2}), To (i, j) = ({3}, {4}), maxsim = {5}",
                    sim, 0, 0, m2 - 1, n2 - 1, h2[m2 - 1, n2 - 1]);
                Console.WriteLine("\n=========================================\n");
                Console.ReadKey();
                */
        }

        return sim;
    }
}

package eu.amadeos.fractals.domain;

/**
 * The iteration formulas. Each escape-time formula has a Mandelbrot form (z starts at 0, c is the point) and
 * a Julia form (z starts at the point, c is a fixed seed); Newton finds the roots of z³ − 1 and has only the
 * one form.
 */
public enum Formula {
    /** z² + c */
    MANDELBROT(Kind.ESCAPE, -0.75, 0, 2.6, -0.8, 0.156, false),
    /** zⁿ + c with an integer power n from 3 to 12. */
    MULTIBROT(Kind.ESCAPE, 0, 0, 3.0, 0.4, 0.6, false),
    /** (|Re z| + i |Im z|)² + c, mirrored so that the ship stands upright */
    BURNING_SHIP(Kind.ESCAPE, -0.4, 0.7, 2.8, -1.25, 0.05, false),
    /** conj(z)² + c, also called Mandelbar */
    TRICORN(Kind.ESCAPE, -0.3, 0, 3.2, -0.9, 0.1, false),
    /** |Re(z²)| + i Im(z²) + c */
    CELTIC(Kind.ESCAPE, -0.4, 0, 3.0, -1.1, 0.1, false),
    /** |Re(z²)| − i |Im(z²)| + c */
    BUFFALO(Kind.ESCAPE, -0.8, 0.6, 3.2, -1.2, 0.0, false),
    /** z² + Re c + Im c · z₋₁: the previous value feeds back. Shown as Julia set by default. */
    PHOENIX(Kind.ESCAPE, 0, 0, 3.0, 0.56667, -0.5, true),
    /** ((z² + c − 1) / (2z + c − 2))², from the physics of magnetic renormalisation; also converges to 1. */
    MAGNET(Kind.ESCAPE, 1.4, 0, 4.6, 1.8, 1.0, false),
    /** Newton's method for z³ − 1: the colour tells which of the three roots a point runs to. */
    NEWTON(Kind.CONVERGE, 0, 0, 3.0, 0, 0, false);

    public enum Kind {ESCAPE, CONVERGE}

    /** Lowest and highest power of {@link #MULTIBROT}. */
    public static final int MIN_POWER = 3;
    public static final int MAX_POWER = 12;

    private final Kind kind;
    private final double centerRe;
    private final double centerIm;
    private final double size;
    private final double seedRe;
    private final double seedIm;
    private final boolean juliaByDefault;

    Formula(Kind kind, double centerRe, double centerIm, double size, double seedRe, double seedIm,
            boolean juliaByDefault) {
        this.kind = kind;
        this.centerRe = centerRe;
        this.centerIm = centerIm;
        this.size = size;
        this.seedRe = seedRe;
        this.seedIm = seedIm;
        this.juliaByDefault = juliaByDefault;
    }

    public Kind kind() {
        return kind;
    }

    /** True if a Julia form exists. */
    public boolean hasJulia() {
        return kind == Kind.ESCAPE;
    }

    public boolean usesPower() {
        return this == MULTIBROT;
    }

    public boolean juliaByDefault() {
        return juliaByDefault;
    }

    /** The whole fractal in view. */
    public Region home(boolean julia) {
        if (julia) return Region.of(0, 0, this == MAGNET ? 6.0 : 3.2);
        return Region.of(centerRe, centerIm, size);
    }

    public double defaultSeedRe() {
        return seedRe;
    }

    public double defaultSeedIm() {
        return seedIm;
    }

    /** Growth of |z| per step far outside, for smooth colouring. */
    public double degree(int power) {
        return usesPower() ? power : 2;
    }

    /**
     * True if this form can be rendered with perturbation, which allows zooms far below the precision of
     * {@code double}.
     */
    public boolean supportsDeepZoom(boolean julia) {
        return this == MANDELBROT && !julia;
    }
}

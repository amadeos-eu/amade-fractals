package eu.amadeos.apps.fractals.domain;

import java.util.Objects;

/**
 * Everything that decides which points belong to the fractal: formula, Julia seed, iteration limit, bailout
 * and the region in view. Colours are {@link ColorSettings}; changing them needs no new calculation.
 *
 * @param formula       the iteration
 * @param power         the power n of {@link Formula#MULTIBROT}, ignored otherwise
 * @param julia         Julia form: z starts at the point and c is the seed
 * @param seedRe        real part of the Julia seed
 * @param seedIm        imaginary part of the Julia seed
 * @param maxIterations points still bounded after this many steps count as inside
 * @param bailout       |z| beyond which a point has escaped
 * @param region        the part of the plane in view
 */
public record FractalSettings(Formula formula, int power, boolean julia, double seedRe, double seedIm,
                              int maxIterations, double bailout, Region region) {
    public static final int MIN_ITERATIONS = 16;
    public static final int MAX_ITERATIONS = 10_000_000;
    public static final int DEFAULT_ITERATIONS = 500;
    /** Large enough for smooth colouring without bands. */
    public static final double DEFAULT_BAILOUT = 64;
    public static final double MIN_BAILOUT = 2;
    public static final double MAX_BAILOUT = 1e6;

    public FractalSettings {
        Objects.requireNonNull(formula, "formula");
        Objects.requireNonNull(region, "region");
        power = Math.max(Formula.MIN_POWER, Math.min(Formula.MAX_POWER, power));
        julia = julia && formula.hasJulia();
        maxIterations = Math.max(MIN_ITERATIONS, Math.min(MAX_ITERATIONS, maxIterations));
        if (!Double.isFinite(seedRe) || !Double.isFinite(seedIm)) throw new IllegalArgumentException("Invalid seed");
        bailout = Double.isFinite(bailout) ? Math.max(MIN_BAILOUT, Math.min(MAX_BAILOUT, bailout)) : DEFAULT_BAILOUT;
    }

    /** The formula as it opens: its usual form, seed and region. */
    public static FractalSettings of(Formula formula) {
        boolean julia = formula.juliaByDefault();
        return new FractalSettings(formula, Formula.MIN_POWER, julia, formula.defaultSeedRe(), formula.defaultSeedIm(),
                DEFAULT_ITERATIONS, DEFAULT_BAILOUT, formula.home(julia));
    }

    public static final FractalSettings DEFAULT = of(Formula.MANDELBROT);

    /** The whole fractal of this form in view. */
    public Region home() {
        return formula.home(julia);
    }

    public double degree() {
        return formula.degree(power);
    }

    /** True if a window this many pixels high needs perturbation to show this region. */
    public boolean needsPerturbation(int height) {
        return formula.supportsDeepZoom(julia) && region.pixelSize(height) < doubleLimit();
    }

    /**
     * The smallest region height this form can show in a window this many pixels high: limited by the
     * precision of {@code double}, unless perturbation is available.
     */
    public double minSize(int height) {
        return formula.supportsDeepZoom(julia) ? Region.MIN_SIZE : doubleLimit() * Math.max(1, height);
    }

    /** Pixel size below which neighbouring pixels of {@code double} coordinates are no longer distinct. */
    private double doubleLimit() {
        double magnitude = Math.max(1, Math.max(Math.abs(region.reDouble()), Math.abs(region.imDouble())));
        return 4e-15 * magnitude;
    }

    public FractalSettings withFormula(Formula newFormula) {
        return new FractalSettings(newFormula, power, julia && newFormula.hasJulia(), seedRe, seedIm, maxIterations,
                bailout, region);
    }

    public FractalSettings withPower(int newPower) {
        return new FractalSettings(formula, newPower, julia, seedRe, seedIm, maxIterations, bailout, region);
    }

    public FractalSettings withJulia(boolean newJulia, double newSeedRe, double newSeedIm) {
        return new FractalSettings(formula, power, newJulia, newSeedRe, newSeedIm, maxIterations, bailout, region);
    }

    public FractalSettings withMaxIterations(int newMax) {
        return new FractalSettings(formula, power, julia, seedRe, seedIm, newMax, bailout, region);
    }

    public FractalSettings withBailout(double newBailout) {
        return new FractalSettings(formula, power, julia, seedRe, seedIm, maxIterations, newBailout, region);
    }

    public FractalSettings withRegion(Region newRegion) {
        return new FractalSettings(formula, power, julia, seedRe, seedIm, maxIterations, bailout, newRegion);
    }
}

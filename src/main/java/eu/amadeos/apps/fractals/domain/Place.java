package eu.amadeos.apps.fractals.domain;

import java.util.Locale;

/** Well-known places worth a visit, for the Places menu. */
public enum Place {
    SEAHORSE_VALLEY(Formula.MANDELBROT, false, 0, 0, "-0.7453", "0.1127", 0.012, 800),
    ELEPHANT_VALLEY(Formula.MANDELBROT, false, 0, 0, "0.2925", "0.0149", 0.012, 800),
    TRIPLE_SPIRAL(Formula.MANDELBROT, false, 0, 0, "-0.0884", "0.6544", 0.012, 1000),
    MINI_MANDELBROT(Formula.MANDELBROT, false, 0, 0, "-1.75488", "0", 0.06, 600),
    /** A long way down: 10²⁰ times, rendered with perturbation. */
    DEEP_SEAHORSE(Formula.MANDELBROT, false, 0, 0,
            "-0.743643887037158704752191506114774", "0.131825904205311970493132056385139", 2e-20, 20000, 0.1),
    DENDRITE_JULIA(Formula.MANDELBROT, true, 0, 1, "0", "0", 3.2, 500),
    DOUADY_RABBIT(Formula.MANDELBROT, true, -0.123, 0.745, "0", "0", 3.2, 500),
    SAN_MARCO_DRAGON(Formula.MANDELBROT, true, -0.75, 0.11, "0", "0", 3.2, 800),
    BURNING_SHIP_ARMADA(Formula.BURNING_SHIP, false, 0, 0, "-1.762", "0.04", 0.1, 800),
    PHOENIX_JULIA(Formula.PHOENIX, true, 0.56667, -0.5, "0", "0", 3.0, 500);

    private final Formula formula;
    private final boolean julia;
    private final double seedRe;
    private final double seedIm;
    private final String re;
    private final String im;
    private final double size;
    private final int iterations;
    private final double density;

    Place(Formula formula, boolean julia, double seedRe, double seedIm, String re, String im, double size,
          int iterations) {
        this(formula, julia, seedRe, seedIm, re, im, size, iterations, 1);
    }

    Place(Formula formula, boolean julia, double seedRe, double seedIm, String re, String im, double size,
          int iterations, double density) {
        this.formula = formula;
        this.julia = julia;
        this.seedRe = seedRe;
        this.seedIm = seedIm;
        this.re = re;
        this.im = im;
        this.size = size;
        this.iterations = iterations;
        this.density = density;
    }

    /** Key of the name in the messages, e.g. {@code place.seahorse-valley}. */
    public String key() {
        return "place." + name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Colour density that suits the place: deep zooms have high iteration counts that change fast from pixel
     * to pixel, so the palette repeats more slowly there.
     */
    public double density() {
        return density;
    }

    public FractalSettings settings() {
        return new FractalSettings(formula, Formula.MIN_POWER, julia, seedRe, seedIm, iterations,
                FractalSettings.DEFAULT_BAILOUT, Region.parse(re, im, size));
    }
}

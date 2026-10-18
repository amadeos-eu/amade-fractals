package eu.amadeos.apps.fractals.service.render;

import eu.amadeos.apps.fractals.domain.Formula;
import eu.amadeos.apps.fractals.domain.FractalSettings;
import eu.amadeos.apps.fractals.domain.Place;
import eu.amadeos.apps.fractals.domain.Region;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Single points with known fates, and perturbation against arbitrary precision. */
class CalculationTest {
    private static FractalData point(FractalSettings settings, double re, double im) {
        FractalData data = new FractalData(settings, 1, 1);
        new PointCalculator(settings).compute(re, im, data, 0);
        return data;
    }

    private static FractalSettings settings(Formula formula, boolean julia, double seedRe, double seedIm) {
        return new FractalSettings(formula, 3, julia, seedRe, seedIm, 1000, 2, formula.home(julia));
    }

    @Test
    void mandelbrotPoints() {
        FractalSettings s = settings(Formula.MANDELBROT, false, 0, 0);
        assertEquals(FractalData.INSIDE, point(s, 0, 0).state(0));
        assertEquals(FractalData.INSIDE, point(s, -1, 0).state(0));
        // -2 stays at 2 for ever: on the boundary, still inside.
        assertEquals(FractalData.INSIDE, point(s, -2, 0).state(0));
        // 1: 0, 1, 2, 5: |5| > 2 after 3 steps.
        FractalData one = point(s, 1, 0);
        assertEquals(FractalData.ESCAPED, one.state(0));
        assertEquals(3, one.iterations(0));
        assertEquals(5, one.lastRe(0), 0);
        // Outside the shortcuts, the periodicity check ends a long orbit early with the same verdict.
        assertEquals(FractalData.INSIDE, point(s, -0.1, 0.8).state(0));
        assertEquals(FractalData.ESCAPED, point(s, 0.26, 0).state(0));
    }

    @Test
    void juliaPoints() {
        // c = 0: the unit disc.
        FractalSettings s = settings(Formula.MANDELBROT, true, 0, 0);
        assertEquals(FractalData.INSIDE, point(s, 0.5, 0.5).state(0));
        FractalData out = point(s, 1.5, 0);
        assertEquals(FractalData.ESCAPED, out.state(0));
        assertEquals(1, out.iterations(0));
    }

    @Test
    void otherEscapeFormulas() {
        for (Formula formula : Formula.values()) {
            if (formula.kind() != Formula.Kind.ESCAPE) continue;
            FractalSettings s = settings(formula, false, 0, 0);
            // The magnet's far points run to its fixed point 1 instead of escaping.
            byte expected = formula == Formula.MAGNET ? FractalData.CONVERGED : FractalData.ESCAPED;
            assertEquals(expected, point(s, 3, 3).state(0), formula + " far out");
        }
        FractalSettings cubic = settings(Formula.MULTIBROT, false, 0, 0);
        assertEquals(FractalData.INSIDE, point(cubic, 0, 0.3).state(0));
        // z³ + c with c = 1: 0, 1, 2, 9.
        assertEquals(3, point(cubic, 1, 0).iterations(0));
        FractalSettings ship = settings(Formula.BURNING_SHIP, false, 0, 0);
        assertEquals(FractalData.INSIDE, point(ship, -1, 0).state(0));
        FractalSettings tricorn = settings(Formula.TRICORN, false, 0, 0);
        assertEquals(FractalData.INSIDE, point(tricorn, 0, 0).state(0));
    }

    @Test
    void tricornIsMirrorSymmetric() {
        FractalSettings s = settings(Formula.TRICORN, false, 0, 0);
        for (double im = 0.05; im < 1; im += 0.1) {
            assertEquals(point(s, -0.3, im).iterations(0), point(s, -0.3, -im).iterations(0));
        }
    }

    @Test
    void magnetConvergesToOne() {
        FractalSettings s = new FractalSettings(Formula.MAGNET, 3, false, 0, 0, 500, 64, Formula.MAGNET.home(false));
        assertEquals(FractalData.CONVERGED, point(s, -1, 0).state(0));
        assertEquals(FractalData.INSIDE, point(s, 0, 0).state(0));
        // c = 2: the denominator 2z + c − 2 is 0 at the start.
        assertEquals(FractalData.ESCAPED, point(s, 2, 0).state(0));
    }

    @Test
    void newtonFindsTheRoots() {
        FractalSettings s = settings(Formula.NEWTON, false, 0, 0);
        assertEquals(FractalData.CONVERGED, point(s, 2, 0).state(0));
        assertEquals(FractalData.CONVERGED + 1, point(s, -1, 1).state(0));
        assertEquals(FractalData.CONVERGED + 2, point(s, -1, -1).state(0));
        assertEquals(FractalData.INSIDE, point(s, 0, 0).state(0));
    }

    @Test
    void perturbationAgreesWithArbitraryPrecision() {
        FractalSettings deep = Place.DEEP_SEAHORSE.settings().withMaxIterations(20_000).withBailout(2);
        int w = 40;
        int h = 30;
        Perturbation perturbation = Perturbation.reference(deep, h, () -> false);
        assertTrue(perturbation.referenceLength() > 1000);
        FractalData data = new FractalData(deep, w, h);
        Region r = deep.region();
        int[][] pixels = {{0, 0}, {39, 29}, {13, 21}};
        for (int[] p : pixels) {
            double dRe = r.offsetRe(p[0] + 0.5, w, h);
            double dIm = r.offsetIm(p[1] + 0.5, w, h);
            int index = p[1] * w + p[0];
            perturbation.compute(dRe, dIm, data, index);
            int truth = iterate(r.re().add(new BigDecimal(dRe)), r.im().add(new BigDecimal(dIm)), 20_000, 2);
            assertTrue(Math.abs(truth - data.iterations(index)) <= 1,
                    () -> "pixel " + p[0] + "," + p[1] + ": " + data.iterations(index) + " instead of " + truth);
        }
    }

    @Test
    void perturbationAgreesWithDoublesWhereBothWork() {
        FractalSettings s = FractalSettings.DEFAULT.withRegion(Region.of(-0.7453, 0.1127, 0.01)).withMaxIterations(800);
        int w = 32;
        int h = 24;
        FractalData direct = Renderer.direct().render(s, w, h, () -> false, (d, r) -> { });
        FractalData perturbed = new FractalData(s, w, h);
        Perturbation perturbation = Perturbation.reference(s, h, () -> false);
        int differ = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                perturbation.compute(s.region().offsetRe(x + 0.5, w, h), s.region().offsetIm(y + 0.5, w, h),
                        perturbed, y * w + x);
                if (Math.abs(perturbed.iterations(y * w + x) - direct.iterations(y * w + x)) > 1) differ++;
            }
        }
        assertTrue(differ <= w * h / 100, "differing pixels: " + differ);
    }

    /** The plain iteration in BigDecimal, the reference for perturbation. */
    private static int iterate(BigDecimal cr, BigDecimal ci, int max, double bailout) {
        MathContext mc = new MathContext(60);
        BigDecimal zr = BigDecimal.ZERO;
        BigDecimal zi = BigDecimal.ZERO;
        BigDecimal two = BigDecimal.valueOf(2);
        for (int n = 0; n < max; n++) {
            BigDecimal t = zr.multiply(zr, mc).subtract(zi.multiply(zi, mc), mc).add(cr, mc);
            zi = two.multiply(zr, mc).multiply(zi, mc).add(ci, mc);
            zr = t;
            if (zr.multiply(zr, mc).add(zi.multiply(zi, mc), mc).doubleValue() > bailout * bailout) return n + 1;
        }
        return max;
    }
}

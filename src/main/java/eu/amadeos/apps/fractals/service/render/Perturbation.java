package eu.amadeos.apps.fractals.service.render;

import eu.amadeos.apps.fractals.domain.FractalSettings;
import eu.amadeos.apps.fractals.domain.Region;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Deep zoom into the Mandelbrot set by perturbation: one reference orbit Z at the centre is calculated with
 * {@link BigDecimal} in as many digits as the zoom needs, every pixel then follows only its small distance
 * dz from that orbit, in {@code double}:
 *
 * <pre>
 * dz(n+1) = (2·Z(n) + dz(n))·dz(n) + dc
 * </pre>
 *
 * <p>Where the reference is no good for a pixel (|Z + dz| &lt; |dz|) or has ended, the pixel continues on the
 * reference from its start with dz = Z + dz ("rebasing", after Zhuoran). This replaces the glitch detection
 * and the second references of older methods and keeps the result exact without more than one reference.
 */
final class Perturbation {
    private final double[] refRe;
    private final double[] refIm;
    private final int length;
    private final int max;
    private final double bailout2;

    private Perturbation(double[] refRe, double[] refIm, int length, int max, double bailout2) {
        this.refRe = refRe;
        this.refIm = refIm;
        this.length = length;
        this.max = max;
        this.bailout2 = bailout2;
    }

    /** Digits the centre needs for a region of this size: those of a pixel plus a safety margin. */
    static int precision(Region region, int height) {
        int digits = (int) Math.ceil(-Math.log10(region.pixelSize(height)));
        return Math.max(32, digits + 24);
    }

    /**
     * Calculates the reference orbit at the centre of the region.
     *
     * @throws CancellationException if cancelled meanwhile
     */
    static Perturbation reference(FractalSettings settings, int height, BooleanSupplier cancelled) {
        MathContext mc = new MathContext(precision(settings.region(), height));
        BigDecimal cr = settings.region().re();
        BigDecimal ci = settings.region().im();
        int max = settings.maxIterations();
        double bailout2 = settings.bailout() * settings.bailout();
        // Z(0) to Z(max): one more than steps, so that the last step has a value to add to.
        int capacity = max + 1;
        double[] re = new double[Math.min(capacity, 1 << 16)];
        double[] im = new double[re.length];
        BigDecimal zr = BigDecimal.ZERO;
        BigDecimal zi = BigDecimal.ZERO;
        BigDecimal two = BigDecimal.valueOf(2);
        int n = 0;
        while (true) {
            if (n == re.length) {
                int grown = (int) Math.min(capacity, re.length * 2L);
                re = java.util.Arrays.copyOf(re, grown);
                im = java.util.Arrays.copyOf(im, grown);
            }
            double dr = zr.doubleValue();
            double di = zi.doubleValue();
            re[n] = dr;
            im[n] = di;
            n++;
            if (n == capacity || dr * dr + di * di > bailout2) break;
            if ((n & 1023) == 0 && cancelled.getAsBoolean()) throw new CancellationException();
            BigDecimal zr2 = zr.multiply(zr, mc);
            BigDecimal zi2 = zi.multiply(zi, mc);
            BigDecimal nextIm = two.multiply(zr, mc).multiply(zi, mc).add(ci, mc);
            zr = zr2.subtract(zi2, mc).add(cr, mc);
            zi = nextIm;
        }
        return new Perturbation(re, im, n, max, bailout2);
    }

    /** Values in the reference orbit, Z(0) = 0 included. */
    int referenceLength() {
        return length;
    }

    // --8<-- [start:compute]
    /** Calculates the pixel at distance {@code dcr + i·dci} from the centre. */
    void compute(double dcr, double dci, FractalData data, int index) {
        double dzr = 0;
        double dzi = 0;
        int m = 0;
        double trap = Double.MAX_VALUE;
        double zr = 0;
        double zi = 0;
        for (int n = 0; n < max; n++) {
            double ar = 2 * refRe[m] + dzr;
            double ai = 2 * refIm[m] + dzi;
            double t = ar * dzr - ai * dzi + dcr;
            dzi = ar * dzi + ai * dzr + dci;
            dzr = t;
            m++;
            zr = refRe[m] + dzr;
            zi = refIm[m] + dzi;
            double r2 = zr * zr + zi * zi;
            if (r2 < trap) trap = r2;
            if (r2 > bailout2) {
                data.set(index, FractalData.ESCAPED, n + 1, zr, zi, trap);
                return;
            }
            if (m == length - 1 || r2 < dzr * dzr + dzi * dzi) {
                dzr = zr;
                dzi = zi;
                m = 0;
            }
        }
        data.set(index, FractalData.INSIDE, max, zr, zi, trap);
    }
    // --8<-- [end:compute]
}

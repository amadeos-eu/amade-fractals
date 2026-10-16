package org.datazuul.amadeos.fractals.service.render;

import org.datazuul.amadeos.fractals.domain.Formula;
import org.datazuul.amadeos.fractals.domain.FractalSettings;

/**
 * Iterates one point in {@code double} precision, for every formula. Points inside the main cardioid and the
 * period-2 bulb of the Mandelbrot set are recognised without iterating; other orbits that close on
 * themselves are caught by a periodicity check, so the inside costs far less than the maximum iterations.
 */
final class PointCalculator {
    /** Roots of z³ − 1. */
    static final double[] ROOTS_RE = {1, -0.5, -0.5};
    static final double[] ROOTS_IM = {0, Math.sqrt(3) / 2, -Math.sqrt(3) / 2};
    private static final double CONVERGED = 1e-12;
    private static final double PERIODIC = 1e-15;

    private final Formula formula;
    private final boolean julia;
    private final double seedRe;
    private final double seedIm;
    private final int max;
    private final int power;
    private final double bailout2;

    PointCalculator(FractalSettings settings) {
        formula = settings.formula();
        julia = settings.julia();
        seedRe = settings.seedRe();
        seedIm = settings.seedIm();
        max = settings.maxIterations();
        power = settings.power();
        bailout2 = settings.bailout() * settings.bailout();
    }

    /** Calculates the point {@code re + i·im} into pixel {@code index}. */
    void compute(double re, double im, FractalData data, int index) {
        if (formula == Formula.NEWTON) {
            newton(re, im, data, index);
        } else if (formula == Formula.MANDELBROT && !julia) {
            mandelbrot(re, im, data, index);
        } else {
            escape(re, im, data, index);
        }
    }

    private void mandelbrot(double cr, double ci, FractalData data, int index) {
        double xq = cr - 0.25;
        double q = xq * xq + ci * ci;
        if (q * (q + xq) <= 0.25 * ci * ci || (cr + 1) * (cr + 1) + ci * ci <= 0.0625) {
            data.set(index, FractalData.INSIDE, max, 0, 0, trapOf(cr, ci, Math.min(max, 64)));
            return;
        }
        double zr = 0;
        double zi = 0;
        double zr2 = 0;
        double zi2 = 0;
        double trap = Double.MAX_VALUE;
        double checkRe = 0;
        double checkIm = 0;
        int checkAt = 8;
        for (int n = 0; n < max; n++) {
            zi = 2 * zr * zi + ci;
            zr = zr2 - zi2 + cr;
            zr2 = zr * zr;
            zi2 = zi * zi;
            double r2 = zr2 + zi2;
            if (r2 < trap) trap = r2;
            if (r2 > bailout2) {
                data.set(index, FractalData.ESCAPED, n + 1, zr, zi, trap);
                return;
            }
            if (Math.abs(zr - checkRe) + Math.abs(zi - checkIm) < PERIODIC) break;
            if (n == checkAt) {
                checkRe = zr;
                checkIm = zi;
                checkAt <<= 1;
            }
        }
        data.set(index, FractalData.INSIDE, max, zr, zi, trap);
    }

    /** Smallest |z|² of the first steps, for orbit trap colouring of points known to be inside. */
    private static double trapOf(double cr, double ci, int steps) {
        double zr = 0;
        double zi = 0;
        double trap = Double.MAX_VALUE;
        for (int n = 0; n < steps; n++) {
            double t = zr * zr - zi * zi + cr;
            zi = 2 * zr * zi + ci;
            zr = t;
            trap = Math.min(trap, zr * zr + zi * zi);
        }
        return trap;
    }

    private void escape(double pr, double pi, FractalData data, int index) {
        double zr = julia ? pr : 0;
        double zi = julia ? pi : 0;
        double cr = julia ? seedRe : pr;
        double ci = julia ? seedIm : pi;
        double prevRe = 0;
        double prevIm = 0;
        double trap = Double.MAX_VALUE;
        double checkRe = zr;
        double checkIm = zi;
        int checkAt = 8;
        boolean periodicityCheck = formula != Formula.PHOENIX;
        for (int n = 0; n < max; n++) {
            double nr;
            double ni;
            switch (formula) {
                case MULTIBROT -> {
                    double wr = zr;
                    double wi = zi;
                    for (int k = 1; k < power; k++) {
                        double t = wr * zr - wi * zi;
                        wi = wr * zi + wi * zr;
                        wr = t;
                    }
                    nr = wr + cr;
                    ni = wi + ci;
                }
                case BURNING_SHIP -> {
                    double ar = Math.abs(zr);
                    double ai = Math.abs(zi);
                    // Mirrored (conjugated), so that the ship stands upright with the imaginary axis up.
                    nr = ar * ar - ai * ai + cr;
                    ni = -2 * ar * ai + ci;
                }
                case TRICORN -> {
                    nr = zr * zr - zi * zi + cr;
                    ni = -2 * zr * zi + ci;
                }
                case CELTIC -> {
                    nr = Math.abs(zr * zr - zi * zi) + cr;
                    ni = 2 * zr * zi + ci;
                }
                case BUFFALO -> {
                    nr = Math.abs(zr * zr - zi * zi) + cr;
                    ni = -2 * Math.abs(zr * zi) + ci;
                }
                case PHOENIX -> {
                    nr = zr * zr - zi * zi + cr + ci * prevRe;
                    ni = 2 * zr * zi + ci * prevIm;
                    prevRe = zr;
                    prevIm = zi;
                }
                case MAGNET -> {
                    double numRe = zr * zr - zi * zi + cr - 1;
                    double numIm = 2 * zr * zi + ci;
                    double denRe = 2 * zr + cr - 2;
                    double denIm = 2 * zi + ci;
                    double d2 = denRe * denRe + denIm * denIm;
                    if (d2 == 0) {
                        nr = Double.MAX_VALUE;
                        ni = 0;
                    } else {
                        double qr = (numRe * denRe + numIm * denIm) / d2;
                        double qi = (numIm * denRe - numRe * denIm) / d2;
                        nr = qr * qr - qi * qi;
                        ni = 2 * qr * qi;
                    }
                }
                default -> {
                    nr = zr * zr - zi * zi + cr;
                    ni = 2 * zr * zi + ci;
                }
            }
            zr = nr;
            zi = ni;
            double r2 = zr * zr + zi * zi;
            if (r2 < trap) trap = r2;
            if (r2 > bailout2 || Double.isNaN(r2)) {
                data.set(index, FractalData.ESCAPED, n + 1, zr, zi, trap);
                return;
            }
            if (formula == Formula.MAGNET) {
                double dr = zr - 1;
                if (dr * dr + zi * zi < CONVERGED) {
                    data.set(index, FractalData.CONVERGED, n + 1, zr, zi, trap);
                    return;
                }
            }
            if (periodicityCheck) {
                if (Math.abs(zr - checkRe) + Math.abs(zi - checkIm) < PERIODIC) break;
                if (n == checkAt) {
                    checkRe = zr;
                    checkIm = zi;
                    checkAt <<= 1;
                }
            }
        }
        data.set(index, FractalData.INSIDE, max, zr, zi, trap);
    }

    private void newton(double zr, double zi, FractalData data, int index) {
        double trap = zr * zr + zi * zi;
        for (int n = 0; n < max; n++) {
            double z2r = zr * zr - zi * zi;
            double z2i = 2 * zr * zi;
            double z3r = z2r * zr - z2i * zi;
            double z3i = z2r * zi + z2i * zr;
            // z − (z³ − 1) / 3z² = (2z³ + 1) / 3z²
            double numRe = 2 * z3r + 1;
            double numIm = 2 * z3i;
            double denRe = 3 * z2r;
            double denIm = 3 * z2i;
            double d2 = denRe * denRe + denIm * denIm;
            if (d2 == 0) break;
            zr = (numRe * denRe + numIm * denIm) / d2;
            zi = (numIm * denRe - numRe * denIm) / d2;
            trap = Math.min(trap, zr * zr + zi * zi);
            for (int k = 0; k < 3; k++) {
                double dr = zr - ROOTS_RE[k];
                double di = zi - ROOTS_IM[k];
                if (dr * dr + di * di < CONVERGED) {
                    data.set(index, (byte) (FractalData.CONVERGED + k), n + 1, zr, zi, trap);
                    return;
                }
            }
        }
        data.set(index, FractalData.INSIDE, max, zr, zi, trap);
    }
}

package org.datazuul.amadeos.fractals.domain;

/**
 * How many iterations a zoom needs: the deeper, the longer the orbits near the boundary run before they
 * escape. Without enough, the edge of the set fills with the inside colour.
 */
public final class Iterations {
    private Iterations() {
    }

    /** A rule of thumb that grows with the decimal digits of the magnification. */
    public static int suggested(double magnification) {
        double digits = Math.max(0, Math.log10(Math.max(1, magnification)));
        double value = 300 + 150 * Math.pow(digits, 1.4);
        return (int) Math.min(FractalSettings.MAX_ITERATIONS, Math.round(value / 50) * 50);
    }
}

package org.datazuul.amadeos.fractals.service.render;

import org.datazuul.amadeos.fractals.domain.FractalSettings;

import java.util.Objects;

/**
 * The result of a calculation, per pixel: how the orbit ended, after how many steps, its last value and its
 * closest approach to 0. Colouring works from these alone, so palettes and colour cycling never recalculate.
 */
public final class FractalData {
    /** Still bounded after the maximum number of iterations. */
    public static final byte INSIDE = 0;
    /** |z| passed the bailout. */
    public static final byte ESCAPED = 1;
    /** Converged to root k: the state is {@code CONVERGED + k}. */
    public static final byte CONVERGED = 2;

    private final FractalSettings settings;
    private final int width;
    private final int height;
    private final int[] iterations;
    private final byte[] states;
    private final float[] lastRe;
    private final float[] lastIm;
    private final float[] trap;
    private boolean perturbation;

    public FractalData(FractalSettings settings, int width, int height) {
        this.settings = Objects.requireNonNull(settings, "settings");
        if (width < 1 || height < 1) throw new IllegalArgumentException("Empty picture " + width + "×" + height);
        this.width = width;
        this.height = height;
        int n = Math.multiplyExact(width, height);
        iterations = new int[n];
        states = new byte[n];
        lastRe = new float[n];
        lastIm = new float[n];
        trap = new float[n];
    }

    public FractalSettings settings() {
        return settings;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int size() {
        return iterations.length;
    }

    /** True if the picture was calculated with perturbation (deep zoom). */
    public boolean perturbation() {
        return perturbation;
    }

    void setPerturbation(boolean used) {
        perturbation = used;
    }

    /** Stores the result for one pixel; {@code trap} is the smallest |z|² of the orbit. */
    public void set(int index, byte state, int n, double re, double im, double trapSquared) {
        states[index] = state;
        iterations[index] = n;
        lastRe[index] = (float) re;
        lastIm[index] = (float) im;
        trap[index] = (float) trapSquared;
    }

    public byte state(int index) {
        return states[index];
    }

    public int iterations(int index) {
        return iterations[index];
    }

    public float lastRe(int index) {
        return lastRe[index];
    }

    public float lastIm(int index) {
        return lastIm[index];
    }

    /** Smallest |z|² along the orbit. */
    public float trap(int index) {
        return trap[index];
    }

    /** Points inside the set, for tests and statistics. */
    public int countInside() {
        int count = 0;
        for (byte s : states) if (s == INSIDE) count++;
        return count;
    }
}

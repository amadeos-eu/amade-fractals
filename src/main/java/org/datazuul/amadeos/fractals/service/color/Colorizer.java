package org.datazuul.amadeos.fractals.service.color;

import org.datazuul.amadeos.fractals.domain.ColorSettings;
import org.datazuul.amadeos.fractals.domain.Coloring;
import org.datazuul.amadeos.fractals.domain.Palette.Gradient;
import org.datazuul.amadeos.fractals.service.render.FractalData;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Turns calculated values into colours. Fast enough to run for every step of colour cycling: no
 * calculation, only a lookup in the palette per pixel.
 */
public final class Colorizer {
    /** Convergent formulas: brightness falls by this factor per iteration at density 1. */
    private static final double CONVERGE_FADE = 0.06;

    private Colorizer() {
    }

    /** A new RGB picture of the data. */
    public static BufferedImage image(FractalData data, ColorSettings colors, Gradient gradient) {
        BufferedImage image = new BufferedImage(data.width(), data.height(), BufferedImage.TYPE_INT_RGB);
        colorize(data, colors, gradient, image);
        return image;
    }

    /** Paints the data into a picture of the same size, type {@link BufferedImage#TYPE_INT_RGB}. */
    public static void colorize(FractalData data, ColorSettings colors, Gradient gradient, BufferedImage target) {
        if (target.getWidth() != data.width() || target.getHeight() != data.height()
                || target.getType() != BufferedImage.TYPE_INT_RGB) {
            throw new IllegalArgumentException("Picture does not match the data");
        }
        int[] pixels = ((DataBufferInt) target.getRaster().getDataBuffer()).getData();
        double logDegree = Math.log(data.settings().degree());
        double scale = colors.density() / ColorSettings.CYCLE;
        double offset = colors.offset();
        Coloring coloring = colors.coloring();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = color(data, i, coloring, scale, offset, logDegree, gradient);
        }
    }

    /** The colour of one pixel. */
    static int color(FractalData data, int i, Coloring coloring, double scale, double offset, double logDegree,
                     Gradient gradient) {
        byte state = data.state(i);
        if (coloring == Coloring.ORBIT_TRAP) {
            // Distance of the closest approach to 0, on a logarithmic scale: rings around the trap.
            double distance = Math.sqrt(Math.max(0, data.trap(i)));
            double position = -Math.log(distance + 1e-9) * scale * 16 + offset;
            int rgb = gradient.at(position);
            return state == FractalData.INSIDE ? darken(rgb, 0.6) : rgb;
        }
        if (state == FractalData.INSIDE) return gradient.inside();
        int n = data.iterations(i);
        if (state >= FractalData.CONVERGED) {
            int root = state - FractalData.CONVERGED;
            int rgb = gradient.at(root / 3.0 + offset);
            double fade = Math.exp(-n * scale * ColorSettings.CYCLE * CONVERGE_FADE);
            return darken(rgb, 0.15 + 0.85 * fade);
        }
        double value = switch (coloring) {
            case SMOOTH -> smooth(n, data.lastRe(i), data.lastIm(i), logDegree);
            case BINARY_DECOMPOSITION -> n + (data.lastIm(i) < 0 ? 0.5 / scale : 0);
            default -> n;
        };
        return gradient.at(value * scale + offset);
    }

    /** n + 1 − log(log |z|) / log d: continuous where the plain count jumps. */
    static double smooth(int n, double re, double im, double logDegree) {
        double logModulus = 0.5 * Math.log(re * re + im * im);
        if (!(logModulus > 0)) return n;
        double value = n + 1 - Math.log(logModulus) / logDegree;
        return Double.isFinite(value) ? Math.max(0, value) : n;
    }

    private static int darken(int rgb, double factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }
}

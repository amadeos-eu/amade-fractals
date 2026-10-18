package eu.amadeos.apps.fractals.domain;

import java.util.Objects;

/**
 * How the calculated values become colours.
 *
 * @param coloring how a point gets its place in the palette
 * @param palette  the colours
 * @param density  palette repeats per {@link #CYCLE} iterations; higher shows more colour changes
 * @param offset   shift of the palette, 0 to 1; colour cycling moves it
 */
public record ColorSettings(Coloring coloring, Palette palette, double density, double offset) {
    /** Iterations per pass through the palette at density 1. */
    public static final double CYCLE = 64;
    public static final double MIN_DENSITY = 0.05;
    public static final double MAX_DENSITY = 20;

    public static final ColorSettings DEFAULT = new ColorSettings(Coloring.SMOOTH, Palette.CLASSIC, 1, 0);

    public ColorSettings {
        Objects.requireNonNull(coloring, "coloring");
        Objects.requireNonNull(palette, "palette");
        density = Double.isFinite(density) ? Math.max(MIN_DENSITY, Math.min(MAX_DENSITY, density)) : 1;
        offset = Double.isFinite(offset) ? offset - Math.floor(offset) : 0;
    }

    public ColorSettings withColoring(Coloring newColoring) {
        return new ColorSettings(newColoring, palette, density, offset);
    }

    public ColorSettings withPalette(Palette newPalette) {
        return new ColorSettings(coloring, newPalette, density, offset);
    }

    public ColorSettings withDensity(double newDensity) {
        return new ColorSettings(coloring, palette, newDensity, offset);
    }

    public ColorSettings withOffset(double newOffset) {
        return new ColorSettings(coloring, palette, density, newOffset);
    }
}

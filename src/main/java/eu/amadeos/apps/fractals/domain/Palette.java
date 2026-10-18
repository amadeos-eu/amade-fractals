package eu.amadeos.apps.fractals.domain;

import eu.amadeos.commons.theme.DesktopTheme;

import java.awt.Color;
import java.util.Arrays;

/**
 * Colour gradients. Each one is cyclic, so that it can repeat and cycle without a seam. {@link #DESKTOP} takes
 * its colours from the amadeOS colour theme and changes with it.
 */
public enum Palette {
    /** Deep blue, white, gold and black, as in the familiar pictures of the Mandelbrot set. */
    CLASSIC(0x000000, 0, 0x000764, 0.16, 0x206BCB, 0.42, 0xEDFFFF, 0.6425, 0xFFAA00, 0.8575, 0x000200),
    FIRE(0x000000, 0, 0x000000, 0.25, 0x800000, 0.5, 0xFF6000, 0.7, 0xFFDC40, 0.85, 0xFFFFDC),
    OCEAN(0x000000, 0, 0x00001E, 0.3, 0x003C78, 0.55, 0x00A0C8, 0.75, 0xB4F0FF, 0.9, 0x145A8C),
    EARTH(0x000000, 0, 0x140C04, 0.3, 0x5A3C14, 0.55, 0xA08C3C, 0.75, 0xDCD2A0, 0.9, 0x3C6428),
    ELECTRIC(0x000000, 0, 0x000000, 0.2, 0x5000A0, 0.45, 0xFF00C8, 0.65, 0xFFFFFF, 0.85, 0x00C8FF),
    RAINBOW(0x000000, 0, 0xFF0000, 1 / 6.0, 0xFFFF00, 2 / 6.0, 0x00FF00, 3 / 6.0, 0x00FFFF, 4 / 6.0, 0x0000FF,
            5 / 6.0, 0xFF00FF),
    GRAYSCALE(0x000000, 0, 0x000000, 0.5, 0xFFFFFF),
    /** The colours of the desktop theme. */
    DESKTOP(0x000000);

    /** Entries of the lookup table: fine enough that neighbours differ by less than one step per channel. */
    public static final int STEPS = 1024;

    private final int inside;
    private final double[] positions;
    private final int[] colors;

    Palette(int inside, double... stops) {
        this.inside = inside;
        this.positions = new double[stops.length / 2];
        this.colors = new int[stops.length / 2];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = stops[2 * i];
            colors[i] = (int) stops[2 * i + 1];
        }
    }

    /** The colours as lookup table of {@link #STEPS} RGB values and the colour of the inside. */
    public Gradient gradient(DesktopTheme theme) {
        if (this == DESKTOP) {
            DesktopTheme t = theme == null ? DesktopTheme.WARM_MOTIF : theme;
            return Gradient.of(t.darkShadow().getRGB() & 0xFFFFFF, new double[]{0, 0.2, 0.45, 0.6, 0.8},
                    new int[]{rgb(t.darkShadow()), rgb(t.shadow()), rgb(t.highlight()), rgb(t.selectionBackground()),
                            rgb(t.base())});
        }
        return Gradient.of(inside, positions, colors);
    }

    private static int rgb(Color c) {
        return c.getRGB() & 0xFFFFFF;
    }

    /**
     * A cyclic gradient as lookup table.
     *
     * @param table  {@link #STEPS} RGB values, from position 0 up to (but without) 1
     * @param inside the colour of points inside the set
     */
    public record Gradient(int[] table, int inside) {
        static Gradient of(int inside, double[] positions, int[] colors) {
            int[] table = new int[STEPS];
            int n = positions.length;
            for (int i = 0; i < STEPS; i++) {
                double t = i / (double) STEPS;
                int k = n - 1;
                while (k > 0 && positions[k] > t) k--;
                int next = (k + 1) % n;
                double start = positions[k];
                double end = next == 0 ? 1 + positions[0] : positions[next];
                double f = end > start ? (t - start) / (end - start) : 0;
                table[i] = mix(colors[k], colors[next], f);
            }
            return new Gradient(table, inside);
        }

        /** The colour at a position; whole numbers are added or removed, so 1.25 is 0.25. */
        public int at(double position) {
            double f = position - Math.floor(position);
            return table[Math.min(STEPS - 1, (int) (f * STEPS))];
        }

        private static int mix(int a, int b, double f) {
            int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
            int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
            int bl = (int) Math.round((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
            return (r << 16) | (g << 8) | bl;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Gradient g && g.inside == inside && Arrays.equals(g.table, table);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(table) + inside;
        }

        @Override
        public String toString() {
            return "Gradient[inside=" + Integer.toHexString(inside) + "]";
        }
    }
}

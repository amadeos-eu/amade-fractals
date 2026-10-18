package eu.amadeos.fractals.service.color;

import eu.amadeos.fractals.domain.ColorSettings;
import eu.amadeos.fractals.domain.Coloring;
import eu.amadeos.fractals.domain.Formula;
import eu.amadeos.fractals.domain.FractalSettings;
import eu.amadeos.fractals.domain.Palette;
import eu.amadeos.fractals.service.render.FractalData;
import eu.amadeos.fractals.service.render.Renderer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorizerTest {
    private final Palette.Gradient gradient = Palette.CLASSIC.gradient(null);

    private static FractalData data(byte state, int n, double re, double im, double trap) {
        FractalData d = new FractalData(FractalSettings.DEFAULT, 1, 1);
        d.set(0, state, n, re, im, trap);
        return d;
    }

    private int color(FractalData d, ColorSettings c) {
        return Colorizer.image(d, c, gradient).getRGB(0, 0) & 0xFFFFFF;
    }

    @Test
    void insideTakesThePalettesInsideColour() {
        assertEquals(gradient.inside(), color(data(FractalData.INSIDE, 500, 0.1, 0.1, 0.01), ColorSettings.DEFAULT));
    }

    @Test
    void smoothValuesAreContinuousAcrossBands() {
        double log2 = Math.log(2);
        // One more step of z² squares |z| and adds one to the count: the smooth value stays the same.
        double before = Colorizer.smooth(10, 64.5, 0, log2);
        double after = Colorizer.smooth(11, 64.5 * 64.5, 0, log2);
        assertEquals(before, after, 1e-9);
        assertTrue(Colorizer.smooth(10, 64.5, 0, log2) < 10);
    }

    @Test
    void offsetShiftsThePalette() {
        FractalData d = data(FractalData.ESCAPED, 16, 100, 0, 1);
        ColorSettings bands = ColorSettings.DEFAULT.withColoring(Coloring.ESCAPE_TIME);
        assertEquals(gradient.at(16 / ColorSettings.CYCLE), color(d, bands));
        assertEquals(gradient.at(16 / ColorSettings.CYCLE + 0.3), color(d, bands.withOffset(0.3)));
        assertEquals(gradient.at(2 * 16 / ColorSettings.CYCLE), color(d, bands.withDensity(2)));
    }

    @Test
    void binaryDecompositionSplitsBySign() {
        ColorSettings binary = ColorSettings.DEFAULT.withColoring(Coloring.BINARY_DECOMPOSITION);
        assertNotEquals(color(data(FractalData.ESCAPED, 16, 100, 5, 1), binary),
                color(data(FractalData.ESCAPED, 16, 100, -5, 1), binary));
    }

    @Test
    void orbitTrapColoursTheInsideToo() {
        ColorSettings trap = ColorSettings.DEFAULT.withColoring(Coloring.ORBIT_TRAP);
        int near = color(data(FractalData.INSIDE, 500, 0, 0, 0.0001), trap);
        int far = color(data(FractalData.INSIDE, 500, 0, 0, 0.5), trap);
        assertNotEquals(near, far);
        assertNotEquals(gradient.inside(), near);
    }

    @Test
    void newtonRootsGetDifferentColours() {
        FractalSettings newton = FractalSettings.of(Formula.NEWTON).withMaxIterations(50);
        FractalData d = Renderer.direct().render(newton, 60, 60, () -> false, (x, y) -> { });
        BufferedImage image = Colorizer.image(d, ColorSettings.DEFAULT.withPalette(Palette.RAINBOW),
                Palette.RAINBOW.gradient(null));
        // Right: root 1; upper left: root e^(2πi/3); lower left: its conjugate.
        int right = image.getRGB(55, 30);
        int upper = image.getRGB(8, 4);
        int lower = image.getRGB(8, 55);
        assertNotEquals(right, upper);
        assertNotEquals(upper, lower);
    }

    @Test
    void recolouringRequiresAMatchingPicture() {
        FractalData d = data(FractalData.INSIDE, 1, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> Colorizer.colorize(d, ColorSettings.DEFAULT, gradient,
                new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB)));
    }
}

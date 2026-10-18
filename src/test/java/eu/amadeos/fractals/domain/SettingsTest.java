package eu.amadeos.fractals.domain;

import eu.amadeos.commons.theme.DesktopTheme;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {
    @Test
    void fractalSettingsKeepTheirValuesInRange() {
        FractalSettings s = new FractalSettings(Formula.MULTIBROT, 99, true, 0.1, 0.2, 1, 0.5, Region.of(0, 0, 3));
        assertEquals(Formula.MAX_POWER, s.power());
        assertEquals(FractalSettings.MIN_ITERATIONS, s.maxIterations());
        assertEquals(FractalSettings.MIN_BAILOUT, s.bailout());
        assertEquals(12, s.degree(), 0);
        // Newton has no Julia form.
        assertFalse(s.withFormula(Formula.NEWTON).julia());
        assertTrue(s.withFormula(Formula.TRICORN).julia());
    }

    @Test
    void everyFormulaOpensWithItsWholeFractal() {
        for (Formula formula : Formula.values()) {
            FractalSettings s = FractalSettings.of(formula);
            assertEquals(formula, s.formula());
            assertEquals(formula.home(s.julia()), s.region());
            assertEquals(formula.juliaByDefault(), s.julia());
        }
        assertTrue(FractalSettings.of(Formula.PHOENIX).julia());
    }

    @Test
    void perturbationOnlyWhereDoublesRunOut() {
        FractalSettings mandelbrot = FractalSettings.DEFAULT;
        assertFalse(mandelbrot.needsPerturbation(600));
        assertTrue(mandelbrot.withRegion(Region.of(-0.75, 0.1, 1e-14)).needsPerturbation(600));
        assertFalse(mandelbrot.withRegion(Region.of(-0.75, 0.1, 1e-10)).needsPerturbation(600));
        // Not for Julia sets or other formulas: they stop at the limit instead.
        FractalSettings julia = mandelbrot.withJulia(true, -0.8, 0.156).withRegion(Region.of(0, 0, 1e-14));
        assertFalse(julia.needsPerturbation(600));
        assertEquals(Region.MIN_SIZE, mandelbrot.minSize(600));
        assertTrue(julia.minSize(600) > 1e-13, () -> "min " + julia.minSize(600));
    }

    @Test
    void colorSettingsWrapTheOffsetAndClampTheDensity() {
        ColorSettings c = new ColorSettings(Coloring.SMOOTH, Palette.FIRE, 1000, 2.25);
        assertEquals(ColorSettings.MAX_DENSITY, c.density());
        assertEquals(0.25, c.offset(), 1e-12);
        assertEquals(0.75, c.withOffset(-0.25).offset(), 1e-12);
    }

    @Test
    void palettesAreCyclicLookupTables() {
        for (Palette palette : Palette.values()) {
            Palette.Gradient g = palette.gradient(DesktopTheme.WARM_MOTIF);
            assertEquals(Palette.STEPS, g.table().length);
            assertEquals(g.at(0.25), g.at(1.25));
            assertEquals(g.at(0.25), g.at(-0.75));
        }
        Palette.Gradient classic = Palette.CLASSIC.gradient(null);
        assertEquals(0x000764, classic.at(0));
        assertEquals(0x000000, classic.inside());
        // Smooth: neighbours in the table differ by little.
        int[] t = classic.table();
        for (int i = 1; i < t.length; i++) {
            assertTrue(Math.abs(((t[i] >> 8) & 0xFF) - ((t[i - 1] >> 8) & 0xFF)) <= 3, "step at " + i);
        }
    }

    @Test
    void desktopPaletteFollowsTheTheme() {
        Palette.Gradient warm = Palette.DESKTOP.gradient(DesktopTheme.WARM_MOTIF);
        Palette.Gradient dark = Palette.DESKTOP.gradient(DesktopTheme.DARK_SLATE);
        assertNotEquals(warm, dark);
        assertEquals(DesktopTheme.DARK_SLATE.darkShadow().getRGB() & 0xFFFFFF, dark.at(0));
    }

    @Test
    void placesAreValid() {
        for (Place place : Place.values()) {
            FractalSettings s = place.settings();
            assertTrue(s.region().size() >= s.minSize(1000), place + " is too deep for its formula");
            assertTrue(place.key().startsWith("place."));
        }
        assertTrue(Place.DEEP_SEAHORSE.settings().needsPerturbation(800));
        assertTrue(Place.DOUADY_RABBIT.settings().julia());
    }

    @Test
    void iterationsGrowWithTheZoom() {
        assertEquals(300, Iterations.suggested(1));
        int previous = 0;
        for (double m = 1; m < 1e60; m *= 1000) {
            int n = Iterations.suggested(m);
            assertTrue(n >= previous, () -> "at " + n);
            previous = n;
        }
        assertTrue(Iterations.suggested(1e20) > 5000);
    }

    @Test
    void imageFormatsByExtension() {
        assertEquals(Optional.of(ImageFormat.JPEG), ImageFormat.forPath(Path.of("a.JPEG")));
        assertEquals(Optional.empty(), ImageFormat.forPath(Path.of("a.fractal")));
        assertEquals(Path.of("x/a.png"), ImageFormat.PNG.withExtension(Path.of("x/a.jpg")));
        assertEquals(Path.of("a.b.jpg"), ImageFormat.JPEG.withExtension(Path.of("a.b")));
    }
}

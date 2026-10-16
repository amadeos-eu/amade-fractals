package org.datazuul.amadeos.fractals.repository;

import org.datazuul.amadeos.fractals.domain.ColorSettings;
import org.datazuul.amadeos.fractals.domain.Coloring;
import org.datazuul.amadeos.fractals.domain.Formula;
import org.datazuul.amadeos.fractals.domain.FractalSettings;
import org.datazuul.amadeos.fractals.domain.ImageFormat;
import org.datazuul.amadeos.fractals.domain.Palette;
import org.datazuul.amadeos.fractals.domain.Place;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences.Bar;
import org.datazuul.amadeos.fractals.repository.ParameterFiles.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryTest {
    @TempDir
    Path dir;

    @Test
    void parameterFilesKeepEveryDigit() throws IOException {
        FractalSettings deep = Place.DEEP_SEAHORSE.settings();
        ColorSettings colors = new ColorSettings(Coloring.ORBIT_TRAP, Palette.OCEAN, 2.5, 0.125);
        Path file = dir.resolve("deep.fractal");
        new ParameterFiles().write(file, new Parameters(deep, colors));
        Parameters read = new ParameterFiles().read(file);
        assertEquals(deep, read.fractal());
        assertEquals(colors, read.colors());
        assertTrue(Files.readString(file).contains("center.re=-0.743643887037158704752191506114774"));
    }

    @Test
    void juliaAndPowerSurviveTheRoundTrip() throws IOException {
        FractalSettings julia = FractalSettings.of(Formula.MULTIBROT).withPower(5).withJulia(true, 0.25, -0.5);
        Parameters read = ParameterFiles.parse(ParameterFiles.format(new Parameters(julia, ColorSettings.DEFAULT)));
        assertEquals(julia, read.fractal());
    }

    @Test
    void missingValuesTakeTheDefaults() throws IOException {
        Parameters read = ParameterFiles.parse("format=amade-fractals/1\nformula=burning-ship\n");
        assertEquals(FractalSettings.of(Formula.BURNING_SHIP), read.fractal());
        assertEquals(ColorSettings.DEFAULT, read.colors());
    }

    @Test
    void rejectsOtherFilesAndBadValues() {
        assertThrows(IOException.class, () -> ParameterFiles.parse("formula=MANDELBROT\n"));
        assertThrows(IOException.class, () -> ParameterFiles.parse("format=amade-fractals/1\nformula=SPIRAL\n"));
        assertThrows(IOException.class, () -> ParameterFiles.parse("format=amade-fractals/1\nsize=-1\n"));
        assertThrows(IOException.class, () -> ParameterFiles.parse("format=amade-fractals/1\ncenter.re=abc\n"));
    }

    @Test
    void imagesAreWrittenWholeOrNotAtAll() throws IOException {
        BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        image.setRGB(5, 5, 0x123456);
        ImageFiles files = new ImageFiles();
        Path png = dir.resolve("sub/a.png");
        files.write(image, png, ImageFormat.PNG);
        assertEquals(0x123456, ImageIO.read(png.toFile()).getRGB(5, 5) & 0xFFFFFF);
        Path jpg = dir.resolve("a.jpg");
        files.write(image, jpg, ImageFormat.JPEG);
        assertEquals(40, ImageIO.read(jpg.toFile()).getWidth());
        assertThrows(IOException.class, () -> files.writeAtomically(dir.resolve("b.png"), out -> {
            out.write(1);
            throw new IOException("disk full");
        }));
        assertFalse(Files.exists(dir.resolve("b.png")));
        try (Stream<Path> left = Files.list(dir)) {
            assertTrue(left.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void preferencesAreStoredAtOnce() {
        Path file = dir.resolve("fractals.properties");
        FractalsPreferences preferences = new FractalsPreferences(file);
        assertTrue(preferences.isVisible(Bar.SETTINGS));
        assertTrue(preferences.autoIterations());
        assertEquals(FractalsPreferences.DEFAULT_EXPORT_WIDTH, preferences.exportWidth());
        preferences.setVisible(Bar.SETTINGS, false);
        preferences.setAutoIterations(false);
        preferences.setExportSize(1920, 1080);
        FractalsPreferences again = new FractalsPreferences(file);
        assertFalse(again.isVisible(Bar.SETTINGS));
        assertTrue(again.isVisible(Bar.TOOLBAR));
        assertFalse(again.autoIterations());
        assertEquals(1920, again.exportWidth());
        assertEquals(1080, again.exportHeight());
    }
}

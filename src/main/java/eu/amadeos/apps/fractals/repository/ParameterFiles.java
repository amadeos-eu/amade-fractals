package eu.amadeos.apps.fractals.repository;

import eu.amadeos.apps.fractals.domain.ColorSettings;
import eu.amadeos.apps.fractals.domain.Coloring;
import eu.amadeos.apps.fractals.domain.Formula;
import eu.amadeos.apps.fractals.domain.FractalSettings;
import eu.amadeos.apps.fractals.domain.Palette;
import eu.amadeos.apps.fractals.domain.Region;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Parameter files ({@code .fractal}): formula, place, iterations and colours in a UTF-8 properties file, so
 * that a view can be bookmarked, shared and opened again. The centre keeps all its digits.
 */
public class ParameterFiles {
    public static final String EXTENSION = "fractal";
    private static final String MAGIC = "amade-fractals";

    /** What a parameter file holds. */
    public record Parameters(FractalSettings fractal, ColorSettings colors) {
    }

    private final ImageFiles files;

    public ParameterFiles(ImageFiles files) {
        this.files = files;
    }

    public ParameterFiles() {
        this(new ImageFiles());
    }

    /**
     * Reads a parameter file.
     *
     * @throws IOException if it cannot be read or is not a parameter file of ours
     */
    public Parameters read(Path path) throws IOException {
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    public void write(Path path, Parameters parameters) throws IOException {
        files.writeAtomically(path, out -> out.write(format(parameters).getBytes(StandardCharsets.UTF_8)));
    }

    /** The parameters as text, also for the clipboard. */
    public static String format(Parameters parameters) {
        FractalSettings f = parameters.fractal();
        ColorSettings c = parameters.colors();
        Properties p = new Properties();
        p.setProperty("format", MAGIC + "/1");
        p.setProperty("formula", f.formula().name());
        p.setProperty("power", String.valueOf(f.power()));
        p.setProperty("julia", String.valueOf(f.julia()));
        p.setProperty("seed.re", String.valueOf(f.seedRe()));
        p.setProperty("seed.im", String.valueOf(f.seedIm()));
        p.setProperty("iterations", String.valueOf(f.maxIterations()));
        p.setProperty("bailout", String.valueOf(f.bailout()));
        p.setProperty("center.re", f.region().re().toPlainString());
        p.setProperty("center.im", f.region().im().toPlainString());
        p.setProperty("size", String.valueOf(f.region().size()));
        p.setProperty("coloring", c.coloring().name());
        p.setProperty("palette", c.palette().name());
        p.setProperty("density", String.valueOf(c.density()));
        p.setProperty("offset", String.valueOf(c.offset()));
        StringWriter text = new StringWriter();
        try {
            p.store(text, "amade-fractals parameters");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return text.toString();
    }

    /**
     * Reads parameters from text; values that are missing take the defaults.
     *
     * @throws IOException if the text is not a parameter file of ours or a value is invalid
     */
    public static Parameters parse(String text) throws IOException {
        Properties p = new Properties();
        p.load(new StringReader(text));
        if (!p.getProperty("format", "").startsWith(MAGIC)) throw new IOException("Not an amade-fractals parameter file");
        try {
            Formula formula = enumValue(p, "formula", Formula.class, Formula.MANDELBROT);
            FractalSettings defaults = FractalSettings.of(formula);
            Region home = defaults.region();
            Region region = new Region(
                    new java.math.BigDecimal(p.getProperty("center.re", home.re().toPlainString()).strip()),
                    new java.math.BigDecimal(p.getProperty("center.im", home.im().toPlainString()).strip()),
                    number(p, "size", home.size()));
            FractalSettings fractal = new FractalSettings(formula,
                    (int) number(p, "power", defaults.power()),
                    Boolean.parseBoolean(p.getProperty("julia", String.valueOf(defaults.julia())).strip()),
                    number(p, "seed.re", defaults.seedRe()),
                    number(p, "seed.im", defaults.seedIm()),
                    (int) Math.min(Integer.MAX_VALUE, number(p, "iterations", defaults.maxIterations())),
                    number(p, "bailout", defaults.bailout()),
                    region);
            ColorSettings colors = new ColorSettings(
                    enumValue(p, "coloring", Coloring.class, ColorSettings.DEFAULT.coloring()),
                    enumValue(p, "palette", Palette.class, ColorSettings.DEFAULT.palette()),
                    number(p, "density", ColorSettings.DEFAULT.density()),
                    number(p, "offset", ColorSettings.DEFAULT.offset()));
            return new Parameters(fractal, colors);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid value: " + e.getMessage(), e);
        }
    }

    private static double number(Properties p, String key, double fallback) {
        String value = p.getProperty(key);
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value.strip());
    }

    private static <E extends Enum<E>> E enumValue(Properties p, String key, Class<E> type, E fallback) {
        String value = p.getProperty(key);
        return value == null || value.isBlank() ? fallback
                : Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}

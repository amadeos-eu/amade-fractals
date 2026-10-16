package org.datazuul.amadeos.fractals.repository;

import com.google.inject.Inject;
import org.datazuul.amadeos.commons.config.AppDirectories;
import org.datazuul.amadeos.commons.config.PropertiesFile;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Settings of amade-fractals in {@code ~/.config/amadeos/fractals/fractals.properties}: which bars the window
 * shows, whether zooming raises the iterations, and the size of the last export. Each change is written at
 * once.
 */
public class FractalsPreferences {
    public static final String FILE = "fractals.properties";
    public static final int DEFAULT_EXPORT_WIDTH = 3840;
    public static final int DEFAULT_EXPORT_HEIGHT = 2160;
    public static final int MAX_EXPORT_SIDE = 20_000;

    public enum Bar {
        TOOLBAR, SETTINGS, STATUS_BAR;

        String key() {
            return "show." + name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
    }

    private final Path file;
    private final Properties properties;

    @Inject
    public FractalsPreferences(AppDirectories directories) {
        this(directories.configFile(FILE));
    }

    public FractalsPreferences(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        this.properties = PropertiesFile.load(file);
    }

    public boolean isVisible(Bar bar) {
        return !"false".equals(properties.getProperty(bar.key()));
    }

    public void setVisible(Bar bar, boolean visible) {
        set(bar.key(), String.valueOf(visible));
    }

    /** Whether zooming in raises the iterations to what the depth needs; on unless switched off. */
    public boolean autoIterations() {
        return !"false".equals(properties.getProperty("iterations.auto"));
    }

    public void setAutoIterations(boolean auto) {
        set("iterations.auto", String.valueOf(auto));
    }

    public int exportWidth() {
        return intValue("export.width", DEFAULT_EXPORT_WIDTH, 16, MAX_EXPORT_SIDE);
    }

    public int exportHeight() {
        return intValue("export.height", DEFAULT_EXPORT_HEIGHT, 16, MAX_EXPORT_SIDE);
    }

    public void setExportSize(int width, int height) {
        set("export.width", String.valueOf(width));
        set("export.height", String.valueOf(height));
    }

    private int intValue(String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(properties.getProperty(key, "").strip());
            return value < min || value > max ? fallback : value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Stores the value at once.
     *
     * @throws java.io.UncheckedIOException if the file cannot be written
     */
    private void set(String key, String value) {
        if (value.equals(properties.getProperty(key))) return;
        properties.setProperty(key, value);
        PropertiesFile.store(file, properties, "amade-fractals");
    }
}

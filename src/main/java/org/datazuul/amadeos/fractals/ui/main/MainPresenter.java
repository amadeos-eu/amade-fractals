package org.datazuul.amadeos.fractals.ui.main;

import com.google.inject.Inject;
import org.datazuul.amadeos.commons.i18n.Language;
import org.datazuul.amadeos.commons.i18n.LocaleSettings;
import org.datazuul.amadeos.commons.i18n.Messages;
import org.datazuul.amadeos.commons.swing.AmadeUI;
import org.datazuul.amadeos.commons.theme.DesktopTheme;
import org.datazuul.amadeos.fractals.domain.ColorSettings;
import org.datazuul.amadeos.fractals.domain.Coloring;
import org.datazuul.amadeos.fractals.domain.Formula;
import org.datazuul.amadeos.fractals.domain.FractalSettings;
import org.datazuul.amadeos.fractals.domain.ImageFormat;
import org.datazuul.amadeos.fractals.domain.Iterations;
import org.datazuul.amadeos.fractals.domain.Palette;
import org.datazuul.amadeos.fractals.domain.Palette.Gradient;
import org.datazuul.amadeos.fractals.domain.Place;
import org.datazuul.amadeos.fractals.domain.Region;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences.Bar;
import org.datazuul.amadeos.fractals.repository.ImageFiles;
import org.datazuul.amadeos.fractals.repository.ParameterFiles;
import org.datazuul.amadeos.fractals.repository.ParameterFiles.Parameters;
import org.datazuul.amadeos.fractals.service.color.Colorizer;
import org.datazuul.amadeos.fractals.service.render.FractalData;
import org.datazuul.amadeos.fractals.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * The actions of the main window, without Swing: navigate (zoom, frame, pan, back and forward), change
 * formula, Julia seed and iterations, colour and cycle, open and save parameters, save and export pictures.
 *
 * <p>Every change of the {@link FractalSettings} goes into the history and starts a new calculation; the
 * one still running is cancelled. Colour changes only recolour the values already calculated.
 *
 * <p>All methods run on the event dispatch thread. Calculations and file access run in the background and
 * continue there through the {@link Ui} executor.
 */
public final class MainPresenter {
    private static final Logger LOG = LoggerFactory.getLogger(MainPresenter.class);
    /** Back steps kept. */
    static final int HISTORY = 100;
    /** Offset change per step of colour cycling. */
    static final double CYCLE_STEP = 0.004;
    /** Largest exported picture: 64 megapixels, 256 MB as RGB. */
    static final long MAX_EXPORT_PIXELS = 64L * 1024 * 1024;

    private final MainView view;
    private final Messages msg;
    private final LocaleSettings locales;
    private final FractalsPreferences preferences;
    private final RenderService renders;
    private final ParameterFiles parameterFiles;
    private final ImageFiles imageFiles;
    private final ExecutorService background;
    private final Executor ui;
    private final Supplier<DesktopTheme> theme;
    private final Supplier<LocalDate> today;

    private FractalSettings settings = FractalSettings.DEFAULT;
    private ColorSettings colors = ColorSettings.DEFAULT;
    private final Deque<FractalSettings> back = new ArrayDeque<>();
    private final Deque<FractalSettings> forward = new ArrayDeque<>();

    private int width;
    private int height;
    private boolean started;
    private FractalData data;
    private FractalData preview;
    private BufferedImage picture;
    private Gradient gradient;
    private Palette gradientPalette;
    private DesktopTheme gradientTheme;
    private RenderService.Task task;
    private RenderService.Task exportTask;
    private boolean cycling;
    private Path parameterFile;

    @Inject
    public MainPresenter(MainView view, Messages msg, LocaleSettings locales, FractalsPreferences preferences,
                         RenderService renders, ParameterFiles parameterFiles, ImageFiles imageFiles,
                         ExecutorService background, @Ui Executor ui) {
        this(view, msg, locales, preferences, renders, parameterFiles, imageFiles, background, ui, AmadeUI::theme,
                LocalDate::now);
    }

    MainPresenter(MainView view, Messages msg, LocaleSettings locales, FractalsPreferences preferences,
                  RenderService renders, ParameterFiles parameterFiles, ImageFiles imageFiles,
                  ExecutorService background, Executor ui, Supplier<DesktopTheme> theme, Supplier<LocalDate> today) {
        this.view = view;
        this.msg = msg;
        this.locales = locales;
        this.preferences = preferences;
        this.renders = renders;
        this.parameterFiles = parameterFiles;
        this.imageFiles = imageFiles;
        this.background = background;
        this.ui = ui;
        this.theme = theme;
        this.today = today;
    }

    /** After the window is shown: renders the Mandelbrot set, or the parameter file named on the command line. */
    public void start(List<String> arguments) {
        started = true;
        Dimension size = view.canvasSize();
        width = Math.max(1, size.width);
        height = Math.max(1, size.height);
        view.showStatus(msg.get("status.ready"));
        Optional<Path> file = arguments.stream().filter(a -> !a.startsWith("--")).findFirst().map(Path::of);
        if (file.isPresent()) {
            open(file.get());
            if (parameterFile != null) return;
        }
        settingsChanged();
    }

    // ---------------------------------------------------------------- state

    public FractalSettings settings() {
        return settings;
    }

    public ColorSettings colors() {
        return colors;
    }

    public Optional<BufferedImage> picture() {
        return Optional.ofNullable(picture);
    }

    public boolean isRendering() {
        return task != null;
    }

    public boolean isExporting() {
        return exportTask != null;
    }

    public boolean isCycling() {
        return cycling;
    }

    public boolean autoIterations() {
        return preferences.autoIterations();
    }

    // ---------------------------------------------------------------- rendering

    /** The picture area changed size; renders again for the new size. */
    public void canvasResized(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1 || (newWidth == width && newHeight == height)) return;
        width = newWidth;
        height = newHeight;
        render();
    }

    /** Renders the current settings again. */
    public void refresh() {
        render();
    }

    /** Stops the calculation and the export. */
    public void stop() {
        boolean stopped = false;
        if (task != null) {
            task.cancel();
            task = null;
            stopped = true;
        }
        if (exportTask != null) {
            exportTask.cancel();
            exportTask = null;
            stopped = true;
        }
        if (stopped) {
            view.hideProgress();
            view.showStatus(msg.get("status.stopped"));
            updateCommands();
        }
    }

    private void render() {
        if (!started) return;
        if (task != null) task.cancel();
        FractalSettings wanted = settings;
        int w = width;
        int h = height;
        data = null;
        if (!isExporting()) view.showProgress(OptionalDouble.of(0));
        view.showStatus(msg.get(wanted.needsPerturbation(h) ? "status.rendering.deep" : "status.rendering"));
        RenderService.Task running = renders.render(wanted, w, h, true, new RenderService.Listener() {
            @Override
            public void progress(double fraction) {
                if (!isExporting()) view.showProgress(OptionalDouble.of(fraction));
            }

            @Override
            public void preview(FractalData coarse) {
                preview = coarse;
                view.showPicture(Colorizer.image(coarse, colors, gradient()));
            }

            @Override
            public void done(FractalData result, Duration time) {
                task = null;
                data = result;
                preview = null;
                picture = Colorizer.image(result, colors, gradient());
                view.showPicture(picture);
                if (!isExporting()) view.hideProgress();
                String seconds = msg.format("time.seconds", time.toMillis() / 1000.0);
                view.showStatus(msg.format(result.perturbation() ? "status.rendered.deep" : "status.rendered",
                        w, h, seconds));
                updateCommands();
            }

            @Override
            public void failed(Throwable error) {
                task = null;
                if (!isExporting()) view.hideProgress();
                view.showStatus(error instanceof OutOfMemoryError ? msg.format("error.memory", w, h)
                        : msg.format("error.render", describe(error)));
                updateCommands();
            }
        });
        task = running;
        updateCommands();
    }

    /** Paints the values already calculated in the current colours. */
    private void recolor() {
        if (data != null) {
            if (picture == null || picture.getWidth() != data.width() || picture.getHeight() != data.height()) {
                picture = Colorizer.image(data, colors, gradient());
            } else {
                Colorizer.colorize(data, colors, gradient(), picture);
            }
            view.showPicture(picture);
        } else if (preview != null) {
            view.showPicture(Colorizer.image(preview, colors, gradient()));
        }
    }

    /** The desktop colour theme changed: the Desktop palette follows it. */
    public void themeChanged() {
        if (colors.palette() == Palette.DESKTOP) recolor();
    }

    private Gradient gradient() {
        DesktopTheme current = theme.get();
        if (gradient == null || gradientPalette != colors.palette()
                || (colors.palette() == Palette.DESKTOP && !current.equals(gradientTheme))) {
            gradient = colors.palette().gradient(current);
            gradientPalette = colors.palette();
            gradientTheme = current;
        }
        return gradient;
    }

    // ---------------------------------------------------------------- navigation

    /** Zooms by {@code factor} keeping the point under the pointer; false if this form cannot zoom further. */
    public boolean zoomAt(double x, double y, double factor) {
        return navigate(settings.region().zoomAt(x, y, width, height, factor));
    }

    /** Centres the point and zooms by {@code factor} (below 1 zooms out). */
    public boolean centerAt(double x, double y, double factor) {
        return navigate(settings.region().centerAt(x, y, width, height, factor));
    }

    /** Fills the window with a frame drawn on the picture. */
    public boolean frame(double x, double y, double frameWidth, double frameHeight) {
        if (frameWidth < 2 || frameHeight < 2) return false;
        return navigate(settings.region().frame(x, y, frameWidth, frameHeight, width, height));
    }

    /** The picture was dragged by this many pixels. */
    public boolean pan(double dx, double dy) {
        if (dx == 0 && dy == 0) return false;
        return navigate(settings.region().pan(dx, dy, height));
    }

    public void zoomIn() {
        navigate(settings.region().zoom(2));
    }

    public void zoomOut() {
        navigate(settings.region().zoom(0.5));
    }

    /** The whole fractal in view again. */
    public void home() {
        apply(settings.withRegion(settings.home()));
    }

    private boolean navigate(Region region) {
        if (region.size() < settings.minSize(height)) {
            view.showStatus(msg.get(settings.formula().supportsDeepZoom(settings.julia())
                    ? "status.limit.deep" : "status.limit"));
            return false;
        }
        FractalSettings next = settings.withRegion(region);
        if (preferences.autoIterations() && region.size() < settings.region().size()) {
            int needed = Iterations.suggested(region.magnification(next.home()));
            if (needed > next.maxIterations()) next = next.withMaxIterations(needed);
        }
        apply(next);
        return true;
    }

    public void goBack() {
        if (back.isEmpty()) return;
        forward.push(settings);
        settings = back.pop();
        settingsChanged();
    }

    public void goForward() {
        if (forward.isEmpty()) return;
        back.push(settings);
        settings = forward.pop();
        settingsChanged();
    }

    /** Goes to a place given in the settings panel; shows an error for text that is not a number. */
    public void goTo(String re, String im, String magnification) {
        Optional<BigDecimal> r = Formats.parseCoordinate(re);
        Optional<BigDecimal> i = Formats.parseCoordinate(im);
        Optional<Double> m = Formats.parseMagnification(magnification);
        if (r.isEmpty() || i.isEmpty() || m.isEmpty()) {
            String bad = r.isEmpty() ? re : i.isEmpty() ? im : magnification;
            view.showError(msg.format("error.number", bad.strip()));
            showSettings();
            return;
        }
        double size = settings.home().size() / m.get();
        if (!(size >= Region.MIN_SIZE && size <= Region.MAX_SIZE)) {
            view.showError(msg.format("error.number", magnification.strip()));
            showSettings();
            return;
        }
        if (!navigate(new Region(r.get(), i.get(), size))) showSettings();
    }

    /** Goes to a place, with the colour density that suits its depth. */
    public void goToPlace(Place place) {
        colors = colors.withDensity(place.density());
        apply(place.settings());
        recolor();
        showSettings();
    }

    // ---------------------------------------------------------------- formula

    public void setFormula(Formula formula) {
        if (formula == settings.formula()) return;
        apply(FractalSettings.of(formula).withPower(settings.power()));
    }

    public void setPower(int power) {
        apply(settings.withPower(power));
    }

    /** Switches between the Mandelbrot and the Julia form, each with the whole fractal in view. */
    public void setJulia(boolean julia) {
        if (julia == settings.julia() || !settings.formula().hasJulia()) return;
        FractalSettings next = settings.withJulia(julia, settings.seedRe(), settings.seedIm());
        apply(next.withRegion(next.home()).withMaxIterations(FractalSettings.DEFAULT_ITERATIONS));
    }

    public void toggleJulia() {
        setJulia(!settings.julia());
    }

    /** The Julia set of the point under the pointer; only from the Mandelbrot form. */
    public boolean pickJuliaSeed(double x, double y) {
        if (!settings.formula().hasJulia() || settings.julia()) return false;
        BigDecimal[] point = settings.region().pointAt(x, y, width, height);
        double re = point[0].doubleValue();
        double im = point[1].doubleValue();
        FractalSettings next = settings.withJulia(true, re, im);
        apply(next.withRegion(next.home()).withMaxIterations(FractalSettings.DEFAULT_ITERATIONS));
        view.showStatus(msg.format("status.julia", Formats.seed(re), Formats.seed(im)));
        return true;
    }

    public void setSeed(String re, String im) {
        Optional<Double> r = Formats.parseNumber(re);
        Optional<Double> i = Formats.parseNumber(im);
        if (r.isEmpty() || i.isEmpty()) {
            view.showError(msg.format("error.number", (r.isEmpty() ? re : im).strip()));
            showSettings();
            return;
        }
        apply(settings.withJulia(settings.julia(), r.get(), i.get()));
    }

    public void setIterations(int iterations) {
        apply(settings.withMaxIterations(iterations));
    }

    public void doubleIterations() {
        setIterations((int) Math.min(FractalSettings.MAX_ITERATIONS, 2L * settings.maxIterations()));
    }

    public void halveIterations() {
        setIterations(settings.maxIterations() / 2);
    }

    public void setBailout(double bailout) {
        apply(settings.withBailout(bailout));
    }

    public void setAutoIterations(boolean auto) {
        remember(() -> preferences.setAutoIterations(auto));
    }

    // ---------------------------------------------------------------- colours

    public void setColoring(Coloring coloring) {
        changeColors(colors.withColoring(coloring));
    }

    public void setPalette(Palette palette) {
        changeColors(colors.withPalette(palette));
    }

    public void setDensity(double density) {
        changeColors(colors.withDensity(density));
    }

    public void setOffset(double offset) {
        changeColors(colors.withOffset(offset));
    }

    private void changeColors(ColorSettings next) {
        if (next.equals(colors)) return;
        colors = next;
        recolor();
        // The menu and the panel both choose colours; each shows the other's choice.
        showSettings();
    }

    public void setCycling(boolean on) {
        if (on == cycling) return;
        cycling = on;
        view.setCycling(on);
        if (!on) showSettings();
        updateCommands();
    }

    public void toggleCycling() {
        setCycling(!cycling);
    }

    /** One step of colour cycling: the palette moves on a little. */
    public void cycleStep() {
        if (!cycling) return;
        colors = colors.withOffset(colors.offset() + CYCLE_STEP);
        recolor();
    }

    // ---------------------------------------------------------------- files

    public void openParameters() {
        view.chooseParameterFile().ifPresent(this::open);
    }

    /** Opens a parameter file; the view it describes is rendered. */
    public void open(Path path) {
        try {
            Parameters parameters = parameterFiles.read(path);
            parameterFile = path;
            colors = parameters.colors();
            FractalSettings before = settings;
            apply(parameters.fractal());
            if (settings == before) settingsChanged();
            recolor();
            showSettings();
            view.showStatus(msg.format("status.opened", path.getFileName()));
        } catch (IOException | UncheckedIOException e) {
            LOG.warn("Cannot open {}", path, e);
            view.showError(msg.format("error.open", path.getFileName(), describe(e)));
        }
    }

    public void saveParameters() {
        String suggested = parameterFile != null ? String.valueOf(parameterFile.getFileName())
                : baseName() + "." + ParameterFiles.EXTENSION;
        Optional<Path> chosen = view.chooseParameterSavePath(suggested);
        if (chosen.isEmpty()) return;
        Path path = chosen.get();
        Parameters parameters = new Parameters(settings, colors);
        background.execute(() -> {
            try {
                parameterFiles.write(path, parameters);
                ui.execute(() -> {
                    parameterFile = path;
                    updateTitle();
                    view.showStatus(msg.format("status.saved", path.getFileName()));
                });
            } catch (IOException | RuntimeException e) {
                LOG.warn("Cannot save {}", path, e);
                ui.execute(() -> view.showError(msg.format("error.save", path.getFileName(), describe(e))));
            }
        });
    }

    /** The parameters as text on the clipboard, e.g. to share a place. */
    public void copyParameters() {
        view.copyToClipboard(ParameterFiles.format(new Parameters(settings, colors)));
        view.showStatus(msg.get("status.copied-parameters"));
    }

    /** Saves the picture as shown. */
    public void saveImage() {
        if (picture == null) return;
        Optional<MainView.SaveTarget> target = view.chooseImageSavePath(baseName() + ".png");
        if (target.isEmpty()) return;
        BufferedImage image = picture;
        writeImage(image, target.get(), () -> view.showStatus(msg.format("status.saved",
                target.get().path().getFileName())));
    }

    /** Renders the current view at a size of its own, e.g. for printing or as wallpaper, and saves it. */
    public void exportImage() {
        if (exportTask != null) return;
        Optional<Dimension> size = view.chooseExportSize(new Dimension(preferences.exportWidth(),
                preferences.exportHeight()));
        if (size.isEmpty()) return;
        int w = size.get().width;
        int h = size.get().height;
        if (w < 1 || h < 1 || (long) w * h > MAX_EXPORT_PIXELS || w > FractalsPreferences.MAX_EXPORT_SIDE
                || h > FractalsPreferences.MAX_EXPORT_SIDE) {
            view.showError(msg.format("error.export-size", w, h));
            return;
        }
        remember(() -> preferences.setExportSize(w, h));
        Optional<MainView.SaveTarget> target = view.chooseImageSavePath(baseName() + "-" + w + "x" + h + ".png");
        if (target.isEmpty()) return;
        // The region keeps its height, so a wider picture shows more at the sides, as a wider window does.
        FractalSettings exported = settings;
        view.showStatus(msg.format("status.exporting", w, h));
        view.showProgress(OptionalDouble.of(0));
        exportTask = renders.export(exported, colors, gradient(), w, h, new RenderService.ExportListener() {
            @Override
            public void progress(double fraction) {
                view.showProgress(OptionalDouble.of(fraction));
            }

            @Override
            public void done(BufferedImage image, Duration time) {
                exportTask = null;
                updateCommands();
                String seconds = msg.format("time.seconds", time.toMillis() / 1000.0);
                writeImage(image, target.get(), () -> view.showStatus(msg.format("status.exported",
                        target.get().path().getFileName(), w, h, seconds)));
            }

            @Override
            public void failed(Throwable error) {
                exportTask = null;
                view.hideProgress();
                updateCommands();
                view.showError(error instanceof OutOfMemoryError ? msg.format("error.memory", w, h)
                        : msg.format("error.render", describe(error)));
            }
        });
        updateCommands();
    }

    private void writeImage(BufferedImage image, MainView.SaveTarget target, Runnable whenSaved) {
        view.showProgress(OptionalDouble.empty());
        background.execute(() -> {
            try {
                imageFiles.write(image, target.path(), target.format());
                ui.execute(() -> {
                    hideProgressIfIdle();
                    whenSaved.run();
                });
            } catch (IOException | RuntimeException e) {
                LOG.warn("Cannot save {}", target.path(), e);
                ui.execute(() -> {
                    hideProgressIfIdle();
                    view.showError(msg.format("error.save", target.path().getFileName(), describe(e)));
                });
            }
        });
    }

    private void hideProgressIfIdle() {
        if (task == null && exportTask == null) view.hideProgress();
    }

    public void copy() {
        if (picture == null) return;
        view.copyToClipboard(picture);
        view.showStatus(msg.get("status.copied"));
    }

    /** "mandelbrot-2026-10-16" */
    private String baseName() {
        String name = settings.formula().name().toLowerCase(Locale.ROOT).replace('_', '-');
        if (settings.julia()) name += "-julia";
        return name + "-" + today.get();
    }

    // ---------------------------------------------------------------- pointer

    /** Shows the coordinates of the point under the mouse pointer. */
    public void pointerAt(double x, double y) {
        Region region = settings.region();
        BigDecimal[] point = region.pointAt(x, y, width, height);
        int decimals = region.decimals() + (int) Math.ceil(Math.log10(Math.max(1, height)));
        view.showPointer(msg.format("status.pointer", Formats.coordinate(point[0], decimals),
                Formats.coordinate(point[1], decimals)));
    }

    public void pointerLeft() {
        view.showPointer(" ");
    }

    // ---------------------------------------------------------------- history and display

    private void apply(FractalSettings next) {
        if (next.equals(settings)) return;
        back.push(settings);
        while (back.size() > HISTORY) back.removeLast();
        forward.clear();
        settings = next;
        settingsChanged();
    }

    private void settingsChanged() {
        showSettings();
        updateTitle();
        view.showZoom(msg.format("status.zoom", magnification()));
        updateCommands();
        render();
    }

    private String magnification() {
        return Formats.magnification(settings.region().magnification(settings.home()), msg.locale());
    }

    private void showSettings() {
        Region region = settings.region();
        int decimals = region.decimals() + 2;
        view.showSettings(settings, colors, new MainView.Location(Formats.coordinate(region.re(), decimals),
                Formats.coordinate(region.im(), decimals), magnification()), preferences.autoIterations());
    }

    /** "Mandelbrot Set - Fractals", "seahorse.fractal - Fractals" */
    private void updateTitle() {
        String name = parameterFile != null ? String.valueOf(parameterFile.getFileName()) : formulaName();
        view.showTitle(msg.format("window.title.document", name));
    }

    String formulaName() {
        String formula = msg.get(formulaKey(settings.formula()));
        return settings.julia() ? msg.format("formula.julia-of", formula) : formula;
    }

    static String formulaKey(Formula formula) {
        return "formula." + formula.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private void updateCommands() {
        view.updateCommands(new MainView.Commands(task != null, picture != null, !back.isEmpty(), !forward.isEmpty(),
                settings.formula().hasJulia(), settings.julia(), cycling, exportTask != null));
    }

    // ---------------------------------------------------------------- window

    public boolean isBarVisible(Bar bar) {
        return preferences.isVisible(bar);
    }

    /** Shows or hides a bar and remembers it; the window changes even if the setting cannot be saved. */
    public void setBarVisible(Bar bar, boolean visible) {
        view.showBar(bar, visible);
        try {
            preferences.setVisible(bar, visible);
        } catch (UncheckedIOException e) {
            LOG.warn("Could not save the preferences", e);
            view.showError(msg.format("error.save-preferences", e.getCause().getMessage()));
        }
    }

    /** Stores a choice; a full disk is only logged, the choice still applies until the program ends. */
    private void remember(Runnable store) {
        try {
            store.run();
        } catch (UncheckedIOException e) {
            LOG.warn("Could not save the preferences", e);
        }
    }

    public void setLanguage(Optional<Language> language) {
        try {
            locales.setApplicationLanguage(language);
        } catch (IOException e) {
            LOG.warn("Could not save the language", e);
            view.showError(msg.format("error.save-language", e.getMessage()));
            return;
        }
        if (locales.restartRequired()) {
            view.showInfo(Messages.commons(msg.locale()).format("language.restart", "amade-fractals"));
        }
    }

    public void about() {
        view.showAbout();
    }

    /** Closes the window; a running export is confirmed first. */
    public void exit() {
        if (exportTask != null && !view.confirm(msg.get("dialog.exit-export.message"),
                msg.get("dialog.exit-export.title"))) {
            return;
        }
        if (task != null) task.cancel();
        if (exportTask != null) exportTask.cancel();
        task = null;
        exportTask = null;
        if (cycling) {
            cycling = false;
            view.setCycling(false);
        }
        view.dispose();
    }

    private static String describe(Throwable e) {
        Throwable cause = e instanceof UncheckedIOException && e.getCause() != null ? e.getCause() : e;
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}

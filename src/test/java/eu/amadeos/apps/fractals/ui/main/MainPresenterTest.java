package eu.amadeos.apps.fractals.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.apps.fractals.QueueExecutor;
import eu.amadeos.apps.fractals.bootstrap.FractalsModule;
import eu.amadeos.apps.fractals.domain.ColorSettings;
import eu.amadeos.apps.fractals.domain.Coloring;
import eu.amadeos.apps.fractals.domain.Formula;
import eu.amadeos.apps.fractals.domain.FractalSettings;
import eu.amadeos.apps.fractals.domain.ImageFormat;
import eu.amadeos.apps.fractals.domain.Palette;
import eu.amadeos.apps.fractals.domain.Place;
import eu.amadeos.apps.fractals.domain.Region;
import eu.amadeos.apps.fractals.repository.FractalsPreferences;
import eu.amadeos.apps.fractals.repository.FractalsPreferences.Bar;
import eu.amadeos.apps.fractals.repository.ImageFiles;
import eu.amadeos.apps.fractals.repository.ParameterFiles;
import eu.amadeos.apps.fractals.service.render.RenderService;
import eu.amadeos.apps.fractals.service.render.Renderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainPresenterTest {
    static final Messages MSG = Messages.load(FractalsModule.class, FractalsModule.MESSAGES, Locale.ENGLISH);

    @TempDir
    Path dir;

    private final QueueExecutor executor = new QueueExecutor();
    private final FakeView view = new FakeView();
    private FractalsPreferences preferences;
    private MainPresenter presenter;

    @BeforeEach
    void setUp() {
        preferences = new FractalsPreferences(dir.resolve("fractals.properties"));
        presenter = new MainPresenter(view, MSG, null, preferences,
                new RenderService(Renderer.direct(), executor, executor), new ParameterFiles(), new ImageFiles(),
                executor, executor, () -> DesktopTheme.WARM_MOTIF, () -> LocalDate.of(2026, 10, 16));
    }

    private void start() {
        presenter.start(List.of());
        executor.runAll();
    }

    @Test
    void startsWithTheMandelbrotSet() {
        start();
        assertEquals(FractalSettings.DEFAULT, view.fractal);
        assertEquals(ColorSettings.DEFAULT, view.colors);
        assertEquals("Mandelbrot Set - Fractals", view.title);
        assertEquals(new MainView.Location("-0.750000", "0.000000", "1"), view.location);
        assertEquals("Zoom 1", view.zoom);
        assertNotNull(view.picture);
        assertEquals(160, view.picture.getWidth());
        assertTrue(view.status.startsWith("160 × 120 pixels in "), view.status);
        assertFalse(view.progressVisible);
        assertTrue(view.commands.hasPicture());
        assertFalse(view.commands.rendering());
        assertFalse(view.commands.canGoBack());
    }

    @Test
    void largeWindowsGetAPreviewFirst() {
        view.canvas = new Dimension(600, 400);
        start();
        assertEquals(150, view.pictures.get(0).getWidth());
        assertEquals(600, view.picture.getWidth());
    }

    @Test
    void zoomingGoesIntoTheHistory() {
        start();
        Region home = presenter.settings().region();
        assertTrue(presenter.zoomAt(40, 30, 4));
        assertTrue(presenter.isRendering());
        executor.runAll();
        assertEquals(home.size() / 4, presenter.settings().region().size(), 1e-12);
        assertEquals("Zoom 4", view.zoom);
        assertTrue(view.commands.canGoBack());
        presenter.goBack();
        assertEquals(home, presenter.settings().region());
        assertTrue(view.commands.canGoForward());
        presenter.goForward();
        assertEquals(home.size() / 4, presenter.settings().region().size(), 1e-12);
        presenter.home();
        assertEquals(home, presenter.settings().region());
        assertFalse(view.commands.canGoForward());
    }

    @Test
    void framesPansAndClicks() {
        start();
        Region home = presenter.settings().region();
        assertTrue(presenter.frame(40, 30, 80, 60));
        assertEquals(home.size() / 2, presenter.settings().region().size(), 1e-12);
        assertFalse(presenter.frame(10, 10, 1, 1));
        assertTrue(presenter.pan(16, 0));
        assertFalse(presenter.pan(0, 0));
        assertTrue(presenter.centerAt(80, 60, 0.5));
        assertEquals(home.size(), presenter.settings().region().size(), 1e-12);
        presenter.zoomIn();
        presenter.zoomOut();
        assertEquals(home.size(), presenter.settings().region().size(), 1e-12);
    }

    @Test
    void deeperZoomsRaiseTheIterations() {
        start();
        presenter.zoomAt(80, 60, 1e6);
        assertTrue(presenter.settings().maxIterations() > FractalSettings.DEFAULT_ITERATIONS);
        presenter.setAutoIterations(false);
        presenter.setIterations(500);
        presenter.zoomAt(80, 60, 1e6);
        assertEquals(500, presenter.settings().maxIterations());
        assertFalse(preferences.autoIterations());
    }

    @Test
    void doublePrecisionFormulasStopAtTheirLimit() {
        start();
        presenter.setFormula(Formula.BURNING_SHIP);
        assertTrue(presenter.zoomAt(80, 60, 1e10));
        executor.runAll();
        Region before = presenter.settings().region();
        assertFalse(presenter.zoomAt(80, 60, 1e6));
        assertEquals(before, presenter.settings().region());
        assertTrue(view.status.startsWith("Deepest zoom reached"), view.status);
    }

    @Test
    void theMandelbrotSetZoomsWithPerturbation() {
        start();
        presenter.goToPlace(Place.DEEP_SEAHORSE);
        executor.runAll();
        assertTrue(view.status.endsWith("with perturbation."), view.status);
        assertTrue(view.zoom.contains("10²⁰"), view.zoom);
        assertEquals(Place.DEEP_SEAHORSE.density(), presenter.colors().density(), 1e-12);
        assertEquals(presenter.colors(), view.colors);
    }

    @Test
    void juliaSetsFromTheMandelbrotSet() {
        start();
        assertTrue(presenter.pickJuliaSeed(80, 60));
        FractalSettings julia = presenter.settings();
        assertTrue(julia.julia());
        assertEquals(-0.75, julia.seedRe(), 0.02);
        assertEquals(Formula.MANDELBROT.home(true), julia.region());
        assertTrue(view.status.startsWith("Julia set for c = (-0.7"), view.status);
        assertEquals("Mandelbrot Set (Julia) - Fractals", view.title);
        // Only from the Mandelbrot form.
        assertFalse(presenter.pickJuliaSeed(10, 10));
        presenter.toggleJulia();
        assertFalse(presenter.settings().julia());
        presenter.setSeed("0,3", "-0.5");
        assertEquals(0.3, presenter.settings().seedRe(), 0);
        presenter.setSeed("x", "1");
        assertEquals(List.of("x is not a number."), view.errors);
        presenter.setFormula(Formula.NEWTON);
        assertFalse(view.commands.juliaAvailable());
        assertFalse(presenter.pickJuliaSeed(10, 10));
    }

    @Test
    void formulaSwitchesOpenTheirWholeFractal() {
        start();
        presenter.setFormula(Formula.MULTIBROT);
        presenter.setPower(5);
        assertEquals(5, presenter.settings().power());
        assertEquals(Formula.MULTIBROT.home(false), presenter.settings().region());
        presenter.setFormula(Formula.PHOENIX);
        assertTrue(presenter.settings().julia());
        executor.runAll();
        assertEquals("Phoenix (Julia) - Fractals", view.title);
    }

    @Test
    void iterationsAndBailout() {
        start();
        presenter.doubleIterations();
        assertEquals(1000, presenter.settings().maxIterations());
        presenter.halveIterations();
        presenter.halveIterations();
        assertEquals(250, presenter.settings().maxIterations());
        presenter.setBailout(4);
        assertEquals(4, presenter.settings().bailout(), 0);
    }

    @Test
    void goToReadsTheLocationFields() {
        start();
        presenter.goTo("-0,7453", "0.1127", "250");
        Region r = presenter.settings().region();
        assertEquals(-0.7453, r.reDouble(), 1e-12);
        assertEquals(2.6 / 250, r.size(), 1e-12);
        presenter.goTo("a", "0", "1");
        assertEquals(List.of("a is not a number."), view.errors);
        assertEquals(r, presenter.settings().region());
        presenter.goTo("0", "0", "1e400");
        assertEquals(2, view.errors.size());
    }

    @Test
    void coloursChangeWithoutCalculating() {
        start();
        BufferedImage picture = view.picture;
        int before = picture.getRGB(5, 5);
        presenter.setPalette(Palette.FIRE);
        assertTrue(executor.isIdle(), "no new calculation");
        assertSame(picture, view.picture);
        assertNotEquals(before, picture.getRGB(5, 5));
        presenter.setColoring(Coloring.ESCAPE_TIME);
        presenter.setDensity(3);
        presenter.setOffset(0.5);
        assertEquals(new ColorSettings(Coloring.ESCAPE_TIME, Palette.FIRE, 3, 0.5), presenter.colors());
        assertFalse(view.commands.canGoBack(), "colours are not in the history");
    }

    @Test
    void colourCyclingMovesThePalette() {
        start();
        presenter.cycleStep();
        assertEquals(0, presenter.colors().offset(), 0);
        presenter.toggleCycling();
        assertTrue(view.cycling);
        assertTrue(view.commands.cycling());
        presenter.cycleStep();
        presenter.cycleStep();
        assertEquals(2 * MainPresenter.CYCLE_STEP, presenter.colors().offset(), 1e-12);
        presenter.setCycling(false);
        assertFalse(view.cycling);
        assertEquals(presenter.colors(), view.colors);
    }

    @Test
    void parametersSaveAndOpen() throws IOException {
        start();
        presenter.goToPlace(Place.TRIPLE_SPIRAL);
        presenter.setPalette(Palette.OCEAN);
        Path file = dir.resolve("spiral.fractal");
        view.parameterSaveAnswers.add(Optional.of(file));
        presenter.saveParameters();
        assertEquals("mandelbrot-2026-10-16.fractal", view.suggestedName);
        executor.runAll();
        assertEquals("Saved spiral.fractal.", view.status);
        assertEquals("spiral.fractal - Fractals", view.title);
        presenter.home();
        presenter.setPalette(Palette.CLASSIC);
        view.openAnswers.add(Optional.of(file));
        presenter.openParameters();
        executor.runAll();
        assertEquals(Place.TRIPLE_SPIRAL.settings(), presenter.settings());
        assertEquals(Palette.OCEAN, presenter.colors().palette());
        assertEquals(Palette.OCEAN, view.colors.palette());
    }

    @Test
    void opensTheFileNamedOnTheCommandLine() throws IOException {
        Path file = dir.resolve("rabbit.fractal");
        new ParameterFiles().write(file, new ParameterFiles.Parameters(Place.DOUADY_RABBIT.settings(),
                ColorSettings.DEFAULT));
        presenter.start(List.of(file.toString()));
        executor.runAll();
        assertEquals(Place.DOUADY_RABBIT.settings(), presenter.settings());
        assertNotNull(view.picture);
        assertEquals("rabbit.fractal - Fractals", view.title);
    }

    @Test
    void reportsFilesThatAreNotParameters() throws IOException {
        Path file = Files.writeString(dir.resolve("x.fractal"), "hello");
        presenter.start(List.of(file.toString()));
        executor.runAll();
        assertTrue(view.errors.get(0).startsWith("Cannot open x.fractal"), view.errors.get(0));
        assertNotNull(view.picture, "the Mandelbrot set instead");
    }

    @Test
    void savesThePictureAsShown() throws IOException {
        start();
        Path file = dir.resolve("m.png");
        view.imageSaveAnswers.add(Optional.of(new MainView.SaveTarget(file, ImageFormat.PNG)));
        presenter.saveImage();
        assertEquals("mandelbrot-2026-10-16.png", view.suggestedName);
        executor.runAll();
        assertEquals(160, ImageIO.read(file.toFile()).getWidth());
        assertEquals("Saved m.png.", view.status);
    }

    @Test
    void exportsAtAnotherSize() throws IOException {
        start();
        Path file = dir.resolve("big.png");
        view.exportSizeAnswers.add(Optional.of(new Dimension(300, 200)));
        view.imageSaveAnswers.add(Optional.of(new MainView.SaveTarget(file, ImageFormat.PNG)));
        presenter.exportImage();
        assertEquals("mandelbrot-2026-10-16-300x200.png", view.suggestedName);
        assertTrue(presenter.isExporting());
        assertTrue(view.commands.exporting());
        executor.runAll();
        BufferedImage exported = ImageIO.read(file.toFile());
        assertEquals(300, exported.getWidth());
        assertEquals(200, exported.getHeight());
        assertTrue(view.status.startsWith("Exported big.png (300 × 200 pixels) in "), view.status);
        assertFalse(presenter.isExporting());
        assertEquals(300, preferences.exportWidth());
    }

    @Test
    void refusesHugeExports() {
        start();
        view.exportSizeAnswers.add(Optional.of(new Dimension(20000, 20000)));
        presenter.exportImage();
        assertTrue(view.errors.get(0).contains("too large"), view.errors.get(0));
        assertFalse(presenter.isExporting());
    }

    @Test
    void stopCancelsTheCalculation() {
        start();
        presenter.zoomIn();
        assertTrue(view.commands.rendering());
        presenter.stop();
        assertFalse(presenter.isRendering());
        executor.runAll();
        assertEquals("Stopped.", view.status);
        assertFalse(view.progressVisible);
    }

    @Test
    void newerViewsReplaceOlderCalculations() {
        start();
        view.pictures.clear();
        presenter.zoomIn();
        presenter.zoomIn();
        executor.runAll();
        assertEquals(1, view.pictures.size(), "only the last view is shown");
    }

    @Test
    void resizingRendersForTheNewSize() {
        start();
        presenter.canvasResized(200, 100);
        executor.runAll();
        assertEquals(200, view.picture.getWidth());
        presenter.canvasResized(200, 100);
        assertTrue(executor.isIdle());
    }

    @Test
    void pointerShowsCoordinates() {
        start();
        presenter.pointerAt(80, 60);
        assertTrue(view.pointer.startsWith("Re -0.75"), view.pointer);
        presenter.pointerLeft();
        assertEquals(" ", view.pointer);
    }

    @Test
    void copiesPictureAndParameters() throws IOException {
        start();
        presenter.copy();
        assertSame(view.picture, view.clipboardImage);
        presenter.copyParameters();
        assertEquals(FractalSettings.DEFAULT, ParameterFiles.parse(view.clipboardText).fractal());
        assertEquals("Parameters copied.", view.status);
    }

    @Test
    void barsAreRemembered() {
        start();
        presenter.setBarVisible(Bar.TOOLBAR, false);
        assertEquals(false, view.bars.get(Bar.TOOLBAR));
        assertFalse(presenter.isBarVisible(Bar.TOOLBAR));
    }

    @Test
    void fullScreenAndEscape() {
        start();
        assertFalse(view.commands.fullScreen());
        presenter.toggleFullScreen();
        assertTrue(view.fullScreen);
        assertTrue(presenter.isFullScreen());
        assertTrue(view.commands.fullScreen());
        // Escape leaves full screen first and keeps the calculation going.
        presenter.zoomIn();
        presenter.escape();
        assertFalse(view.fullScreen);
        assertFalse(view.commands.fullScreen());
        assertTrue(presenter.isRendering());
        // Then it stops.
        presenter.escape();
        assertFalse(presenter.isRendering());
        executor.runAll();
        assertEquals("Stopped.", view.status);
        // Full screen is not a bar setting: the bars keep their remembered state.
        presenter.setFullScreen(true);
        assertTrue(presenter.isBarVisible(Bar.TOOLBAR));
        presenter.setFullScreen(false);
        assertTrue(view.bars.isEmpty());
    }

    @Test
    void exitAsksWhileExporting() {
        start();
        view.exportSizeAnswers.add(Optional.of(new Dimension(64, 64)));
        view.imageSaveAnswers.add(Optional.of(new MainView.SaveTarget(dir.resolve("e.png"), ImageFormat.PNG)));
        presenter.exportImage();
        view.confirmations.add(false);
        presenter.exit();
        assertFalse(view.disposed);
        view.confirmations.add(true);
        presenter.exit();
        assertTrue(view.disposed);
        executor.runAll();
        assertFalse(Files.exists(dir.resolve("e.png")));
    }

    @Test
    void desktopPaletteFollowsTheTheme() {
        DesktopTheme[] theme = {DesktopTheme.WARM_MOTIF};
        presenter = new MainPresenter(view, MSG, null, preferences,
                new RenderService(Renderer.direct(), executor, executor), new ParameterFiles(), new ImageFiles(),
                executor, executor, () -> theme[0], LocalDate::now);
        start();
        presenter.setPalette(Palette.DESKTOP);
        int warm = view.picture.getRGB(0, 0);
        theme[0] = DesktopTheme.DARK_SLATE;
        presenter.themeChanged();
        assertNotEquals(warm, view.picture.getRGB(0, 0));
    }
}

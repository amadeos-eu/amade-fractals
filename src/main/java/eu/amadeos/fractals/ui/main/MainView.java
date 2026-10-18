package eu.amadeos.fractals.ui.main;

import eu.amadeos.fractals.domain.ColorSettings;
import eu.amadeos.fractals.domain.FractalSettings;
import eu.amadeos.fractals.domain.ImageFormat;
import eu.amadeos.fractals.repository.FractalsPreferences.Bar;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalDouble;

/** What {@link MainPresenter} needs from the window; tests replace it. */
public interface MainView {
    /** Which commands can run now; menus and toolbar follow. */
    record Commands(boolean rendering, boolean hasPicture, boolean canGoBack, boolean canGoForward,
                    boolean juliaAvailable, boolean julia, boolean cycling, boolean exporting,
                    boolean fullScreen) {
        public static final Commands NONE =
                new Commands(false, false, false, false, false, false, false, false, false);
    }

    /** The centre and magnification as the settings panel shows them. */
    record Location(String re, String im, String magnification) {
    }

    /** Where and in which format to save a picture. */
    record SaveTarget(Path path, ImageFormat format) {
    }

    void showTitle(String title);

    /** Fills the settings panel. */
    void showSettings(FractalSettings fractal, ColorSettings colors, Location location, boolean autoIterations);

    /** Shows a picture: the finished one, or a coarse preview to be scaled to the window. */
    void showPicture(BufferedImage picture);

    void showStatus(String text);

    /** The coordinates under the mouse pointer, or blank. */
    void showPointer(String text);

    /** The magnification in the status bar. */
    void showZoom(String text);

    /** Shows the progress bar: a fraction from 0 to 1, or empty while the progress is not known. */
    void showProgress(OptionalDouble fraction);

    void hideProgress();

    void updateCommands(Commands commands);

    /** The size of the picture area in pixels. */
    Dimension canvasSize();

    /** Asks for a parameter file to open; empty if cancelled. */
    Optional<Path> chooseParameterFile();

    /** Asks where to save the parameters; replacing a file is confirmed. Empty if cancelled. */
    Optional<Path> chooseParameterSavePath(String suggestedName);

    /** Asks where to save a picture, preselecting PNG; replacing a file is confirmed. Empty if cancelled. */
    Optional<SaveTarget> chooseImageSavePath(String suggestedName);

    /** Asks for the size of an exported picture; empty if cancelled. */
    Optional<Dimension> chooseExportSize(Dimension suggested);

    void copyToClipboard(BufferedImage picture);

    void copyToClipboard(String text);

    /** Starts or stops the timer that calls {@link MainPresenter#cycleStep()}. */
    void setCycling(boolean cycling);

    /** Shows or hides the toolbar, the settings or the status bar. */
    void showBar(Bar bar, boolean visible);

    /**
     * Full screen: only the picture, over the whole screen, without menu, bars and window frame; or back to the
     * window as it was.
     */
    void setFullScreen(boolean fullScreen);

    /** A yes/no question; true for yes. */
    boolean confirm(String message, String title);

    void showError(String message);

    void showInfo(String message);

    void showAbout();

    void dispose();
}

package org.datazuul.amadeos.fractals.ui.main;

import org.datazuul.amadeos.commons.theme.DesktopTheme;
import org.datazuul.amadeos.fractals.OnEdt;
import org.datazuul.amadeos.fractals.QueueExecutor;
import org.datazuul.amadeos.fractals.domain.Formula;
import org.datazuul.amadeos.fractals.domain.Palette;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences.Bar;
import org.datazuul.amadeos.fractals.repository.ImageFiles;
import org.datazuul.amadeos.fractals.repository.ParameterFiles;
import org.datazuul.amadeos.fractals.service.render.RenderService;
import org.datazuul.amadeos.fractals.service.render.Renderer;
import org.datazuul.amadeos.fractals.ui.icons.ToolIcon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The real window with the real renderer: settings, actions, gestures. Needs a display (Xvfb in CI). */
class MainWindowTest {
    @TempDir
    Path dir;

    @Test
    void exploreThroughTheWindow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        OnEdt.run(() -> {
            QueueExecutor executor = new QueueExecutor();
            FractalsPreferences preferences = new FractalsPreferences(dir.resolve("fractals.properties"));
            MainWindow window = new MainWindow(MainPresenterTest.MSG, null);
            MainPresenter presenter = new MainPresenter(window, MainPresenterTest.MSG, null, preferences,
                    new RenderService(Renderer.direct(), executor, executor), new ParameterFiles(), new ImageFiles(),
                    executor, executor, () -> DesktopTheme.WARM_MOTIF, LocalDate::now);
            window.bind(presenter);
            window.canvas().setSize(200, 150);
            try {
                FractalActions actions = window.actions();
                assertFalse(actions.saveImage.isEnabled());
                presenter.start(List.of());
                executor.runAll();
                assertTrue(actions.saveImage.isEnabled());
                assertFalse(actions.back.isEnabled());
                assertFalse(actions.stop.isEnabled());
                assertNotNull(window.canvas().picture());
                assertEquals("Zoom 1", window.zoomText());
                assertTrue(window.statusText().startsWith("200 × 150 pixels"), window.statusText());

                // A click zooms in at the point.
                click(window, MouseEvent.BUTTON1, 100, 75, 0);
                assertTrue(actions.stop.isEnabled());
                executor.runAll();
                assertEquals("Zoom 2", window.zoomText());
                assertTrue(actions.back.isEnabled());
                actions.back.actionPerformed(null);
                executor.runAll();
                assertEquals("Zoom 1", window.zoomText());

                // The settings panel drives the presenter and shows its state.
                SettingsPanel panel = window.settingsPanel();
                panel.formulaBox().setSelectedItem(Formula.TRICORN);
                executor.runAll();
                assertEquals(Formula.TRICORN, presenter.settings().formula());
                panel.juliaBox().doClick();
                executor.runAll();
                assertTrue(presenter.settings().julia());
                assertTrue((Boolean) actions.julia.getValue(Action.SELECTED_KEY));
                panel.iterationsSpinner().setValue(800);
                assertEquals(800, presenter.settings().maxIterations());
                panel.magnificationField().setText("10");
                panel.magnificationField().postActionEvent();
                executor.runAll();
                assertEquals("Zoom 10", window.zoomText());
                BufferedImage before = window.canvas().picture();
                int pixel = before.getRGB(10, 10);
                panel.paletteBox().setSelectedItem(Palette.FIRE);
                assertNotEquals(pixel, window.canvas().picture().getRGB(10, 10));

                // Ctrl+click on the Mandelbrot form opens its Julia set.
                actions.julia.actionPerformed(null);
                executor.runAll();
                assertFalse(presenter.settings().julia());
                click(window, MouseEvent.BUTTON1, 60, 75, MouseEvent.CTRL_DOWN_MASK);
                executor.runAll();
                assertTrue(presenter.settings().julia());

                actions.cycle.actionPerformed(null);
                assertTrue(window.isCycleTimerRunning());
                actions.cycle.actionPerformed(null);
                assertFalse(window.isCycleTimerRunning());

                presenter.setBarVisible(Bar.SETTINGS, false);
                presenter.setBarVisible(Bar.SETTINGS, true);

                // Full screen: only the picture, without menu, bars and frame; F11 or Escape go back.
                window.setVisible(true);
                presenter.setBarVisible(Bar.STATUS_BAR, false);
                actions.fullScreen.actionPerformed(null);
                assertTrue(window.isFullScreen());
                assertTrue(window.isUndecorated());
                assertNull(window.getJMenuBar());
                assertFalse(window.settingsPanel().isShowing());
                assertTrue((Boolean) actions.fullScreen.getValue(Action.SELECTED_KEY));
                assertEquals(MainPresenterTest.MSG.get("status.full-screen"), window.canvas().noteText());
                presenter.zoomIn();
                assertFalse(Double.isNaN(window.canvas().progressValue()));
                executor.runAll();
                assertTrue(Double.isNaN(window.canvas().progressValue()));
                pressKey(window, KeyEvent.VK_ESCAPE);
                assertFalse(window.isFullScreen());
                assertFalse(window.isUndecorated());
                assertNotNull(window.getJMenuBar());
                assertTrue(window.settingsPanel().isShowing());
                assertFalse(window.progressBar().getParent().getParent().isVisible(), "status bar stays hidden");
                assertEquals("", window.canvas().noteText());
                pressKey(window, KeyEvent.VK_F11);
                assertTrue(window.isFullScreen());
                pressKey(window, KeyEvent.VK_F11);
                assertFalse(window.isFullScreen());
                presenter.setBarVisible(Bar.STATUS_BAR, true);
            } finally {
                window.dispose();
            }
        });
    }

    private static void pressKey(MainWindow window, int key) {
        javax.swing.JRootPane root = window.getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(key, 0);
        Object name = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke);
        assertNotNull(name, "no binding for " + stroke);
        root.getActionMap().get(name).actionPerformed(null);
    }

    private static void click(MainWindow window, int button, int x, int y, int modifiers) {
        long now = System.currentTimeMillis();
        int mask = button == MouseEvent.BUTTON1 ? MouseEvent.BUTTON1_DOWN_MASK : MouseEvent.BUTTON3_DOWN_MASK;
        window.canvas().dispatchEvent(new MouseEvent(window.canvas(), MouseEvent.MOUSE_PRESSED, now,
                modifiers | mask, x, y, 1, false, button));
        window.canvas().dispatchEvent(new MouseEvent(window.canvas(), MouseEvent.MOUSE_RELEASED, now,
                modifiers, x, y, 1, false, button));
    }

    @Test
    void everyGlyphPaints() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        OnEdt.run(() -> {
            for (ToolIcon.Glyph glyph : ToolIcon.Glyph.values()) {
                ToolIcon icon = new ToolIcon(glyph, 32);
                BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                icon.paintIcon(new JButton(), image.getGraphics(), 0, 0);
                int painted = 0;
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 32; x++) if ((image.getRGB(x, y) >>> 24) != 0) painted++;
                }
                assertTrue(painted > 40, glyph + " is nearly empty");
            }
        });
    }
}

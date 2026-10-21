package eu.amadeos.apps.fractals.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.StandardGlyph;
import eu.amadeos.commons.swing.ToolIcon;
import eu.amadeos.commons.swing.ToolIcon.Glyph;
import eu.amadeos.apps.fractals.ui.icons.FractalGlyph;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * One action per command, shared by the menus and the toolbar, so that both are enabled and disabled
 * together. Texts come from the messages ({@code &} marks the mnemonic), icons from {@link ToolIcon}.
 */
public final class FractalActions {
    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
    private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
    private static final int ALT = InputEvent.ALT_DOWN_MASK;

    public final Action open;
    public final Action saveParameters;
    public final Action saveImage;
    public final Action exportImage;
    public final Action exit;
    public final Action back;
    public final Action forward;
    public final Action copy;
    public final Action copyParameters;
    public final Action home;
    public final Action zoomIn;
    public final Action zoomOut;
    public final Action julia;
    public final Action moreIterations;
    public final Action fewerIterations;
    public final Action cycle;
    public final Action refresh;
    public final Action stop;
    public final Action fullScreen;
    /** Escape, without a menu entry: leaves full screen, otherwise stops. */
    public final Action escape;

    private final Messages msg;

    public FractalActions(MainPresenter presenter, Messages msg) {
        this.msg = msg;
        open = action("menu.file.open", StandardGlyph.OPEN, KeyEvent.VK_O, CTRL, presenter::openParameters);
        saveParameters = action("menu.file.save", StandardGlyph.SAVE, KeyEvent.VK_S, CTRL, presenter::saveParameters);
        saveImage = action("menu.file.save-image", FractalGlyph.PICTURE, KeyEvent.VK_S, CTRL | SHIFT, presenter::saveImage);
        exportImage = action("menu.file.export", FractalGlyph.EXPORT, KeyEvent.VK_E, CTRL, presenter::exportImage);
        exit = action("menu.file.exit", null, KeyEvent.VK_Q, CTRL, presenter::exit);
        back = action("menu.edit.back", FractalGlyph.BACK, KeyEvent.VK_Z, CTRL, presenter::goBack);
        forward = action("menu.edit.forward", FractalGlyph.FORWARD, KeyEvent.VK_Y, CTRL, presenter::goForward);
        copy = action("menu.edit.copy", StandardGlyph.COPY, KeyEvent.VK_C, CTRL, presenter::copy);
        copyParameters = action("menu.edit.copy-parameters", null, KeyEvent.VK_C, CTRL | SHIFT,
                presenter::copyParameters);
        home = action("menu.view.home", FractalGlyph.HOME, KeyEvent.VK_HOME, 0, presenter::home);
        zoomIn = action("menu.view.zoom-in", StandardGlyph.ZOOM_IN, KeyEvent.VK_PLUS, CTRL, presenter::zoomIn);
        zoomOut = action("menu.view.zoom-out", StandardGlyph.ZOOM_OUT, KeyEvent.VK_MINUS, CTRL, presenter::zoomOut);
        julia = action("menu.fractal.julia", FractalGlyph.JULIA, KeyEvent.VK_J, CTRL, presenter::toggleJulia);
        moreIterations = action("menu.fractal.more-iterations", null, KeyEvent.VK_I, CTRL,
                presenter::doubleIterations);
        fewerIterations = action("menu.fractal.fewer-iterations", null, KeyEvent.VK_I, CTRL | SHIFT,
                presenter::halveIterations);
        cycle = action("menu.colors.cycle", FractalGlyph.CYCLE, KeyEvent.VK_K, CTRL, presenter::toggleCycling);
        refresh = action("menu.view.refresh", StandardGlyph.REFRESH, KeyEvent.VK_F5, 0, presenter::refresh);
        stop = action("menu.view.stop", FractalGlyph.STOP, KeyEvent.VK_ESCAPE, 0, presenter::stop);
        fullScreen = action("menu.view.full-screen", FractalGlyph.FULL_SCREEN, KeyEvent.VK_F11, 0,
                presenter::toggleFullScreen);
        escape = new AbstractAction("escape") {
            @Override
            public void actionPerformed(ActionEvent e) {
                presenter.escape();
            }
        };
        escape.putValue(Action.ACTION_COMMAND_KEY, "escape");
        back.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ALT));
        forward.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ALT));
        update(MainView.Commands.NONE);
    }

    /** All actions, for tests and key bindings. */
    public List<Action> all() {
        return List.of(open, saveParameters, saveImage, exportImage, exit, back, forward, copy, copyParameters, home,
                zoomIn, zoomOut, julia, moreIterations, fewerIterations, cycle, refresh, stop, fullScreen);
    }

    /**
     * Further keys that run an action, besides its accelerator: Ctrl+Z and Ctrl+Y for back and forward, the
     * number pad for zooming, and Escape, which leaves full screen before it stops anything.
     */
    public List<KeyBinding> extraKeys() {
        return List.of(new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), escape),
                new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Z, CTRL), back),
                new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Y, CTRL), forward),
                new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, CTRL), zoomIn),
                new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, CTRL), zoomIn),
                new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, CTRL), zoomOut));
    }

    public record KeyBinding(KeyStroke key, Action action) {
    }

    public void update(MainView.Commands c) {
        saveImage.setEnabled(c.hasPicture());
        exportImage.setEnabled(!c.exporting());
        copy.setEnabled(c.hasPicture());
        back.setEnabled(c.canGoBack());
        forward.setEnabled(c.canGoForward());
        julia.setEnabled(c.juliaAvailable());
        julia.putValue(Action.SELECTED_KEY, c.julia());
        cycle.setEnabled(c.hasPicture() || c.cycling());
        cycle.putValue(Action.SELECTED_KEY, c.cycling());
        stop.setEnabled(c.rendering() || c.exporting());
        fullScreen.putValue(Action.SELECTED_KEY, c.fullScreen());
    }

    private Action action(String key, Glyph glyph, int keyCode, int modifiers, Runnable command) {
        Mnemonics.Text text = Mnemonics.parse(msg.get(key));
        Action action = new AbstractAction(text.text()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isEnabled()) command.run();
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, key);
        if (text.index() >= 0) {
            action.putValue(Action.MNEMONIC_KEY, text.keyCode());
            action.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, text.index());
        }
        String tip = msg.has(key + ".tip") ? msg.get(key + ".tip") : text.text();
        action.putValue(Action.SHORT_DESCRIPTION, tip);
        if (keyCode != 0) action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyCode, modifiers));
        if (glyph != null) {
            action.putValue(Action.SMALL_ICON, new ToolIcon(glyph, 16));
            action.putValue(Action.LARGE_ICON_KEY, new ToolIcon(glyph));
        }
        return action;
    }
}

package eu.amadeos.apps.fractals.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.apps.fractals.bootstrap.AmadeFractalsApplication;
import eu.amadeos.apps.fractals.domain.ColorSettings;
import eu.amadeos.apps.fractals.domain.FractalSettings;
import eu.amadeos.apps.fractals.domain.ImageFormat;
import eu.amadeos.apps.fractals.repository.FractalsPreferences;
import eu.amadeos.apps.fractals.repository.FractalsPreferences.Bar;
import eu.amadeos.apps.fractals.repository.ParameterFiles;
import eu.amadeos.apps.fractals.ui.view.FractalCanvas;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The main window: menu bar and toolbar, the settings on the left, the fractal in the middle, and a status
 * bar with messages, the coordinates under the pointer, the magnification and the progress.
 */
public final class MainWindow extends JFrame implements MainView {
    /** Rendering for a new window size waits until the resizing pauses this long. */
    static final int RESIZE_DELAY = 200;
    /** Colour cycling: 25 steps a second. */
    static final int CYCLE_DELAY = 40;

    private final Messages msg;
    private final LocaleSettings locales;
    private final FractalCanvas canvas = new FractalCanvas();
    private final JLabel status = new JLabel(" ");
    private final JLabel pointer = new JLabel(" ");
    private final JLabel zoom = new JLabel(" ");
    private final JProgressBar progress = new JProgressBar(0, 1000);
    private final JPanel statusBar;
    private final Timer resizeTimer;
    private final Timer cycleTimer;
    /** How long the hint about leaving full screen stays. */
    static final int NOTE_TIME = 3000;
    private final JPanel desk = new JPanel(new BorderLayout());
    private final Border deskBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
    private boolean fullScreen;
    private Rectangle normalBounds;
    private int normalState;
    private JScrollPane settingsPane;
    private SettingsPanel settingsPanel;
    private MainMenuBar menuBar;
    private MainToolBar toolBar;
    private FractalActions actions;
    private MainPresenter presenter;
    private File lastDirectory;

    @Inject
    public MainWindow(Messages msg, LocaleSettings locales) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.locales = locales;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        canvas.setName("canvas");
        canvas.setPlaceholder(msg.get("placeholder"));
        desk.setBorder(deskBorder);
        desk.add(canvas, BorderLayout.CENTER);
        add(desk, BorderLayout.CENTER);
        statusBar = statusBar();
        add(statusBar, BorderLayout.SOUTH);
        java.net.URL icon = getClass().getResource("/icons/amade-fractals.png");
        if (icon != null) setIconImage(new javax.swing.ImageIcon(icon).getImage());
        resizeTimer = new Timer(RESIZE_DELAY, e -> {
            if (presenter != null) presenter.canvasResized(canvas.getWidth(), canvas.getHeight());
        });
        resizeTimer.setRepeats(false);
        cycleTimer = new Timer(CYCLE_DELAY, e -> {
            if (presenter != null) presenter.cycleStep();
        });
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });
        setPreferredSize(new Dimension(1180, 780));
        pack();
        setLocationRelativeTo(null);
    }

    public void bind(MainPresenter newPresenter) {
        EdtExecutor.requireEdt();
        presenter = newPresenter;
        actions = new FractalActions(presenter, msg);
        menuBar = new MainMenuBar(presenter, actions, msg, locales);
        setJMenuBar(menuBar);
        toolBar = new MainToolBar(actions);
        add(toolBar, BorderLayout.NORTH);
        settingsPanel = new SettingsPanel(presenter, actions, msg);
        settingsPane = new JScrollPane(settingsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        settingsPane.setBorder(BorderFactory.createEmptyBorder());
        settingsPane.getVerticalScrollBar().setUnitIncrement(16);
        add(settingsPane, BorderLayout.WEST);
        canvas.setListener(new FractalCanvas.Listener() {
            @Override
            public boolean centerAt(double x, double y, double factor) {
                return presenter.centerAt(x, y, factor);
            }

            @Override
            public boolean zoomAt(double x, double y, double factor) {
                return presenter.zoomAt(x, y, factor);
            }

            @Override
            public boolean frame(double x, double y, double width, double height) {
                return presenter.frame(x, y, width, height);
            }

            @Override
            public boolean pan(double dx, double dy) {
                return presenter.pan(dx, dy);
            }

            @Override
            public boolean pickJuliaSeed(double x, double y) {
                return presenter.pickJuliaSeed(x, y);
            }

            @Override
            public void pointerAt(double x, double y) {
                presenter.pointerAt(x, y);
            }

            @Override
            public void pointerLeft() {
                presenter.pointerLeft();
            }
        });
        // The Desktop palette follows the colour theme.
        AmadeUI.style(canvas, (c, theme) -> {
            if (presenter != null) presenter.themeChanged();
        });
        // Accelerators also when the focus is not in a menu, e.g. Escape on the picture.
        for (Action a : actions.all()) {
            KeyStroke key = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
            if (key != null) bindKey(key, a);
        }
        for (FractalActions.KeyBinding binding : actions.extraKeys()) bindKey(binding.key(), binding.action());
        for (Bar bar : Bar.values()) showBar(bar, presenter.isBarVisible(bar));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                presenter.exit();
            }
        });
        updateCommands(Commands.NONE);
        pack();
        setLocationRelativeTo(null);
    }

    private void bindKey(KeyStroke key, Action action) {
        String name = String.valueOf(action.getValue(Action.ACTION_COMMAND_KEY)) + "@" + key;
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        getRootPane().getActionMap().put(name, action);
    }

    public FractalCanvas canvas() {
        return canvas;
    }

    public FractalActions actions() {
        return actions;
    }

    SettingsPanel settingsPanel() {
        return settingsPanel;
    }

    String statusText() {
        return status.getText();
    }

    String zoomText() {
        return zoom.getText();
    }

    JProgressBar progressBar() {
        return progress;
    }

    boolean isCycleTimerRunning() {
        return cycleTimer.isRunning();
    }

    // ---------------------------------------------------------------- MainView

    @Override
    public void showTitle(String title) {
        EdtExecutor.requireEdt();
        setTitle(title);
    }

    @Override
    public void showSettings(FractalSettings fractal, ColorSettings colors, Location location, boolean autoIterations) {
        EdtExecutor.requireEdt();
        settingsPanel.show(fractal, colors, location, autoIterations);
        menuBar.show(fractal, colors, autoIterations);
    }

    @Override
    public void showPicture(BufferedImage picture) {
        EdtExecutor.requireEdt();
        canvas.setPicture(picture);
    }

    @Override
    public void showStatus(String text) {
        EdtExecutor.requireEdt();
        status.setText(text);
    }

    @Override
    public void showPointer(String text) {
        EdtExecutor.requireEdt();
        pointer.setText(text == null || text.isEmpty() ? " " : text);
    }

    @Override
    public void showZoom(String text) {
        EdtExecutor.requireEdt();
        zoom.setText(text);
    }

    @Override
    public void showProgress(OptionalDouble fraction) {
        EdtExecutor.requireEdt();
        progress.setVisible(true);
        progress.setIndeterminate(fraction.isEmpty());
        if (fraction.isPresent()) {
            progress.setValue((int) Math.round(fraction.getAsDouble() * 1000));
            progress.setString(msg.format("status.percent", Math.round(fraction.getAsDouble() * 100)));
        } else {
            progress.setString("");
        }
        if (fullScreen) canvas.setProgress(fraction.orElse(0));
    }

    @Override
    public void hideProgress() {
        EdtExecutor.requireEdt();
        progress.setIndeterminate(false);
        progress.setVisible(false);
        canvas.setProgress(Double.NaN);
    }

    @Override
    public void updateCommands(Commands commands) {
        EdtExecutor.requireEdt();
        if (actions != null) actions.update(commands);
    }

    @Override
    public Dimension canvasSize() {
        EdtExecutor.requireEdt();
        if (canvas.getWidth() > 0 && canvas.getHeight() > 0) return canvas.getSize();
        return canvas.getPreferredSize();
    }

    @Override
    public Optional<Path> chooseParameterFile() {
        EdtExecutor.requireEdt();
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = parameterFilter();
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return Optional.empty();
        lastDirectory = chooser.getCurrentDirectory();
        return Optional.of(chooser.getSelectedFile().toPath());
    }

    @Override
    public Optional<Path> chooseParameterSavePath(String suggestedName) {
        EdtExecutor.requireEdt();
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = parameterFilter();
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), suggestedName));
        while (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            if (!path.getFileName().toString().contains(".")) {
                path = path.resolveSibling(path.getFileName() + "." + ParameterFiles.EXTENSION);
            }
            lastDirectory = chooser.getCurrentDirectory();
            if (!Files.exists(path) || confirmOverwrite(path)) return Optional.of(path);
        }
        return Optional.empty();
    }

    private FileNameExtensionFilter parameterFilter() {
        return new FileNameExtensionFilter(msg.format("filter.parameters", "*." + ParameterFiles.EXTENSION),
                ParameterFiles.EXTENSION);
    }

    @Override
    public Optional<SaveTarget> chooseImageSavePath(String suggestedName) {
        EdtExecutor.requireEdt();
        JFileChooser chooser = new JFileChooser(lastDirectory != null ? lastDirectory : picturesDirectory());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        Map<FileFilter, ImageFormat> byFilter = new LinkedHashMap<>();
        for (ImageFormat format : ImageFormat.values()) {
            String patterns = String.join(", ", format.extensions().stream().map(e -> "*." + e).toList());
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    msg.format("filter.format", msg.get("format." + format.defaultExtension()), patterns),
                    format.extensions().toArray(String[]::new));
            byFilter.put(filter, format);
            chooser.addChoosableFileFilter(filter);
            if (format == ImageFormat.PNG) chooser.setFileFilter(filter);
        }
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), suggestedName));
        // Switching the type changes the extension of the name.
        chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, e -> {
            ImageFormat format = byFilter.get(chooser.getFileFilter());
            File current = chooser.getSelectedFile();
            if (format != null && current != null && !current.getName().isBlank()) {
                chooser.setSelectedFile(format.withExtension(new File(chooser.getCurrentDirectory(),
                        current.getName()).toPath()).toFile());
            }
        });
        while (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            ImageFormat format = byFilter.getOrDefault(chooser.getFileFilter(), ImageFormat.PNG);
            Path path = chooser.getSelectedFile().toPath();
            Optional<ImageFormat> typed = ImageFormat.forPath(path);
            if (typed.isPresent()) format = typed.get();
            else path = format.withExtension(path);
            lastDirectory = chooser.getCurrentDirectory();
            if (!Files.exists(path) || confirmOverwrite(path)) return Optional.of(new SaveTarget(path, format));
        }
        return Optional.empty();
    }

    private boolean confirmOverwrite(Path path) {
        return confirm(msg.format("dialog.overwrite.message", path.getFileName()), msg.get("dialog.overwrite.title"));
    }

    /** ~/Pictures (or its translated name from the XDG user directories), else the home folder. */
    private static File picturesDirectory() {
        File home = new File(System.getProperty("user.home"));
        try {
            Path dirs = home.toPath().resolve(".config/user-dirs.dirs");
            if (Files.isRegularFile(dirs)) {
                for (String line : Files.readAllLines(dirs)) {
                    if (line.startsWith("XDG_PICTURES_DIR=")) {
                        String value = line.substring(line.indexOf('=') + 1).replace("\"", "")
                                .replace("$HOME", home.getPath());
                        File dir = new File(value);
                        if (dir.isDirectory()) return dir;
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            // fall through
        }
        File pictures = new File(home, "Pictures");
        return pictures.isDirectory() ? pictures : home;
    }

    /** Sizes offered for export; the first is the window's own. */
    record ExportPreset(String label, int width, int height) {
        @Override
        public String toString() {
            return label;
        }
    }

    List<ExportPreset> exportPresets() {
        Dimension window = canvasSize();
        return List.of(
                new ExportPreset(msg.format("export.preset.window", window.width, window.height), window.width,
                        window.height),
                new ExportPreset("1920 × 1080 (Full HD)", 1920, 1080),
                new ExportPreset("2560 × 1440 (QHD)", 2560, 1440),
                new ExportPreset("3840 × 2160 (4K UHD)", 3840, 2160),
                new ExportPreset("7680 × 4320 (8K UHD)", 7680, 4320),
                new ExportPreset(msg.format("export.preset.a4", 3508, 2480), 3508, 2480),
                new ExportPreset(msg.format("export.preset.square", 4096, 4096), 4096, 4096));
    }

    @Override
    public Optional<Dimension> chooseExportSize(Dimension suggested) {
        EdtExecutor.requireEdt();
        JComboBox<ExportPreset> presets = new JComboBox<>(exportPresets().toArray(ExportPreset[]::new));
        JSpinner width = new JSpinner(new SpinnerNumberModel(suggested.width, 16,
                FractalsPreferences.MAX_EXPORT_SIDE, 16));
        JSpinner height = new JSpinner(new SpinnerNumberModel(suggested.height, 16,
                FractalsPreferences.MAX_EXPORT_SIDE, 16));
        presets.setSelectedIndex(-1);
        presets.addActionListener(e -> {
            if (presets.getSelectedItem() instanceof ExportPreset p) {
                width.setValue(p.width());
                height.setValue(p.height());
            }
        });
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 6);
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel(msg.get("export.preset")), c);
        c.gridx = 1;
        panel.add(presets, c);
        c.gridx = 0;
        c.gridy = 1;
        panel.add(new JLabel(msg.get("export.width")), c);
        c.gridx = 1;
        panel.add(width, c);
        c.gridx = 0;
        c.gridy = 2;
        panel.add(new JLabel(msg.get("export.height")), c);
        c.gridx = 1;
        panel.add(height, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        JLabel note = new JLabel(msg.get("export.note"));
        note.setFont(note.getFont().deriveFont(Font.PLAIN, 11f));
        panel.add(note, c);
        int answer = JOptionPane.showConfirmDialog(this, panel, msg.get("dialog.export.title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return Optional.empty();
        return Optional.of(new Dimension((Integer) width.getValue(), (Integer) height.getValue()));
    }

    @Override
    public void copyToClipboard(BufferedImage picture) {
        EdtExecutor.requireEdt();
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new ImageSelection(picture), null);
    }

    @Override
    public void copyToClipboard(String text) {
        EdtExecutor.requireEdt();
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    /** An image on the clipboard. */
    private record ImageSelection(Image image) implements Transferable {
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return image;
        }
    }

    @Override
    public void setCycling(boolean cycling) {
        EdtExecutor.requireEdt();
        if (cycling) cycleTimer.start();
        else cycleTimer.stop();
    }

    @Override
    public void showBar(Bar bar, boolean visible) {
        EdtExecutor.requireEdt();
        // In full screen the bars stay hidden; leaving it shows them as the presenter remembers them.
        if (fullScreen) return;
        switch (bar) {
            case TOOLBAR -> toolBar.setVisible(visible);
            case SETTINGS -> settingsPane.setVisible(visible);
            case STATUS_BAR -> statusBar.setVisible(visible);
        }
        revalidate();
    }

    @Override
    public void setFullScreen(boolean on) {
        EdtExecutor.requireEdt();
        if (on == fullScreen) return;
        GraphicsDevice device = getGraphicsConfiguration().getDevice();
        if (on) {
            normalBounds = getBounds();
            normalState = getExtendedState();
            fullScreen = true;
            setJMenuBar(null);
            toolBar.setVisible(false);
            settingsPane.setVisible(false);
            statusBar.setVisible(false);
            desk.setBorder(null);
            // The window frame can only be removed while the window is not shown.
            super.dispose();
            setUndecorated(true);
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(this);
            } else {
                setExtendedState(NORMAL);
                setBounds(getGraphicsConfiguration().getBounds());
            }
            setVisible(true);
            canvas.showNote(msg.get("status.full-screen"), NOTE_TIME);
            if (progress.isVisible() && !progress.isIndeterminate()) canvas.setProgress(progress.getValue() / 1000.0);
        } else {
            leaveFullScreen(device);
            fullScreen = false;
            setUndecorated(false);
            setJMenuBar(menuBar);
            desk.setBorder(deskBorder);
            for (Bar bar : Bar.values()) showBar(bar, presenter.isBarVisible(bar));
            canvas.hideNote();
            canvas.setProgress(Double.NaN);
            if (normalBounds != null) setBounds(normalBounds);
            setExtendedState(normalState);
            setVisible(true);
        }
        revalidate();
        canvas.requestFocusInWindow();
    }

    boolean isFullScreen() {
        return fullScreen;
    }

    /** Gives the screen back and takes the window down, so that its frame can change. */
    private void leaveFullScreen(GraphicsDevice device) {
        if (device.getFullScreenWindow() == this) device.setFullScreenWindow(null);
        super.dispose();
    }

    @Override
    public boolean confirm(String message, String title) {
        EdtExecutor.requireEdt();
        return JOptionPane.showConfirmDialog(this, plain(message), title, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    @Override
    public void showError(String message) {
        EdtExecutor.requireEdt();
        JOptionPane.showMessageDialog(this, plain(message), msg.get("dialog.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfo(String message) {
        EdtExecutor.requireEdt();
        JOptionPane.showMessageDialog(this, plain(message), getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void showAbout() {
        EdtExecutor.requireEdt();
        JOptionPane.showMessageDialog(this,
                "<html><center><h2>amade-fractals " + AmadeFractalsApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<small>&copy; 2026 datazuul.com &bull; EUPL-1.2</small></center></html>",
                msg.get("dialog.about.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        if (fullScreen) {
            GraphicsDevice device = getGraphicsConfiguration().getDevice();
            if (device.getFullScreenWindow() == this) device.setFullScreenWindow(null);
        }
        canvas.hideNote();
        resizeTimer.stop();
        cycleTimer.stop();
        super.dispose();
    }

    /** A text component, so that messages (with file names and errors) are not read as Swing HTML. */
    private static JTextArea plain(String message) {
        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false);
        text.setLineWrap(message.length() > 200);
        text.setWrapStyleWord(true);
        if (message.length() > 200) text.setColumns(50);
        return text;
    }

    private JPanel statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        Font small = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
        status.setFont(small);
        status.setName("status");
        pointer.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        pointer.setName("pointer");
        zoom.setFont(small);
        zoom.setName("zoom");
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        left.add(status);
        progress.setName("progress");
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(160, 16));
        progress.setVisible(false);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        right.add(pointer);
        right.add(zoom);
        right.add(progress);
        bar.add(left, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}

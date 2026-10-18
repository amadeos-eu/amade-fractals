package eu.amadeos.apps.fractals.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.apps.fractals.domain.ColorSettings;
import eu.amadeos.apps.fractals.domain.Coloring;
import eu.amadeos.apps.fractals.domain.Formula;
import eu.amadeos.apps.fractals.domain.FractalSettings;
import eu.amadeos.apps.fractals.domain.Palette;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Function;

/**
 * The settings beside the picture, in three groups: the fractal (formula, power, Julia seed, iterations,
 * bailout), the location (centre and magnification, to type in or copy) and the colours (colouring,
 * palette, density, offset, cycling). Changes apply at once; text fields when Enter is pressed.
 */
public final class SettingsPanel extends JPanel {
    /** Density slider: 0 to this, mapped logarithmically onto the allowed densities. */
    static final int DENSITY_STEPS = 200;
    static final int OFFSET_STEPS = 200;

    private final Messages msg;
    private final JComboBox<Formula> formula = new JComboBox<>(Formula.values());
    private final JSpinner power = new JSpinner(new SpinnerNumberModel(Formula.MIN_POWER, Formula.MIN_POWER,
            Formula.MAX_POWER, 1));
    private final JCheckBox julia = new JCheckBox();
    private final JTextField seedRe = field(8);
    private final JTextField seedIm = field(8);
    private final JSpinner iterations = new JSpinner(new SpinnerNumberModel(FractalSettings.DEFAULT_ITERATIONS,
            FractalSettings.MIN_ITERATIONS, FractalSettings.MAX_ITERATIONS, 100));
    private final JCheckBox autoIterations = new JCheckBox();
    private final JSpinner bailout = new JSpinner(new SpinnerNumberModel(FractalSettings.DEFAULT_BAILOUT,
            FractalSettings.MIN_BAILOUT, FractalSettings.MAX_BAILOUT, 1.0));
    private final JTextField centerRe = field(14);
    private final JTextField centerIm = field(14);
    private final JTextField magnification = field(14);
    private final JComboBox<Coloring> coloring = new JComboBox<>(Coloring.values());
    private final JComboBox<Palette> palette = new JComboBox<>(Palette.values());
    private final JSlider density = new JSlider(0, DENSITY_STEPS, densityToSlider(1));
    private final JSlider offset = new JSlider(0, OFFSET_STEPS, 0);
    private boolean updating;

    public SettingsPanel(MainPresenter presenter, FractalActions actions, Messages msg) {
        super(new GridBagLayout());
        EdtExecutor.requireEdt();
        this.msg = msg;
        setBorder(BorderFactory.createCompoundBorder(new AmadeUI.MotifBevelBorder(true, 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        formula.setName("formula");
        formula.setRenderer(renderer(f -> msg.get(MainPresenter.formulaKey((Formula) f)), null));
        formula.addActionListener(e -> {
            if (!updating && formula.getSelectedItem() instanceof Formula f) presenter.setFormula(f);
        });
        power.setName("power");
        power.addChangeListener(e -> {
            if (!updating) presenter.setPower((Integer) power.getValue());
        });
        Mnemonics.apply(julia, msg.get("settings.julia"));
        julia.setName("julia");
        julia.addActionListener(e -> {
            if (!updating) presenter.setJulia(julia.isSelected());
        });
        seedRe.setName("seed-re");
        seedIm.setName("seed-im");
        seedRe.addActionListener(e -> presenter.setSeed(seedRe.getText(), seedIm.getText()));
        seedIm.addActionListener(e -> presenter.setSeed(seedRe.getText(), seedIm.getText()));
        iterations.setName("iterations");
        iterations.addChangeListener(e -> {
            if (!updating) presenter.setIterations((Integer) iterations.getValue());
        });
        Mnemonics.apply(autoIterations, msg.get("settings.auto-iterations"));
        autoIterations.setName("auto-iterations");
        autoIterations.addActionListener(e -> presenter.setAutoIterations(autoIterations.isSelected()));
        bailout.setName("bailout");
        bailout.addChangeListener(e -> {
            if (!updating) presenter.setBailout(((Number) bailout.getValue()).doubleValue());
        });

        centerRe.setName("center-re");
        centerIm.setName("center-im");
        magnification.setName("magnification");
        for (JTextField f : new JTextField[]{centerRe, centerIm, magnification}) {
            f.addActionListener(e -> goTo(presenter));
        }
        JButton go = new JButton(Mnemonics.strip(msg.get("settings.go")));
        Mnemonics.apply(go, msg.get("settings.go"));
        go.setName("go");
        go.addActionListener(e -> goTo(presenter));

        coloring.setName("coloring");
        coloring.setRenderer(renderer(c -> msg.get(MainMenuBar.coloringKey((Coloring) c)), null));
        coloring.addActionListener(e -> {
            if (!updating && coloring.getSelectedItem() instanceof Coloring c) presenter.setColoring(c);
        });
        palette.setName("palette");
        palette.setRenderer(renderer(p -> msg.get(MainMenuBar.paletteKey((Palette) p)),
                p -> new GradientIcon((Palette) p)));
        palette.addActionListener(e -> {
            if (!updating && palette.getSelectedItem() instanceof Palette p) presenter.setPalette(p);
        });
        density.setName("density");
        density.addChangeListener(e -> {
            if (!updating) presenter.setDensity(sliderToDensity(density.getValue()));
        });
        offset.setName("offset");
        offset.addChangeListener(e -> {
            if (!updating) presenter.setOffset(offset.getValue() / (double) OFFSET_STEPS);
        });
        JCheckBox cycle = new JCheckBox(actions.cycle);
        cycle.setIcon(null);
        cycle.setName("cycle");

        Rows rows = new Rows();
        rows.heading(msg.get("settings.group.fractal"));
        rows.labelled("settings.formula", formula);
        rows.inline("settings.power", power);
        rows.add(julia, 2);
        rows.labelled("settings.seed", pair(seedRe, seedIm));
        rows.inline("settings.iterations", iterations);
        rows.add(autoIterations, 4);
        rows.inline("settings.bailout", bailout);
        rows.heading(msg.get("settings.group.location"));
        rows.labelled("settings.center-re", centerRe);
        rows.labelled("settings.center-im", centerIm);
        rows.labelled("settings.magnification", pair(magnification, go));
        rows.heading(msg.get("settings.group.colors"));
        rows.labelled("settings.coloring", coloring);
        rows.labelled("settings.palette", palette);
        rows.inline("settings.density", density);
        rows.inline("settings.offset", offset);
        rows.add(cycle, 6);
        JTextArea hint = new JTextArea(msg.get("settings.hint"));
        hint.setName("hint");
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setOpaque(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        rows.add(new JSeparator(SwingConstants.HORIZONTAL), 4);
        rows.add(hint, 0);
        rows.fill();
        setPreferredSize(new Dimension(250, getPreferredSize().height));
    }

    private void goTo(MainPresenter presenter) {
        presenter.goTo(centerRe.getText(), centerIm.getText(), magnification.getText());
    }

    /** Shows the settings without reacting to the changes. */
    public void show(FractalSettings fractal, ColorSettings colors, MainView.Location location, boolean auto) {
        EdtExecutor.requireEdt();
        updating = true;
        try {
            formula.setSelectedItem(fractal.formula());
            power.setValue(fractal.power());
            power.setEnabled(fractal.formula().usesPower());
            julia.setSelected(fractal.julia());
            julia.setEnabled(fractal.formula().hasJulia());
            seedRe.setText(Formats.seed(fractal.seedRe()));
            seedIm.setText(Formats.seed(fractal.seedIm()));
            boolean seedUsed = fractal.julia();
            seedRe.setEnabled(seedUsed);
            seedIm.setEnabled(seedUsed);
            iterations.setValue(fractal.maxIterations());
            autoIterations.setSelected(auto);
            bailout.setValue(fractal.bailout());
            centerRe.setText(location.re());
            centerIm.setText(location.im());
            magnification.setText(location.magnification());
            for (JTextField f : new JTextField[]{centerRe, centerIm, magnification}) f.setCaretPosition(0);
            coloring.setSelectedItem(colors.coloring());
            palette.setSelectedItem(colors.palette());
            density.setValue(densityToSlider(colors.density()));
            offset.setValue((int) Math.round(colors.offset() * OFFSET_STEPS) % (OFFSET_STEPS + 1));
        } finally {
            updating = false;
        }
    }

    static double sliderToDensity(int value) {
        double ratio = ColorSettings.MAX_DENSITY / ColorSettings.MIN_DENSITY;
        return ColorSettings.MIN_DENSITY * Math.pow(ratio, value / (double) DENSITY_STEPS);
    }

    static int densityToSlider(double density) {
        double ratio = ColorSettings.MAX_DENSITY / ColorSettings.MIN_DENSITY;
        double value = Math.log(density / ColorSettings.MIN_DENSITY) / Math.log(ratio) * DENSITY_STEPS;
        return (int) Math.max(0, Math.min(DENSITY_STEPS, Math.round(value)));
    }

    // For tests.
    JComboBox<Formula> formulaBox() {
        return formula;
    }

    JCheckBox juliaBox() {
        return julia;
    }

    JSpinner iterationsSpinner() {
        return iterations;
    }

    JTextField magnificationField() {
        return magnification;
    }

    JTextField centerReField() {
        return centerRe;
    }

    JComboBox<Palette> paletteBox() {
        return palette;
    }

    private static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return f;
    }

    private static JPanel pair(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(a, c);
        c.insets = new Insets(0, 4, 0, 0);
        // A button keeps its own width; a second field shares the row.
        c.weightx = b instanceof JButton ? 0 : 1;
        p.add(b, c);
        return p;
    }

    private static DefaultListCellRenderer renderer(Function<Object, String> text, Function<Object, Icon> icon) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(text.apply(value));
                    if (icon != null) setIcon(icon.apply(value));
                }
                return this;
            }
        };
    }

    /** A strip of the palette's colours. */
    private static final class GradientIcon implements Icon {
        private final Palette palette;

        GradientIcon(Palette palette) {
            this.palette = palette;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Palette.Gradient gradient = palette.gradient(AmadeUI.theme());
            int w = getIconWidth();
            for (int i = 0; i < w; i++) {
                g.setColor(new java.awt.Color(gradient.at(i / (double) w)));
                g.drawLine(x + i, y, x + i, y + getIconHeight() - 1);
            }
            AmadeUI.paintBevel(g, x, y, w, getIconHeight(), false, 1);
        }

        @Override
        public int getIconWidth() {
            return 40;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }

    /** Lays out the panel row by row in two columns: headings, labels above or beside their fields. */
    private final class Rows {
        private final GridBagConstraints c = new GridBagConstraints();

        Rows() {
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
        }

        void heading(String text) {
            JLabel label = new JLabel(text);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            full(label, new Insets(c.gridy == 0 ? 0 : 6, 0, 2, 0));
            full(new JSeparator(SwingConstants.HORIZONTAL), new Insets(0, 0, 4, 0));
        }

        /** The label above the field. */
        void labelled(String key, JComponent field) {
            full(label(key, field), new Insets(0, 0, 1, 0));
            full(field, new Insets(0, 0, 4, 0));
        }

        /** The label left of the field. */
        void inline(String key, JComponent field) {
            c.gridwidth = 1;
            c.gridx = 0;
            c.weightx = 0;
            c.insets = new Insets(0, 0, 4, 6);
            SettingsPanel.this.add(label(key, field), c);
            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(0, 0, 4, 0);
            SettingsPanel.this.add(field, c);
            c.gridy++;
        }

        void add(JComponent component, int gapBelow) {
            full(component, new Insets(0, 0, gapBelow, 0));
        }

        void fill() {
            c.weighty = 1;
            full(new JPanel(), new Insets(0, 0, 0, 0));
        }

        private void full(JComponent component, Insets insets) {
            c.gridx = 0;
            c.gridwidth = 2;
            c.weightx = 1;
            c.insets = insets;
            SettingsPanel.this.add(component, c);
            c.gridy++;
        }

        private JLabel label(String key, JComponent field) {
            JLabel label = Mnemonics.apply(new JLabel(), msg.get(key));
            label.setLabelFor(field instanceof JPanel p && p.getComponentCount() > 0 ? p.getComponent(0)
                    : field instanceof JSpinner s ? ((JSpinner.DefaultEditor) s.getEditor()).getTextField() : field);
            return label;
        }
    }
}

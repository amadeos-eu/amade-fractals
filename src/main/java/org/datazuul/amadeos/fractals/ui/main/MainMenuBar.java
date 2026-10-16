package org.datazuul.amadeos.fractals.ui.main;

import org.datazuul.amadeos.commons.i18n.Language;
import org.datazuul.amadeos.commons.i18n.LocaleSettings;
import org.datazuul.amadeos.commons.i18n.Messages;
import org.datazuul.amadeos.commons.i18n.Mnemonics;
import org.datazuul.amadeos.commons.swing.EdtExecutor;
import org.datazuul.amadeos.fractals.domain.ColorSettings;
import org.datazuul.amadeos.fractals.domain.Coloring;
import org.datazuul.amadeos.fractals.domain.Formula;
import org.datazuul.amadeos.fractals.domain.FractalSettings;
import org.datazuul.amadeos.fractals.domain.Palette;
import org.datazuul.amadeos.fractals.domain.Place;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences.Bar;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.border.BevelBorder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** File, Edit, View, Fractal, Colours, Places, Options and Help. */
public final class MainMenuBar extends JMenuBar {
    private final Messages msg;
    private final Map<Formula, JRadioButtonMenuItem> formulas = new EnumMap<>(Formula.class);
    private final Map<Coloring, JRadioButtonMenuItem> colorings = new EnumMap<>(Coloring.class);
    private final Map<Palette, JRadioButtonMenuItem> palettes = new EnumMap<>(Palette.class);
    private final JCheckBoxMenuItem autoIterations;

    public MainMenuBar(MainPresenter presenter, FractalActions actions, Messages msg, LocaleSettings locales) {
        EdtExecutor.requireEdt();
        this.msg = msg;
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        JMenu file = menu("menu.file");
        file.add(item(actions.open));
        file.add(item(actions.saveParameters));
        file.addSeparator();
        file.add(item(actions.saveImage));
        file.add(item(actions.exportImage));
        file.addSeparator();
        file.add(item(actions.exit));

        JMenu edit = menu("menu.edit");
        edit.add(item(actions.back));
        edit.add(item(actions.forward));
        edit.addSeparator();
        edit.add(item(actions.copy));
        edit.add(item(actions.copyParameters));

        JMenu view = menu("menu.view");
        for (Bar bar : Bar.values()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(msg.get(barKey(bar)), presenter.isBarVisible(bar));
            item.setName(barKey(bar));
            item.addActionListener(e -> presenter.setBarVisible(bar, item.isSelected()));
            view.add(item);
        }
        view.addSeparator();
        view.add(item(actions.home));
        view.add(item(actions.zoomIn));
        view.add(item(actions.zoomOut));
        view.addSeparator();
        view.add(item(actions.refresh));
        view.add(item(actions.stop));

        JMenu fractal = menu("menu.fractal");
        ButtonGroup formulaGroup = new ButtonGroup();
        for (Formula formula : Formula.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(msg.get(MainPresenter.formulaKey(formula)));
            item.setName(MainPresenter.formulaKey(formula));
            item.addActionListener(e -> presenter.setFormula(formula));
            formulaGroup.add(item);
            formulas.put(formula, item);
            fractal.add(item);
        }
        fractal.addSeparator();
        JCheckBoxMenuItem julia = new JCheckBoxMenuItem(actions.julia);
        julia.setName("menu.fractal.julia");
        julia.setIcon(null);
        fractal.add(julia);
        fractal.addSeparator();
        fractal.add(item(actions.moreIterations));
        fractal.add(item(actions.fewerIterations));
        autoIterations = Mnemonics.apply(new JCheckBoxMenuItem(), msg.get("menu.fractal.auto-iterations"));
        autoIterations.setName("menu.fractal.auto-iterations");
        autoIterations.setSelected(presenter.autoIterations());
        autoIterations.addActionListener(e -> presenter.setAutoIterations(autoIterations.isSelected()));
        fractal.add(autoIterations);

        JMenu colors = menu("menu.colors");
        ButtonGroup coloringGroup = new ButtonGroup();
        for (Coloring coloring : Coloring.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(msg.get(coloringKey(coloring)));
            item.setName(coloringKey(coloring));
            item.addActionListener(e -> presenter.setColoring(coloring));
            coloringGroup.add(item);
            colorings.put(coloring, item);
            colors.add(item);
        }
        colors.addSeparator();
        ButtonGroup paletteGroup = new ButtonGroup();
        for (Palette palette : Palette.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(msg.get(paletteKey(palette)));
            item.setName(paletteKey(palette));
            item.addActionListener(e -> presenter.setPalette(palette));
            paletteGroup.add(item);
            palettes.put(palette, item);
            colors.add(item);
        }
        colors.addSeparator();
        JCheckBoxMenuItem cycle = new JCheckBoxMenuItem(actions.cycle);
        cycle.setName("menu.colors.cycle");
        cycle.setIcon(null);
        colors.add(cycle);

        JMenu places = menu("menu.places");
        for (Place place : Place.values()) {
            JMenuItem item = new JMenuItem(msg.get(place.key()));
            item.setName(place.key());
            item.addActionListener(e -> presenter.goToPlace(place));
            places.add(item);
            if (place == Place.DEEP_SEAHORSE || place == Place.SAN_MARCO_DRAGON) places.addSeparator();
        }

        JMenu options = menu("menu.options");
        if (locales != null) options.add(languageMenu(presenter, locales));

        JMenu help = menu("menu.help");
        JMenuItem about = new JMenuItem(msg.get("menu.help.about"));
        about.setName("menu.help.about");
        about.addActionListener(e -> presenter.about());
        help.add(about);
    }

    /** Marks the formula, colouring and palette in use. */
    public void show(FractalSettings fractal, ColorSettings colorSettings, boolean auto) {
        formulas.get(fractal.formula()).setSelected(true);
        colorings.get(colorSettings.coloring()).setSelected(true);
        palettes.get(colorSettings.palette()).setSelected(true);
        autoIterations.setSelected(auto);
    }

    static String barKey(Bar bar) {
        return switch (bar) {
            case TOOLBAR -> "menu.view.toolbar";
            case SETTINGS -> "menu.view.settings";
            case STATUS_BAR -> "menu.view.status-bar";
        };
    }

    static String coloringKey(Coloring coloring) {
        return "coloring." + coloring.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    static String paletteKey(Palette palette) {
        return "palette." + palette.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** "Desktop setting (Deutsch)" and the five languages; the choice applies at the next start. */
    private JMenu languageMenu(MainPresenter presenter, LocaleSettings locales) {
        JMenu menu = new JMenu(msg.get("menu.options.language"));
        Messages commons = Messages.commons(msg.locale());
        List<Optional<Language>> choices = new ArrayList<>();
        choices.add(Optional.empty());
        for (Language language : Language.values()) choices.add(Optional.of(language));
        ButtonGroup group = new ButtonGroup();
        Optional<Language> current = locales.applicationLanguage();
        for (Optional<Language> choice : choices) {
            String label = choice.map(Language::nativeName).orElseGet(() -> commons.format("language.follow-desktop",
                    locales.desktopResolution().language().nativeName()));
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, choice.equals(current));
            item.setName(choice.map(Language::code).orElse(LocaleSettings.FOLLOW_DESKTOP));
            item.addActionListener(e -> presenter.setLanguage(choice));
            group.add(item);
            menu.add(item);
            if (choice.isEmpty()) menu.addSeparator();
        }
        return menu;
    }

    private JMenu menu(String key) {
        JMenu menu = Mnemonics.apply(new JMenu(), msg.get(key));
        menu.setName(key);
        add(menu);
        return menu;
    }

    private static JMenuItem item(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setName(String.valueOf(action.getValue(Action.ACTION_COMMAND_KEY)));
        // Menus show text; the small icons would crowd them.
        item.setIcon(null);
        return item;
    }
}

package org.datazuul.amadeos.fractals.ui.main;

import org.datazuul.amadeos.commons.i18n.Language;
import org.datazuul.amadeos.commons.i18n.Messages;
import org.datazuul.amadeos.commons.i18n.Mnemonics;
import org.datazuul.amadeos.commons.i18n.TranslationCheck;
import org.datazuul.amadeos.fractals.bootstrap.FractalsModule;
import org.datazuul.amadeos.fractals.domain.Coloring;
import org.datazuul.amadeos.fractals.domain.Formula;
import org.datazuul.amadeos.fractals.domain.Palette;
import org.datazuul.amadeos.fractals.domain.Place;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(FractalsModule.class, FractalsModule.MESSAGES));
    }

    @Test
    void everyNameHasAText() {
        Messages english = MainPresenterTest.MSG;
        for (Formula f : Formula.values()) assertFalse(english.get(MainPresenter.formulaKey(f)).startsWith("!"));
        for (Coloring c : Coloring.values()) assertFalse(english.get(MainMenuBar.coloringKey(c)).startsWith("!"));
        for (Palette p : Palette.values()) assertFalse(english.get(MainMenuBar.paletteKey(p)).startsWith("!"));
        for (Place p : Place.values()) assertFalse(english.get(p.key()).startsWith("!"));
    }

    @Test
    void textsAreFormattedInTheLanguage() {
        Messages german = Messages.load(FractalsModule.class, FractalsModule.MESSAGES, Locale.GERMANY);
        // No thousands separator in pixel counts, a decimal comma in times.
        assertEquals("1920 × 1080 Pixel in 1,50 s.", german.format("status.rendered", 1920, 1080,
                german.format("time.seconds", 1.5)));
        Messages french = Messages.load(FractalsModule.class, FractalsModule.MESSAGES, Locale.FRANCE);
        assertEquals("À propos d'amade-fractals", french.get("menu.help.about"));
        assertEquals("Impossible d'ouvrir a.fractal : b", french.format("error.open", "a.fractal", "b"));
    }

    /** Within the menu bar and within each menu, no two entries share a mnemonic letter. */
    @Test
    void mnemonicsAreUniquePerMenu() {
        Map<String, List<String>> menus = Map.of(
                "bar", List.of("menu.file", "menu.edit", "menu.view", "menu.fractal", "menu.colors", "menu.places",
                        "menu.options", "menu.help"),
                "file", List.of("menu.file.open", "menu.file.save", "menu.file.save-image", "menu.file.export",
                        "menu.file.exit"),
                "edit", List.of("menu.edit.back", "menu.edit.forward", "menu.edit.copy", "menu.edit.copy-parameters"),
                "view", List.of("menu.view.home", "menu.view.zoom-in", "menu.view.zoom-out",
                        "menu.view.full-screen", "menu.view.refresh", "menu.view.stop"),
                "fractal", List.of("menu.fractal.julia", "menu.fractal.more-iterations",
                        "menu.fractal.fewer-iterations", "menu.fractal.auto-iterations"));
        Map<String, String> clashes = new HashMap<>();
        for (Language language : Language.values()) {
            Messages messages = Messages.load(FractalsModule.class, FractalsModule.MESSAGES, language.locale());
            for (Map.Entry<String, List<String>> menu : menus.entrySet()) {
                Map<Character, String> seen = new HashMap<>();
                for (String key : menu.getValue()) {
                    Mnemonics.Text t = Mnemonics.parse(messages.get(key));
                    if (t.index() < 0) {
                        clashes.put(language.code() + " " + key, "no mnemonic");
                        continue;
                    }
                    char letter = Character.toLowerCase(t.text().charAt(t.index()));
                    String other = seen.put(letter, key);
                    if (other != null) clashes.put(language.code() + " " + key, other);
                }
            }
        }
        assertEquals(Map.of(), clashes);
    }
}

package eu.amadeos.apps.fractals.ui.main;

import eu.amadeos.commons.swing.AmadeUI;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import java.awt.Insets;

/**
 * Square Motif buttons for the toolbar, raised in 3D like the buttons of the Front Panel (as in
 * amade-writepad): icon only, tooltip from the action, never take the focus.
 */
final class ToolButtons {
    private ToolButtons() {
    }

    static JButton button(Action action) {
        JButton button = new JButton(action);
        button.setHideActionText(true);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        // Not a UIResource, so the toolbar's look and feel does not swap in its flat rollover border.
        button.setBorder(BorderFactory.createCompoundBorder(new AmadeUI.MotifButtonBorder(2),
                BorderFactory.createEmptyBorder()));
        button.setName(String.valueOf(action.getValue(Action.ACTION_COMMAND_KEY)));
        return button;
    }

    /** A button that stays pressed while its action is selected, e.g. Julia set or colour cycling. */
    static JToggleButton toggle(Action action) {
        JToggleButton button = new JToggleButton(action);
        button.setHideActionText(true);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 2, 2, 2));
        button.setBorder(BorderFactory.createCompoundBorder(new AmadeUI.MotifButtonBorder(2),
                BorderFactory.createEmptyBorder()));
        button.setName(String.valueOf(action.getValue(Action.ACTION_COMMAND_KEY)));
        return button;
    }
}

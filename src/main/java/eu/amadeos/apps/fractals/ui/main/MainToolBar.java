package eu.amadeos.apps.fractals.ui.main;

import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.commons.swing.ToolButtons;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;
import java.util.List;

/** Open, save, picture, export; back, forward, home; zoom, full screen; Julia, colour cycling; refresh, stop. */
public final class MainToolBar extends JToolBar {
    public MainToolBar(FractalActions actions) {
        EdtExecutor.requireEdt();
        setFloatable(false);
        setRollover(false);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED),
                BorderFactory.createEmptyBorder(2, 3, 2, 3)));
        group(List.of(actions.open, actions.saveParameters, actions.saveImage, actions.exportImage));
        group(List.of(actions.back, actions.forward, actions.home));
        add(ToolButtons.button(actions.zoomIn));
        add(ToolButtons.button(actions.zoomOut));
        add(ToolButtons.toggle(actions.fullScreen));
        addSeparator();
        add(ToolButtons.toggle(actions.julia));
        add(ToolButtons.toggle(actions.cycle));
        addSeparator();
        add(ToolButtons.button(actions.refresh));
        add(ToolButtons.button(actions.stop));
    }

    private void group(List<Action> group) {
        for (Action a : group) add(ToolButtons.button(a));
        addSeparator();
    }
}

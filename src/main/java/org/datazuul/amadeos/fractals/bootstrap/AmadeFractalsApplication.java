package org.datazuul.amadeos.fractals.bootstrap;

import org.datazuul.amadeos.commons.app.AmadeApplication;
import org.datazuul.amadeos.commons.app.AppInfo;
import org.datazuul.amadeos.fractals.ui.main.MainPresenter;
import org.datazuul.amadeos.fractals.ui.main.MainWindow;

import java.util.List;

public final class AmadeFractalsApplication {
    public static final AppInfo APP = new AppInfo("amade-fractals", "Fractals", "fractals");

    private AmadeFractalsApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeFractalsApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    /** Opens the parameter file named as first argument, if any. */
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            MainWindow window = injector.getInstance(MainWindow.class);
            MainPresenter presenter = injector.getInstance(MainPresenter.class);
            window.bind(presenter);
            window.setVisible(true);
            presenter.start(List.of(args));
        }, new FractalsModule());
    }
}

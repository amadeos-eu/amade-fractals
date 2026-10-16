package org.datazuul.amadeos.fractals.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.datazuul.amadeos.commons.i18n.Messages;
import org.datazuul.amadeos.commons.swing.EdtExecutor;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences;
import org.datazuul.amadeos.fractals.repository.ImageFiles;
import org.datazuul.amadeos.fractals.repository.ParameterFiles;
import org.datazuul.amadeos.fractals.service.render.RenderService;
import org.datazuul.amadeos.fractals.service.render.Renderer;
import org.datazuul.amadeos.fractals.ui.main.MainView;
import org.datazuul.amadeos.fractals.ui.main.MainWindow;
import org.datazuul.amadeos.fractals.ui.main.Ui;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * amade-fractals bindings. The background executor, theme, language and the other shared services come
 * from {@link org.datazuul.amadeos.commons.app.CommonsModule}.
 */
public final class FractalsModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "org.datazuul.amadeos.fractals.messages";

    @Override
    protected void configure() {
        bind(ImageFiles.class).in(Scopes.SINGLETON);
        bind(FractalsPreferences.class).in(Scopes.SINGLETON);
        bind(MainWindow.class).in(Scopes.SINGLETON);
        bind(MainView.class).to(MainWindow.class);
    }

    @Provides
    @Singleton
    ParameterFiles parameterFiles(ImageFiles files) {
        return new ParameterFiles(files);
    }

    /**
     * The calculation runs on one platform thread per core, a little below normal priority so that the
     * window stays responsive.
     */
    @Provides
    @Singleton
    Renderer renderer() {
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        return new Renderer(Executors.newFixedThreadPool(cores, workerThreads()), cores);
    }

    static ThreadFactory workerThreads() {
        AtomicInteger count = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "fractal-worker-" + count.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
    }

    @Provides
    @Singleton
    RenderService renderService(Renderer renderer, ExecutorService background, @Ui Executor ui) {
        return new RenderService(renderer, background, ui);
    }

    /** Continues on the event dispatch thread after background work. */
    @Provides
    @Ui
    Executor ui(EdtExecutor edt) {
        return edt;
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(FractalsModule.class, MESSAGES, locale);
    }
}

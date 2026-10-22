package eu.amadeos.apps.fractals.service.render;

import eu.amadeos.apps.fractals.domain.ColorSettings;
import eu.amadeos.apps.fractals.domain.FractalSettings;
import eu.amadeos.apps.fractals.domain.Palette.Gradient;
import eu.amadeos.apps.fractals.domain.Region;
import eu.amadeos.apps.fractals.service.color.Colorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Renders in the background and reports on the event dispatch thread. A large picture is first calculated
 * at a quarter of its size, so that something appears at once while the details follow. Nothing is reported
 * any more once a task is cancelled.
 */
public class RenderService {
    private static final Logger LOG = LoggerFactory.getLogger(RenderService.class);
    /** Pictures with more pixels get a coarse preview first. */
    static final int PREVIEW_ABOVE = 200_000;
    static final int PREVIEW_DIVISOR = 4;
    /** Rows calculated at a time when exporting. */
    static final int EXPORT_STRIP = 128;

    /** Called on the event dispatch thread, never after the task was cancelled. */
    public interface Listener {
        /** Share of the work done, 0 to 1. */
        void progress(double fraction);

        /** A coarse version of the picture, smaller than asked for. */
        default void preview(FractalData data) {
        }

        void done(FractalData data, Duration time);

        void failed(Throwable error);
    }

    /** A running calculation. */
    public static final class Task {
        private final AtomicBoolean cancelled = new AtomicBoolean();

        public void cancel() {
            cancelled.set(true);
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    private final Renderer renderer;
    private final ExecutorService background;
    private final Executor ui;

    public RenderService(Renderer renderer, ExecutorService background, Executor ui) {
        this.renderer = renderer;
        this.background = background;
        this.ui = ui;
    }

    /**
     * Starts rendering a picture of the given size.
     *
     * @param preview whether a large picture gets a coarse preview first
     */
    public Task render(FractalSettings settings, int width, int height, boolean preview, Listener listener) {
        Task task = new Task();
        background.execute(() -> run(task, settings, Math.max(1, width), Math.max(1, height), preview, listener));
        return task;
    }

    // --8<-- [start:run]
    private void run(Task task, FractalSettings settings, int width, int height, boolean preview, Listener listener) {
        long start = System.nanoTime();
        AtomicLong lastReport = new AtomicLong();
        try {
            boolean coarse = preview && (long) width * height > PREVIEW_ABOVE;
            double coarseShare = coarse ? 1.0 / (PREVIEW_DIVISOR * PREVIEW_DIVISOR + 1) : 0;
            if (coarse) {
                FractalData small = renderer.render(settings, Math.max(1, width / PREVIEW_DIVISOR),
                        Math.max(1, height / PREVIEW_DIVISOR), task::isCancelled,
                        (done, rows) -> report(task, listener, lastReport, coarseShare * done / rows));
                post(task, () -> listener.preview(small));
            }
            FractalData data = renderer.render(settings, width, height, task::isCancelled,
                    (done, rows) -> report(task, listener, lastReport,
                            coarseShare + (1 - coarseShare) * done / rows));
            Duration time = Duration.ofNanos(System.nanoTime() - start);
            post(task, () -> listener.done(data, time));
        } catch (CancellationException e) {
            // A newer picture is wanted; nothing to report.
        } catch (RuntimeException | OutOfMemoryError e) {
            LOG.warn("Rendering failed", e);
            post(task, () -> listener.failed(e));
        }
    }
    // --8<-- [end:run]

    /** Called on the event dispatch thread, never after the task was cancelled. */
    public interface ExportListener {
        void progress(double fraction);

        void done(BufferedImage image, Duration time);

        void failed(Throwable error);
    }

    /**
     * Renders a large picture strip by strip and colours each strip at once, so that only the finished RGB
     * picture is kept in memory, never the values of all its pixels.
     */
    public Task export(FractalSettings settings, ColorSettings colors, Gradient gradient, int width, int height,
                       ExportListener listener) {
        Task task = new Task();
        background.execute(() -> {
            long start = System.nanoTime();
            AtomicLong lastReport = new AtomicLong();
            try {
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Region region = settings.region();
                double pixel = region.pixelSize(height);
                // --8<-- [start:strips]
                for (int top = 0; top < height; top += EXPORT_STRIP) {
                    int rows = Math.min(EXPORT_STRIP, height - top);
                    // The strip's own region: same pixel size, centred on the strip.
                    double shift = (height / 2.0 - (top + rows / 2.0)) * pixel;
                    Region strip = new Region(region.re(), region.im().add(new java.math.BigDecimal(shift)),
                            pixel * rows);
                    int done = top;
                    FractalData data = renderer.render(settings.withRegion(strip), width, rows, task::isCancelled,
                            (r, total) -> report(task, listener::progress, lastReport,
                                    (done + r) / (double) height));
                    BufferedImage part = Colorizer.image(data, colors, gradient);
                    image.getRaster().setDataElements(0, top, part.getRaster());
                }
                // --8<-- [end:strips]
                Duration time = Duration.ofNanos(System.nanoTime() - start);
                post(task, () -> listener.done(image, time));
            } catch (CancellationException e) {
                // Stopped by the user.
            } catch (RuntimeException | OutOfMemoryError e) {
                LOG.warn("Export failed", e);
                post(task, () -> listener.failed(e));
            }
        });
        return task;
    }

    private void report(Task task, Listener listener, AtomicLong lastReport, double fraction) {
        report(task, listener::progress, lastReport, fraction);
    }

    /** At most every 50 ms, so that the event queue is not flooded. */
    private void report(Task task, java.util.function.DoubleConsumer progress, AtomicLong lastReport,
                        double fraction) {
        long now = System.nanoTime();
        long last = lastReport.get();
        if (now - last < 50_000_000L || !lastReport.compareAndSet(last, now)) return;
        post(task, () -> progress.accept(fraction));
    }

    // --8<-- [start:post]
    private void post(Task task, Runnable action) {
        ui.execute(() -> {
            if (!task.isCancelled()) action.run();
        });
    }
    // --8<-- [end:post]
}

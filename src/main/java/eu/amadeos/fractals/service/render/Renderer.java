package eu.amadeos.fractals.service.render;

import eu.amadeos.fractals.domain.FractalSettings;
import eu.amadeos.fractals.domain.Region;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Calculates a picture on all processor cores: the rows go to the workers one after another, each worker
 * taking the next free row, so that rows with many iterations do not hold up a fixed share of the picture.
 * Deep zooms into the Mandelbrot set go through {@link Perturbation}, everything else through
 * {@link PointCalculator}.
 */
public final class Renderer {
    /** Called from the workers after each row. */
    @FunctionalInterface
    public interface Progress {
        void rowsDone(int done, int rows);
    }

    private final Executor workers;
    private final int parallelism;

    /**
     * @param workers     runs the calculation; platform threads, as the work is pure calculation
     * @param parallelism how many rows are calculated at the same time
     */
    public Renderer(Executor workers, int parallelism) {
        this.workers = workers;
        this.parallelism = Math.max(1, parallelism);
    }

    /** Calculates in the calling thread; for tests and small pictures. */
    public static Renderer direct() {
        return new Renderer(Runnable::run, 1);
    }

    /**
     * Calculates the region of the settings for a picture of the given size; blocks until done.
     *
     * @throws CancellationException if {@code cancelled} became true meanwhile
     */
    public FractalData render(FractalSettings settings, int width, int height, BooleanSupplier cancelled,
                              Progress progress) {
        FractalData data = new FractalData(settings, width, height);
        Region region = settings.region();
        double pixel = region.pixelSize(height);
        double left = region.offsetRe(0.5, width, height);
        double top = region.offsetIm(0.5, width, height);
        RowTask rowTask;
        if (settings.needsPerturbation(height)) {
            Perturbation perturbation = Perturbation.reference(settings, height, cancelled);
            data.setPerturbation(true);
            rowTask = y -> {
                double dci = top - y * pixel;
                int index = y * width;
                for (int x = 0; x < width && !cancelled.getAsBoolean(); x++) {
                    perturbation.compute(left + x * pixel, dci, data, index + x);
                }
            };
        } else {
            PointCalculator calculator = new PointCalculator(settings);
            double re = region.reDouble() + left;
            double im = region.imDouble() + top;
            rowTask = y -> {
                double ci = im - y * pixel;
                int index = y * width;
                for (int x = 0; x < width && !cancelled.getAsBoolean(); x++) {
                    calculator.compute(re + x * pixel, ci, data, index + x);
                }
            };
        }
        run(rowTask, height, cancelled, progress);
        return data;
    }

    private void run(RowTask rowTask, int rows, BooleanSupplier cancelled, Progress progress) {
        AtomicInteger next = new AtomicInteger();
        AtomicInteger done = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        int tasks = Math.min(parallelism, rows);
        CountDownLatch finished = new CountDownLatch(tasks);
        for (int t = 0; t < tasks; t++) {
            workers.execute(() -> {
                try {
                    int y;
                    while (failure.get() == null && !cancelled.getAsBoolean() && (y = next.getAndIncrement()) < rows) {
                        rowTask.row(y);
                        progress.rowsDone(done.incrementAndGet(), rows);
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                } finally {
                    finished.countDown();
                }
            });
        }
        try {
            finished.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Interrupted");
        }
        Throwable error = failure.get();
        if (error instanceof RuntimeException runtime) throw runtime;
        if (error instanceof Error fatal) throw fatal;
        if (error != null) throw new IllegalStateException(error);
        if (cancelled.getAsBoolean()) throw new CancellationException();
    }

    @FunctionalInterface
    private interface RowTask {
        void row(int y);
    }
}

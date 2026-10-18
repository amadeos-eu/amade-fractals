package eu.amadeos.fractals.service.render;

import eu.amadeos.fractals.QueueExecutor;
import eu.amadeos.fractals.domain.ColorSettings;
import eu.amadeos.fractals.domain.FractalSettings;
import eu.amadeos.fractals.domain.Palette;
import eu.amadeos.fractals.domain.Place;
import eu.amadeos.fractals.domain.Region;
import eu.amadeos.fractals.service.color.Colorizer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendererTest {
    private static final FractalSettings SMALL = FractalSettings.DEFAULT.withMaxIterations(200);

    @Test
    void parallelRenderingEqualsSerial() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            FractalData serial = Renderer.direct().render(SMALL, 120, 90, () -> false, (d, r) -> { });
            AtomicInteger reports = new AtomicInteger();
            FractalData parallel = new Renderer(pool, 4).render(SMALL, 120, 90, () -> false,
                    (d, r) -> reports.incrementAndGet());
            assertEquals(90, reports.get());
            for (int i = 0; i < serial.size(); i++) {
                assertEquals(serial.iterations(i), parallel.iterations(i));
                assertEquals(serial.state(i), parallel.state(i));
            }
            assertTrue(serial.countInside() > 1000);
            assertFalse(serial.perturbation());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void stopsWhenCancelled() {
        AtomicInteger rows = new AtomicInteger();
        assertThrows(CancellationException.class, () -> Renderer.direct().render(SMALL, 50, 50,
                () -> rows.get() >= 10, (d, r) -> rows.set(d)));
        assertEquals(10, rows.get());
    }

    @Test
    void failuresReachTheCaller() {
        assertThrows(IllegalStateException.class, () -> Renderer.direct().render(SMALL, 20, 20, () -> false,
                (d, r) -> {
                    throw new IllegalStateException("boom");
                }));
    }

    @Test
    void deepZoomUsesPerturbation() {
        FractalSettings deep = Place.DEEP_SEAHORSE.settings();
        FractalData data = Renderer.direct().render(deep, 24, 16, () -> false, (d, r) -> { });
        assertTrue(data.perturbation());
        // Structure, not one colour: the iteration counts vary across the picture.
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (int i = 0; i < data.size(); i++) {
            min = Math.min(min, data.iterations(i));
            max = Math.max(max, data.iterations(i));
        }
        assertTrue(max - min > 100, () -> "flat picture");
    }

    @Test
    void serviceShowsAPreviewThenThePicture() {
        QueueExecutor executor = new QueueExecutor();
        RenderService service = new RenderService(Renderer.direct(), executor, executor);
        List<String> events = new ArrayList<>();
        FractalData[] result = new FractalData[1];
        service.render(SMALL, 600, 400, true, listener(events, result));
        executor.runAll();
        assertTrue(events.indexOf("preview 150×100") >= 0, events::toString);
        assertTrue(events.indexOf("preview 150×100") < events.indexOf("done"));
        assertTrue(events.contains("progress"));
        assertEquals("done", events.get(events.size() - 1));
        assertEquals(600, result[0].width());
    }

    @Test
    void smallPicturesNeedNoPreview() {
        QueueExecutor executor = new QueueExecutor();
        RenderService service = new RenderService(Renderer.direct(), executor, executor);
        List<String> events = new ArrayList<>();
        service.render(SMALL, 100, 80, true, listener(events, new FractalData[1]));
        executor.runAll();
        assertFalse(events.stream().anyMatch(e -> e.startsWith("preview")));
        assertEquals("done", events.get(events.size() - 1));
    }

    @Test
    void cancelledTasksReportNothing() {
        QueueExecutor executor = new QueueExecutor();
        RenderService service = new RenderService(Renderer.direct(), executor, executor);
        List<String> events = new ArrayList<>();
        RenderService.Task task = service.render(SMALL, 600, 400, true, listener(events, new FractalData[1]));
        task.cancel();
        executor.runAll();
        assertEquals(List.of(), events);
    }

    @Test
    void exportAssemblesStripsIntoTheWholePicture() {
        QueueExecutor executor = new QueueExecutor();
        RenderService service = new RenderService(Renderer.direct(), executor, executor);
        FractalSettings s = SMALL.withRegion(Region.of(-0.7453, 0.1127, 0.05));
        int w = 160;
        int h = RenderService.EXPORT_STRIP * 2 + 37;
        BufferedImage[] exported = new BufferedImage[1];
        Palette.Gradient gradient = Palette.CLASSIC.gradient(null);
        service.export(s, ColorSettings.DEFAULT, gradient, w, h, new RenderService.ExportListener() {
            @Override
            public void progress(double fraction) {
            }

            @Override
            public void done(BufferedImage image, Duration time) {
                exported[0] = image;
            }

            @Override
            public void failed(Throwable error) {
                throw new AssertionError(error);
            }
        });
        executor.runAll();
        assertNotNull(exported[0]);
        BufferedImage whole = Colorizer.image(Renderer.direct().render(s, w, h, () -> false, (d, r) -> { }),
                ColorSettings.DEFAULT, gradient);
        int differ = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (exported[0].getRGB(x, y) != whole.getRGB(x, y)) differ++;
            }
        }
        // The strips' coordinates may differ in the last bit; that may change a pixel here and there.
        assertTrue(differ <= w * h / 200, "differing pixels: " + differ);
    }

    private static RenderService.Listener listener(List<String> events, FractalData[] result) {
        return new RenderService.Listener() {
            @Override
            public void progress(double fraction) {
                if (!events.contains("progress")) events.add("progress");
            }

            @Override
            public void preview(FractalData data) {
                events.add("preview " + data.width() + "×" + data.height());
            }

            @Override
            public void done(FractalData data, Duration time) {
                result[0] = data;
                events.add("done");
            }

            @Override
            public void failed(Throwable error) {
                events.add("failed " + error);
            }
        };
    }
}

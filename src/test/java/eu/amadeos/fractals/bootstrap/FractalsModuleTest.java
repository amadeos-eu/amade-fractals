package eu.amadeos.fractals.bootstrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FractalsModuleTest {
    @Test
    void appInfo() {
        assertEquals("fractals", AmadeFractalsApplication.APP.configName());
        assertEquals("amade-fractals", AmadeFractalsApplication.APP.id());
        assertEquals("dev", AmadeFractalsApplication.version());
    }

    @Test
    void workersDoNotKeepTheProgramAlive() {
        Thread worker = FractalsModule.workerThreads().newThread(() -> { });
        assertTrue(worker.isDaemon());
        assertTrue(worker.getName().startsWith("fractal-worker-"));
        assertTrue(worker.getPriority() < Thread.NORM_PRIORITY);
    }
}

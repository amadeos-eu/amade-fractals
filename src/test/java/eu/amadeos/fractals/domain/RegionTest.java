package eu.amadeos.fractals.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTest {
    private static final int W = 400;
    private static final int H = 300;
    private final Region region = Region.of(-0.5, 0, 3);

    @Test
    void mapsPixelsToThePlaneWithTheImaginaryAxisUp() {
        assertEquals(0.01, region.pixelSize(H), 1e-15);
        assertEquals(-2.0, region.offsetRe(0, W, H), 1e-12);
        assertEquals(1.5, region.offsetIm(0, W, H), 1e-12);
        assertEquals(-1.5, region.offsetIm(H, W, H), 1e-12);
        BigDecimal[] corner = region.pointAt(W, 0, W, H);
        assertEquals(1.5, corner[0].doubleValue(), 1e-12);
        assertEquals(1.5, corner[1].doubleValue(), 1e-12);
    }

    @Test
    void zoomKeepsThePointUnderThePointer() {
        Region zoomed = region.zoomAt(100, 50, W, H, 4);
        assertEquals(0.75, zoomed.size(), 1e-15);
        BigDecimal[] before = region.pointAt(100, 50, W, H);
        BigDecimal[] after = zoomed.pointAt(100, 50, W, H);
        assertEquals(before[0].doubleValue(), after[0].doubleValue(), 1e-12);
        assertEquals(before[1].doubleValue(), after[1].doubleValue(), 1e-12);
    }

    @Test
    void centerAtMovesThePointToTheMiddle() {
        Region centred = region.centerAt(300, 75, W, H, 2);
        assertEquals(0.5, centred.reDouble(), 1e-12);
        assertEquals(0.75, centred.imDouble(), 1e-12);
        assertEquals(1.5, centred.size(), 1e-15);
    }

    @Test
    void frameFillsTheWindow() {
        // A frame of half the window's width and height around the right half's centre.
        Region framed = region.frame(200, 75, 200, 150, W, H);
        assertEquals(1.5, framed.size(), 1e-12);
        assertEquals(0.5, framed.reDouble(), 1e-12);
        assertEquals(0, framed.imDouble(), 1e-12);
        // A narrow frame: its height decides, so that all of it stays visible.
        Region tall = region.frame(0, 0, 40, 150, W, H);
        assertEquals(1.5, tall.size(), 1e-12);
    }

    @Test
    void panMovesTheContentWithThePointer() {
        Region moved = region.pan(100, -30, H);
        // Dragging right brings what was left into view: the centre moves left.
        assertEquals(-1.5, moved.reDouble(), 1e-12);
        assertEquals(-0.3, moved.imDouble(), 1e-12);
        assertEquals(region.size(), moved.size());
    }

    @Test
    void keepsTheDigitsADeepZoomNeeds() {
        Region deep = Region.parse("-0.743643887037158704752191506114774", "0.131825904205311970493132056385139", 1e-25);
        Region zoomed = deep.zoomAt(W / 2.0, H / 2.0, W, H, 10);
        assertEquals(deep.re(), zoomed.re());
        Region moved = deep.pan(1, 0, H);
        BigDecimal step = deep.re().subtract(moved.re());
        assertEquals(1e-25 / H, step.doubleValue(), 1e-30);
        assertTrue(moved.re().scale() <= deep.decimals() + 4, () -> "digits " + moved.re().scale());
        assertTrue(deep.decimals() >= 25);
    }

    @Test
    void sizeStaysInRange() {
        assertEquals(Region.MIN_SIZE, region.withSize(1e-320).size());
        assertEquals(Region.MAX_SIZE, region.zoom(1e-9).size());
        assertThrows(IllegalArgumentException.class, () -> Region.of(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Region.of(0, 0, Double.NaN));
    }

    @Test
    void magnificationIsRelativeToHome() {
        Region home = Formula.MANDELBROT.home(false);
        assertEquals(1, home.magnification(home), 1e-12);
        assertEquals(1000, home.zoom(1000).magnification(home), 1e-9);
    }
}

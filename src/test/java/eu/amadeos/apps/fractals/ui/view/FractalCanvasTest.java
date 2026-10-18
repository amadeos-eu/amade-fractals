package eu.amadeos.apps.fractals.ui.view;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FractalCanvasTest {
    @Test
    void framesEndAtThePointer() {
        // 400 × 300 window: dragging 100 to the right and 10 down gives a 100 × 10 frame.
        assertEquals(new Rectangle(50, 40, 100, 10), FractalCanvas.frameFor(50, 40, 150, 50, 400, 300));
        // Dragging mostly down does not widen the frame past the pointer.
        assertEquals(new Rectangle(50, 40, 20, 200), FractalCanvas.frameFor(50, 40, 70, 240, 400, 300));
        // Dragging up and left grows the frame towards the top left.
        assertEquals(new Rectangle(45, 25, 5, 30), FractalCanvas.frameFor(50, 55, 45, 25, 400, 300));
        // Dragging out of the window stops the frame at its edge.
        assertEquals(new Rectangle(350, 0, 50, 40), FractalCanvas.frameFor(350, 40, 500, -20, 400, 300));
    }

    @Test
    void zoomTransformsMoveThePointToItsNewPlace() {
        AffineTransform t = FractalCanvas.scaleAbout(100, 50, 2, 200, 150);
        Point2D moved = t.transform(new Point2D.Double(100, 50), null);
        assertEquals(200, moved.getX(), 1e-9);
        assertEquals(150, moved.getY(), 1e-9);
        Point2D other = t.transform(new Point2D.Double(110, 50), null);
        assertEquals(220, other.getX(), 1e-9);
    }

    @Test
    void notesAndProgressOverThePicture() throws Exception {
        FractalCanvas canvas = new FractalCanvas();
        canvas.setSize(300, 200);
        BufferedImage grey = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics g = grey.getGraphics();
        g.setColor(new java.awt.Color(0x808080));
        g.fillRect(0, 0, 300, 200);
        g.dispose();
        canvas.setPicture(grey);
        assertEquals("", canvas.noteText());
        assertTrue(Double.isNaN(canvas.progressValue()));
        canvas.showNote("Full screen", 50);
        assertEquals("Full screen", canvas.noteText());
        BufferedImage shown = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        canvas.setProgress(0.5);
        canvas.paint(shown.getGraphics());
        // The note darkens the middle at the top; the progress line is light up to the half, dark after it.
        assertTrue((shown.getRGB(150, 30) & 0xFF) < 0x60, "note");
        assertEquals(0x80, shown.getRGB(150, 100) & 0xFF);
        assertTrue((shown.getRGB(100, 198) & 0xFF) > 0xC0, "progress");
        assertTrue((shown.getRGB(250, 198) & 0xFF) < 0x60, "rest of the line");
        canvas.setProgress(Double.NaN);
        assertTrue(Double.isNaN(canvas.progressValue()));
        canvas.setProgress(2);
        assertEquals(1, canvas.progressValue(), 0);
        // The note goes by itself.
        long end = System.currentTimeMillis() + 5000;
        while (!canvas.noteText().isEmpty() && System.currentTimeMillis() < end) {
            javax.swing.SwingUtilities.invokeAndWait(() -> { });
            Thread.sleep(20);
        }
        assertEquals("", canvas.noteText());
    }

    @Test
    void aNewPictureEndsThePendingTransform() {
        FractalCanvas canvas = new FractalCanvas();
        canvas.setSize(400, 300);
        canvas.setListener(new FractalCanvas.Listener() {
            @Override
            public boolean centerAt(double x, double y, double factor) {
                return true;
            }

            @Override
            public boolean zoomAt(double x, double y, double factor) {
                return true;
            }

            @Override
            public boolean frame(double x, double y, double width, double height) {
                return true;
            }

            @Override
            public boolean pan(double dx, double dy) {
                return true;
            }

            @Override
            public boolean pickJuliaSeed(double x, double y) {
                return true;
            }

            @Override
            public void pointerAt(double x, double y) {
            }

            @Override
            public void pointerLeft() {
            }
        });
        BufferedImage picture = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        canvas.setPicture(picture);
        assertTrue(canvas.pendingTransform().isIdentity());
        canvas.dispatchEvent(new java.awt.event.MouseWheelEvent(canvas, java.awt.event.MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(), 0, 100, 100, 0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1, -1));
        AffineTransform pending = canvas.pendingTransform();
        assertEquals(FractalCanvas.WHEEL_FACTOR, pending.getScaleX(), 1e-9);
        canvas.setPicture(picture);
        assertTrue(canvas.pendingTransform().isIdentity());
    }
}

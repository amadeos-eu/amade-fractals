package org.datazuul.amadeos.fractals.ui.view;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FractalCanvasTest {
    @Test
    void framesKeepTheWindowsShape() {
        // 400 × 300 window: dragging 100 to the right and 10 down gives a 100 × 75 frame.
        assertEquals(new Rectangle(50, 40, 100, 75), FractalCanvas.frameFor(50, 40, 150, 50, 400, 300));
        // Dragging up and left grows the frame towards the top left.
        assertEquals(new Rectangle(10, 25, 40, 30), FractalCanvas.frameFor(50, 55, 45, 25, 400, 300));
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

package org.datazuul.amadeos.fractals.ui.view;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Shows the fractal and turns mouse gestures into navigation:
 * <ul>
 *     <li>click: zoom in 2× at the point, right click: zoom out</li>
 *     <li>drag: a frame to fill the window with</li>
 *     <li>mouse wheel: zoom at the pointer</li>
 *     <li>right or middle button dragged: move the picture</li>
 *     <li>Ctrl+click: the Julia set of the point</li>
 * </ul>
 * Until the new picture is calculated, the old one is shown moved and scaled as the gesture asked, so that
 * every gesture answers at once.
 */
public final class FractalCanvas extends JComponent {
    /** Mouse wheel zoom per notch. */
    public static final double WHEEL_FACTOR = 1.25;
    /** Movement below this many pixels is a click, not a drag. */
    static final int DRAG_THRESHOLD = 4;

    /** What the gestures ask for; each returns false if it cannot be done, and the picture stays. */
    public interface Listener {
        boolean centerAt(double x, double y, double factor);

        boolean zoomAt(double x, double y, double factor);

        boolean frame(double x, double y, double width, double height);

        boolean pan(double dx, double dy);

        boolean pickJuliaSeed(double x, double y);

        void pointerAt(double x, double y);

        void pointerLeft();
    }

    private static final Listener NONE = new Listener() {
        @Override
        public boolean centerAt(double x, double y, double factor) {
            return false;
        }

        @Override
        public boolean zoomAt(double x, double y, double factor) {
            return false;
        }

        @Override
        public boolean frame(double x, double y, double width, double height) {
            return false;
        }

        @Override
        public boolean pan(double dx, double dy) {
            return false;
        }

        @Override
        public boolean pickJuliaSeed(double x, double y) {
            return false;
        }

        @Override
        public void pointerAt(double x, double y) {
        }

        @Override
        public void pointerLeft() {
        }
    };

    private Listener listener = NONE;
    private BufferedImage picture;
    /** Applied to the picture until a new one arrives; the identity when nothing is pending. */
    private AffineTransform pending = new AffineTransform();
    private String placeholder = "";
    /** A short message over the picture, e.g. how to leave full screen; empty when none is shown. */
    private String note = "";
    private final Timer noteTimer = new Timer(0, e -> hideNote());
    /** Progress line along the bottom edge, 0 to 1; NaN when hidden. Used in full screen, without status bar. */
    private double progress = Double.NaN;

    private int pressX;
    private int pressY;
    private int pressButton;
    private boolean dragging;
    private Rectangle frame;
    private int panX;
    private int panY;

    public FractalCanvas() {
        setOpaque(true);
        setFocusable(true);
        setPreferredSize(new Dimension(800, 600));
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                pressX = e.getX();
                pressY = e.getY();
                pressButton = e.getButton();
                dragging = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - pressX;
                int dy = e.getY() - pressY;
                if (!dragging && Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) return;
                dragging = true;
                if (pressButton == MouseEvent.BUTTON1) {
                    frame = frameFor(pressX, pressY, e.getX(), e.getY(), getWidth(), getHeight());
                } else {
                    panX = dx;
                    panY = dy;
                }
                listener.pointerAt(e.getX(), e.getY());
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging) {
                    if (frame != null) {
                        Rectangle r = frame;
                        frame = null;
                        if (listener.frame(r.x, r.y, r.width, r.height)) {
                            double scale = Math.min(getWidth() / (double) r.width, getHeight() / (double) r.height);
                            transform(scaleAbout(r.getCenterX(), r.getCenterY(), scale, getWidth() / 2.0,
                                    getHeight() / 2.0));
                        }
                    } else if (panX != 0 || panY != 0) {
                        int dx = panX;
                        int dy = panY;
                        panX = 0;
                        panY = 0;
                        if (listener.pan(dx, dy)) transform(AffineTransform.getTranslateInstance(dx, dy));
                    }
                    dragging = false;
                    repaint();
                    return;
                }
                click(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                listener.pointerAt(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                listener.pointerLeft();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double notches = e.getPreciseWheelRotation();
                if (notches == 0) return;
                double factor = Math.pow(WHEEL_FACTOR, -notches);
                if (listener.zoomAt(e.getX(), e.getY(), factor)) {
                    transform(scaleAbout(e.getX(), e.getY(), factor, e.getX(), e.getY()));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    private void click(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
        if (pressButton == MouseEvent.BUTTON1 && ctrl) {
            if (listener.pickJuliaSeed(x, y)) repaint();
        } else if (pressButton == MouseEvent.BUTTON1) {
            if (listener.centerAt(x, y, 2)) transform(scaleAbout(x, y, 2, getWidth() / 2.0, getHeight() / 2.0));
        } else if (pressButton == MouseEvent.BUTTON3 || pressButton == MouseEvent.BUTTON2) {
            if (listener.centerAt(x, y, 0.5)) transform(scaleAbout(x, y, 0.5, getWidth() / 2.0, getHeight() / 2.0));
        }
    }

    public void setListener(Listener newListener) {
        listener = newListener == null ? NONE : newListener;
    }

    /** Text shown while there is no picture yet. */
    public void setPlaceholder(String text) {
        placeholder = text == null ? "" : text;
        repaint();
    }

    /** Shows a message over the top of the picture for a while. */
    public void showNote(String text, int millis) {
        note = text == null ? "" : text;
        noteTimer.stop();
        noteTimer.setInitialDelay(millis);
        noteTimer.setRepeats(false);
        if (!note.isEmpty()) noteTimer.start();
        repaint();
    }

    public void hideNote() {
        noteTimer.stop();
        note = "";
        repaint();
    }

    /** The message shown, or empty. */
    public String noteText() {
        return note;
    }

    /** Shows a thin progress line along the bottom edge (0 to 1), or hides it (NaN). */
    public void setProgress(double fraction) {
        progress = Double.isNaN(fraction) ? Double.NaN : Math.max(0, Math.min(1, fraction));
        repaint();
    }

    /** The progress line, or NaN when hidden. */
    public double progressValue() {
        return progress;
    }

    /** Shows a new picture, scaled to the window if smaller (a preview); a pending transform ends. */
    public void setPicture(BufferedImage newPicture) {
        picture = newPicture;
        pending = new AffineTransform();
        repaint();
    }

    public BufferedImage picture() {
        return picture;
    }

    /** The frame being dragged, for tests. */
    Rectangle frame() {
        return frame;
    }

    /** The transform shown until the next picture, for tests. */
    AffineTransform pendingTransform() {
        return new AffineTransform(pending);
    }

    private void transform(AffineTransform step) {
        AffineTransform combined = new AffineTransform(step);
        combined.concatenate(pending);
        pending = combined;
        repaint();
    }

    /** Moves point (x, y) to (toX, toY) and scales by {@code factor} around it. */
    static AffineTransform scaleAbout(double x, double y, double factor, double toX, double toY) {
        AffineTransform t = AffineTransform.getTranslateInstance(toX, toY);
        t.scale(factor, factor);
        t.translate(-x, -y);
        return t;
    }

    /**
     * The frame between the press point and the pointer, in any shape: zooming shows all of it, with more of the
     * picture on its shorter side.
     */
    static Rectangle frameFor(int x0, int y0, int x1, int y1, int width, int height) {
        int left = Math.max(0, Math.min(x0, x1));
        int top = Math.max(0, Math.min(y0, y1));
        int right = Math.min(width, Math.max(x0, x1));
        int bottom = Math.min(height, Math.max(y0, y1));
        return new Rectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (picture == null) {
                paintPlaceholder(g2);
                paintOverlay(g2);
                return;
            }
            AffineTransform t = new AffineTransform(pending);
            if (dragging && (panX != 0 || panY != 0)) t.preConcatenate(AffineTransform.getTranslateInstance(panX, panY));
            if (picture.getWidth() != getWidth() || picture.getHeight() != getHeight()) {
                // A coarse preview: stretched over the window, smoothly.
                t.scale(getWidth() / (double) picture.getWidth(), getHeight() / (double) picture.getHeight());
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            } else if (!t.isIdentity()) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
            g2.drawImage(picture, t, null);
            if (frame != null) paintFrame(g2, frame);
            paintOverlay(g2);
        } finally {
            g2.dispose();
        }
    }

    private static void paintFrame(Graphics2D g, Rectangle r) {
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(r);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.BLACK);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0));
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    }

    private void paintOverlay(Graphics2D g) {
        if (!Double.isNaN(progress)) {
            int h = 4;
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, getHeight() - h, getWidth(), h);
            g.setColor(new Color(255, 255, 255, 200));
            g.fillRect(0, getHeight() - h, (int) Math.round(getWidth() * progress), h);
        }
        if (note.isEmpty()) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(note) + 32;
        int h = fm.getHeight() + 16;
        int x = (getWidth() - w) / 2;
        int y = 24;
        g.setColor(new Color(0, 0, 0, 170));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));
        g.setColor(Color.WHITE);
        g.drawString(note, x + 16, y + 8 + fm.getAscent());
    }

    private void paintPlaceholder(Graphics2D g) {
        if (placeholder.isEmpty()) return;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(placeholder, (getWidth() - fm.stringWidth(placeholder)) / 2, getHeight() / 2);
    }
}

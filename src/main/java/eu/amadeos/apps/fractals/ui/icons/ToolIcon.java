package eu.amadeos.apps.fractals.ui.icons;

import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.theme.DesktopTheme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Toolbar icons drawn in Java2D on a 16 × 16 grid, as in amade-writepad and amade-imagescan: no icon theme
 * needed, any size, and the theme's colours (lines in the button's text colour, paper in the field colour,
 * highlights in the selection colour).
 */
public final class ToolIcon implements Icon {
    /** Size in toolbars at 100 % scaling. */
    public static final int SIZE = 22;

    public enum Glyph {
        OPEN, SAVE, PICTURE, EXPORT, COPY,
        BACK, FORWARD, HOME, ZOOM_IN, ZOOM_OUT,
        JULIA, CYCLE, REFRESH, STOP, FULL_SCREEN
    }

    private final Glyph glyph;
    private final int size;

    public ToolIcon(Glyph glyph) {
        this(glyph, SIZE);
    }

    public ToolIcon(Glyph glyph, int size) {
        this.glyph = Objects.requireNonNull(glyph, "glyph");
        this.size = size;
    }

    public Glyph glyph() {
        return glyph;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        DesktopTheme theme = AmadeUI.theme();
        boolean enabled = c == null || c.isEnabled();
        Color ink = !enabled ? theme.textInactive()
                : c != null && c.getForeground() != null ? c.getForeground() : theme.textActive();
        Color paper = theme.fieldBackground();
        Color light = enabled ? theme.selectionBackground() : theme.textInactive();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 16.0, size / 16.0);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            new Painter(g2, ink, paper, light).paint(glyph);
        } finally {
            g2.dispose();
        }
    }

    /** Draws one glyph on the 16-unit grid; lines on half units so they are crisp at 100 %. */
    private record Painter(Graphics2D g, Color ink, Color paper, Color light) {
        void paint(Glyph glyph) {
            switch (glyph) {
                case OPEN -> open();
                case SAVE -> save();
                case PICTURE -> picture(1.5, 2.5, 13, 11);
                case EXPORT -> export();
                case COPY -> {
                    page(1.5, 1.5, 8, 10);
                    page(6.5, 4.5, 8, 10);
                }
                case BACK -> arrow(false);
                case FORWARD -> arrow(true);
                case HOME -> home();
                case ZOOM_IN -> {
                    magnifier(6.5, 6.5, 5);
                    line(4, 6.5, 9, 6.5);
                    line(6.5, 4, 6.5, 9);
                }
                case ZOOM_OUT -> {
                    magnifier(6.5, 6.5, 5);
                    line(4, 6.5, 9, 6.5);
                }
                case JULIA -> julia();
                case CYCLE -> cycle();
                case REFRESH -> refresh();
                case STOP -> stop();
                case FULL_SCREEN -> fullScreen();
            }
        }

        private void fillAndDraw(Shape shape, Color fill) {
            g.setColor(fill);
            g.fill(shape);
            g.setColor(ink);
            g.draw(shape);
        }

        private void line(double x1, double y1, double x2, double y2) {
            g.setColor(ink);
            g.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        private void page(double x, double y, double w, double h) {
            double fold = Math.min(3, w / 3);
            Path2D sheet = new Path2D.Double();
            sheet.moveTo(x, y);
            sheet.lineTo(x + w - fold, y);
            sheet.lineTo(x + w, y + fold);
            sheet.lineTo(x + w, y + h);
            sheet.lineTo(x, y + h);
            sheet.closePath();
            fillAndDraw(sheet, paper);
            Path2D corner = new Path2D.Double();
            corner.moveTo(x + w - fold, y);
            corner.lineTo(x + w - fold, y + fold);
            corner.lineTo(x + w, y + fold);
            g.setColor(ink);
            g.draw(corner);
        }

        private void open() {
            Path2D back = new Path2D.Double();
            back.moveTo(1.5, 13.5);
            back.lineTo(1.5, 3.5);
            back.lineTo(5.5, 3.5);
            back.lineTo(7, 5.5);
            back.lineTo(12.5, 5.5);
            back.lineTo(12.5, 8.5);
            fillAndDraw(back, paper);
            Path2D front = new Path2D.Double();
            front.moveTo(1.5, 13.5);
            front.lineTo(4, 8.5);
            front.lineTo(15, 8.5);
            front.lineTo(12.5, 13.5);
            front.closePath();
            fillAndDraw(front, ink);
        }

        private void save() {
            Path2D disk = new Path2D.Double();
            disk.moveTo(1.5, 1.5);
            disk.lineTo(12.5, 1.5);
            disk.lineTo(14.5, 3.5);
            disk.lineTo(14.5, 14.5);
            disk.lineTo(1.5, 14.5);
            disk.closePath();
            fillAndDraw(disk, ink);
            g.setColor(paper);
            g.fill(new Rectangle2D.Double(4, 2, 7, 4));
            g.fill(new Rectangle2D.Double(3.5, 8.5, 9, 6));
            g.setColor(ink);
            g.fill(new Rectangle2D.Double(8, 2.5, 2, 3));
            line(5, 10.5, 11, 10.5);
            line(5, 12.5, 11, 12.5);
        }

        /** A framed picture: sun and mountains. */
        private void picture(double x, double y, double w, double h) {
            fillAndDraw(new Rectangle2D.Double(x, y, w, h), paper);
            g.setColor(light);
            g.fill(new Ellipse2D.Double(x + w - 5, y + 1.5, 3, 3));
            Path2D hills = new Path2D.Double();
            hills.moveTo(x + 0.5, y + h - 0.5);
            hills.lineTo(x + w * 0.35, y + h * 0.4);
            hills.lineTo(x + w * 0.55, y + h * 0.7);
            hills.lineTo(x + w * 0.72, y + h * 0.5);
            hills.lineTo(x + w - 0.5, y + h - 0.5);
            hills.closePath();
            g.setColor(ink);
            g.fill(hills);
        }

        /** A small picture growing into a large frame. */
        private void export() {
            g.setColor(ink);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{1.5f, 1.5f}, 0));
            g.draw(new Rectangle2D.Double(1.5, 1.5, 13, 13));
            g.setStroke(new BasicStroke(1f));
            picture(1.5, 7.5, 7, 7);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            line(9.5, 6.5, 13, 3);
            g.setStroke(new BasicStroke(1f));
            Path2D head = new Path2D.Double();
            head.moveTo(14.5, 1.5);
            head.lineTo(14.5, 6.5);
            head.lineTo(9.5, 1.5);
            head.closePath();
            g.fill(head);
        }

        /** Back points left, forward right. */
        private void arrow(boolean right) {
            if (right) {
                g.translate(16, 0);
                g.scale(-1, 1);
            }
            Path2D shape = new Path2D.Double();
            shape.moveTo(1.5, 8);
            shape.lineTo(7.5, 2);
            shape.lineTo(7.5, 5.5);
            shape.lineTo(14.5, 5.5);
            shape.lineTo(14.5, 10.5);
            shape.lineTo(7.5, 10.5);
            shape.lineTo(7.5, 14);
            shape.closePath();
            fillAndDraw(shape, paper);
        }

        private void home() {
            Path2D house = new Path2D.Double();
            house.moveTo(1, 8.5);
            house.lineTo(8, 1.5);
            house.lineTo(15, 8.5);
            house.lineTo(13.5, 8.5);
            house.lineTo(13.5, 14.5);
            house.lineTo(2.5, 14.5);
            house.lineTo(2.5, 8.5);
            house.closePath();
            fillAndDraw(house, paper);
            fillAndDraw(new Rectangle2D.Double(6.5, 9.5, 3, 5), ink);
        }

        private void magnifier(double cx, double cy, double r) {
            fillAndDraw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r), paper);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double edge = r * Math.sqrt(0.5);
            line(cx + edge, cy + edge, Math.min(15, cx + r + 3.5), Math.min(15, cy + r + 3.5));
            g.setStroke(new BasicStroke(1f));
        }

        /** The Mandelbrot silhouette: cardioid and bulb, with the seed of a Julia set. */
        private void julia() {
            Path2D cardioid = new Path2D.Double();
            double a = 2.6;
            for (int i = 0; i <= 64; i++) {
                double t = 2 * Math.PI * i / 64;
                double r = a * (1 - Math.cos(t));
                double px = 14 + r * Math.cos(t);
                double py = 9 + r * Math.sin(t);
                if (i == 0) cardioid.moveTo(px, py);
                else cardioid.lineTo(px, py);
            }
            cardioid.closePath();
            g.setColor(ink);
            g.fill(cardioid);
            g.fill(new Ellipse2D.Double(5.2, 7.4, 3.2, 3.2));
            g.fill(new Ellipse2D.Double(3.6, 8.4, 1.2, 1.2));
            g.setColor(light);
            g.fill(new Ellipse2D.Double(1, 1, 4.5, 4.5));
            g.setColor(ink);
            g.draw(new Ellipse2D.Double(1, 1, 4.5, 4.5));
        }

        /** A colour wheel with an arrow around it. */
        private void cycle() {
            Color[] quarters = {light, paper, ink, paper};
            for (int i = 0; i < 4; i++) {
                g.setColor(quarters[i]);
                g.fill(new Arc2D.Double(4, 4, 8, 8, i * 90, 90, Arc2D.PIE));
            }
            g.setColor(ink);
            g.draw(new Ellipse2D.Double(4, 4, 8, 8));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(1.5, 1.5, 13, 13, 100, 250, Arc2D.OPEN));
            g.setStroke(new BasicStroke(1f));
            Path2D head = new Path2D.Double();
            head.moveTo(5, 0.5);
            head.lineTo(9, 2.2);
            head.lineTo(5.5, 4.8);
            head.closePath();
            g.fill(head);
        }

        private void refresh() {
            g.setColor(ink);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 60, 270, Arc2D.OPEN));
            g.setStroke(new BasicStroke(1f));
            Path2D head = new Path2D.Double();
            head.moveTo(10, 0.8);
            head.lineTo(14.8, 3.6);
            head.lineTo(10, 6.6);
            head.closePath();
            g.fill(head);
        }

        /** A screen with arrows to its four corners. */
        private void fullScreen() {
            fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 13, 11), paper);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            double[][] corners = {{4, 5, 1, 1}, {12, 5, -1, 1}, {4, 11, 1, -1}, {12, 11, -1, -1}};
            for (double[] c : corners) {
                line(c[0], c[1], c[0] + 2.5 * c[2], c[1]);
                line(c[0], c[1], c[0], c[1] + 2 * c[3]);
                line(c[0], c[1], c[0] + 2 * c[2], c[1] + 1.6 * c[3]);
            }
            g.setStroke(new BasicStroke(1f));
        }

        private void stop() {
            Path2D octagon = new Path2D.Double();
            for (int i = 0; i < 8; i++) {
                double t = Math.PI / 8 + i * Math.PI / 4;
                double px = 8 + 6.8 * Math.cos(t);
                double py = 8 + 6.8 * Math.sin(t);
                if (i == 0) octagon.moveTo(px, py);
                else octagon.lineTo(px, py);
            }
            octagon.closePath();
            fillAndDraw(octagon, paper);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            line(5.5, 5.5, 10.5, 10.5);
            line(10.5, 5.5, 5.5, 10.5);
            g.setStroke(new BasicStroke(1f));
        }
    }
}

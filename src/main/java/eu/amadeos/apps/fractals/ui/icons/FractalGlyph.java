package eu.amadeos.apps.fractals.ui.icons;

import eu.amadeos.commons.swing.StandardGlyph;
import eu.amadeos.commons.swing.ToolIcon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The toolbar glyphs only Fractals needs; open, save, copy, zoom and refresh come from
 * {@link StandardGlyph}. Highlights (the sun, the Julia seed, a quarter of the colour wheel) are in the
 * theme's selection colour.
 */
public enum FractalGlyph implements ToolIcon.Glyph {
    PICTURE, EXPORT, BACK, FORWARD, HOME, JULIA, CYCLE, STOP, FULL_SCREEN;

    @Override
    public void paint(ToolIcon.Pen pen) {
        switch (this) {
            case PICTURE -> picture(pen, 1.5, 2.5, 13, 11);
            case EXPORT -> export(pen);
            case BACK -> arrow(pen);
            case FORWARD -> {
                pen.mirror();
                arrow(pen);
            }
            case HOME -> home(pen);
            case JULIA -> julia(pen);
            case CYCLE -> cycle(pen);
            case STOP -> stop(pen);
            case FULL_SCREEN -> fullScreen(pen);
        }
    }

    /** A framed picture: sun and mountains. */
    private static void picture(ToolIcon.Pen pen, double x, double y, double w, double h) {
        pen.fillAndDraw(new Rectangle2D.Double(x, y, w, h), pen.paper());
        pen.fill(new Ellipse2D.Double(x + w - 5, y + 1.5, 3, 3), pen.light());
        Path2D hills = new Path2D.Double();
        hills.moveTo(x + 0.5, y + h - 0.5);
        hills.lineTo(x + w * 0.35, y + h * 0.4);
        hills.lineTo(x + w * 0.55, y + h * 0.7);
        hills.lineTo(x + w * 0.72, y + h * 0.5);
        hills.lineTo(x + w - 0.5, y + h - 0.5);
        hills.closePath();
        pen.fill(hills, pen.ink());
    }

    /** A small picture growing into a large frame. */
    private static void export(ToolIcon.Pen pen) {
        pen.g().setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[]{1.5f, 1.5f}, 0));
        pen.draw(new Rectangle2D.Double(1.5, 1.5, 13, 13));
        pen.thin();
        picture(pen, 1.5, 7.5, 7, 7);
        pen.bold(1.6f);
        pen.line(9.5, 6.5, 13, 3);
        pen.thin();
        pen.triangle(14.5, 1.5, 14.5, 6.5, 9.5, 1.5);
    }

    /** Back points left; forward is its mirror image. */
    private static void arrow(ToolIcon.Pen pen) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(1.5, 8);
        shape.lineTo(7.5, 2);
        shape.lineTo(7.5, 5.5);
        shape.lineTo(14.5, 5.5);
        shape.lineTo(14.5, 10.5);
        shape.lineTo(7.5, 10.5);
        shape.lineTo(7.5, 14);
        shape.closePath();
        pen.fillAndDraw(shape, pen.paper());
    }

    private static void home(ToolIcon.Pen pen) {
        Path2D house = new Path2D.Double();
        house.moveTo(1, 8.5);
        house.lineTo(8, 1.5);
        house.lineTo(15, 8.5);
        house.lineTo(13.5, 8.5);
        house.lineTo(13.5, 14.5);
        house.lineTo(2.5, 14.5);
        house.lineTo(2.5, 8.5);
        house.closePath();
        pen.fillAndDraw(house, pen.paper());
        pen.fillAndDraw(new Rectangle2D.Double(6.5, 9.5, 3, 5), pen.ink());
    }

    // --8<-- [start:julia]
    /** The Mandelbrot silhouette: cardioid and bulb, with the seed of a Julia set. */
    private static void julia(ToolIcon.Pen pen) {
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
        pen.fill(cardioid, pen.ink());
        pen.fill(new Ellipse2D.Double(5.2, 7.4, 3.2, 3.2), pen.ink());
        pen.fill(new Ellipse2D.Double(3.6, 8.4, 1.2, 1.2), pen.ink());
        pen.fillAndDraw(new Ellipse2D.Double(1, 1, 4.5, 4.5), pen.light());
    }
    // --8<-- [end:julia]

    /** A colour wheel with an arrow around it. */
    private static void cycle(ToolIcon.Pen pen) {
        Color[] quarters = {pen.light(), pen.paper(), pen.ink(), pen.paper()};
        for (int i = 0; i < 4; i++) {
            pen.fill(new Arc2D.Double(4, 4, 8, 8, i * 90, 90, Arc2D.PIE), quarters[i]);
        }
        pen.draw(new Ellipse2D.Double(4, 4, 8, 8));
        pen.bold(1.4f);
        pen.draw(new Arc2D.Double(1.5, 1.5, 13, 13, 100, 250, Arc2D.OPEN));
        pen.thin();
        pen.triangle(5, 0.5, 9, 2.2, 5.5, 4.8);
    }

    /** A screen with arrows to its four corners. */
    private static void fullScreen(ToolIcon.Pen pen) {
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 13, 11), pen.paper());
        pen.g().setStroke(new BasicStroke(1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        double[][] corners = {{4, 5, 1, 1}, {12, 5, -1, 1}, {4, 11, 1, -1}, {12, 11, -1, -1}};
        for (double[] c : corners) {
            pen.line(c[0], c[1], c[0] + 2.5 * c[2], c[1]);
            pen.line(c[0], c[1], c[0], c[1] + 2 * c[3]);
            pen.line(c[0], c[1], c[0] + 2 * c[2], c[1] + 1.6 * c[3]);
        }
        pen.thin();
    }

    private static void stop(ToolIcon.Pen pen) {
        Path2D octagon = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double t = Math.PI / 8 + i * Math.PI / 4;
            double px = 8 + 6.8 * Math.cos(t);
            double py = 8 + 6.8 * Math.sin(t);
            if (i == 0) octagon.moveTo(px, py);
            else octagon.lineTo(px, py);
        }
        octagon.closePath();
        pen.fillAndDraw(octagon, pen.paper());
        pen.bold(2f);
        pen.line(5.5, 5.5, 10.5, 10.5);
        pen.line(10.5, 5.5, 5.5, 10.5);
        pen.thin();
    }
}

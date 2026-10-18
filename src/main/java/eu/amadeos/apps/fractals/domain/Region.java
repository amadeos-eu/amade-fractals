package eu.amadeos.apps.fractals.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * The part of the complex plane in view: its centre, exact to as many digits as the zoom needs, and its
 * height. The width follows from the shape of the window, so the same region fits any window.
 *
 * <p>Pixel coordinates run from the top left corner; the imaginary axis points up. A position {@code x}
 * between 0 and the width addresses the edge between pixels, {@code x + 0.5} the centre of pixel {@code x}.
 *
 * @param re   real part of the centre
 * @param im   imaginary part of the centre
 * @param size height of the view in the complex plane, greater than 0
 */
public record Region(BigDecimal re, BigDecimal im, double size) {
    /** Smallest height: below this even perturbation in {@code double} runs out of exponent. */
    public static final double MIN_SIZE = 1e-290;
    public static final double MAX_SIZE = 100;

    public Region {
        Objects.requireNonNull(re, "re");
        Objects.requireNonNull(im, "im");
        if (!(size > 0) || Double.isInfinite(size)) throw new IllegalArgumentException("Invalid size: " + size);
    }

    public static Region of(double re, double im, double size) {
        return new Region(new BigDecimal(Double.toString(re)), new BigDecimal(Double.toString(im)), size);
    }

    public static Region parse(String re, String im, double size) {
        return new Region(new BigDecimal(re.strip()), new BigDecimal(im.strip()), size);
    }

    public double reDouble() {
        return re.doubleValue();
    }

    public double imDouble() {
        return im.doubleValue();
    }

    /** Width and height of one pixel in a window this many pixels high. */
    public double pixelSize(int height) {
        return size / Math.max(1, height);
    }

    /** Real distance from the centre to position {@code x} of a window. */
    public double offsetRe(double x, int width, int height) {
        return (x - width / 2.0) * pixelSize(height);
    }

    /** Imaginary distance from the centre to position {@code y} of a window. */
    public double offsetIm(double y, int width, int height) {
        return (height / 2.0 - y) * pixelSize(height);
    }

    /** The point at a position of the window, exactly. */
    public BigDecimal[] pointAt(double x, double y, int width, int height) {
        return new BigDecimal[]{
                round(re.add(new BigDecimal(offsetRe(x, width, height))), size),
                round(im.add(new BigDecimal(offsetIm(y, width, height))), size)};
    }

    /** Zooms by {@code factor} (greater than 1 zooms in) and keeps the point under the pointer in place. */
    public Region zoomAt(double x, double y, int width, int height, double factor) {
        double keep = 1 - 1 / factor;
        return moved(offsetRe(x, width, height) * keep, offsetIm(y, width, height) * keep, size / factor);
    }

    /** Zooms by {@code factor} around the centre. */
    public Region zoom(double factor) {
        return withSize(size / factor);
    }

    /** The point at the position becomes the centre, zoomed by {@code factor}. */
    public Region centerAt(double x, double y, int width, int height, double factor) {
        return moved(offsetRe(x, width, height), offsetIm(y, width, height), size / factor);
    }

    /** Fills the window with a frame drawn in it (top left corner, width and height in pixels). */
    public Region frame(double x, double y, double frameWidth, double frameHeight, int width, int height) {
        double scale = Math.max(frameWidth / Math.max(1, width), frameHeight / Math.max(1, height));
        return moved(offsetRe(x + frameWidth / 2, width, height), offsetIm(y + frameHeight / 2, width, height),
                size * scale);
    }

    /** The picture was dragged by {@code dx, dy} pixels: what was there is now there. */
    public Region pan(double dx, double dy, int height) {
        double pixel = pixelSize(height);
        return moved(-dx * pixel, dy * pixel, size);
    }

    public Region withSize(double newSize) {
        return new Region(re, im, clamp(newSize));
    }

    /** How much larger the view is than the region: home size / size. */
    public double magnification(Region home) {
        return home.size / size;
    }

    /** Significant decimal places worth showing for the centre at this zoom. */
    public int decimals() {
        return decimals(size);
    }

    private Region moved(double dRe, double dIm, double newSize) {
        double s = clamp(newSize);
        return new Region(round(re.add(new BigDecimal(dRe)), s), round(im.add(new BigDecimal(dIm)), s), s);
    }

    private static double clamp(double s) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, s));
    }

    private static int decimals(double size) {
        return Math.max(3, (int) Math.ceil(-Math.log10(size)) + 4);
    }

    /** Enough digits for a picture of up to 10 000 pixels, without the noise of binary fractions. */
    private static BigDecimal round(BigDecimal value, double size) {
        BigDecimal rounded = value.setScale(decimals(size) + 4, RoundingMode.HALF_EVEN).stripTrailingZeros();
        return rounded.signum() == 0 ? BigDecimal.ZERO : rounded;
    }
}

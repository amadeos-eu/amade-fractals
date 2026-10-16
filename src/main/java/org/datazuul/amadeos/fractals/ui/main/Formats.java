package org.datazuul.amadeos.fractals.ui.main;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/** Numbers as the window shows them and reads them back. */
public final class Formats {
    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    private Formats() {
    }

    /** A magnification: "250" or "1.5 × 10⁶" in the locale's decimal sign. */
    public static String magnification(double value, Locale locale) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        if (value >= 0.01 && value < 100_000) {
            DecimalFormat plain = new DecimalFormat(value < 10 ? "0.##" : "0", symbols);
            return plain.format(value);
        }
        int exponent = (int) Math.floor(Math.log10(value));
        double mantissa = value / Math.pow(10, exponent);
        if (mantissa >= 9.95) {
            mantissa /= 10;
            exponent++;
        }
        return new DecimalFormat("0.#", symbols).format(mantissa) + " × 10" + superscript(exponent);
    }

    /** Reads a magnification as typed: "1e6", "1000000", "1.5 × 10⁶", "1,5e6"; empty if it is not one. */
    public static Optional<Double> parseMagnification(String text) {
        String t = text.strip().replace(' ', ' ').replace(" ", "").replace('×', 'x').replace('*', 'x');
        int power = t.indexOf("x10");
        if (power >= 0) {
            String exponent = normalSuperscripts(t.substring(power + 3)).replace("^", "");
            t = t.substring(0, power) + "e" + exponent;
        }
        t = t.replace(',', '.');
        try {
            double value = Double.parseDouble(t);
            return value > 0 && Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** A coordinate with a fixed number of decimal places, never in scientific notation. */
    public static String coordinate(BigDecimal value, int decimals) {
        BigDecimal rounded = value.setScale(decimals, RoundingMode.HALF_EVEN);
        if (rounded.signum() == 0) rounded = rounded.abs();
        return rounded.toPlainString();
    }

    /** Reads a coordinate; a decimal comma is accepted. */
    public static Optional<BigDecimal> parseCoordinate(String text) {
        String t = text.strip().replace('−', '-').replace(',', '.');
        try {
            return Optional.of(new BigDecimal(t));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** A Julia seed component, short. */
    public static String seed(double value) {
        return new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }

    /** Reads a plain number; a decimal comma is accepted. */
    public static Optional<Double> parseNumber(String text) {
        try {
            double value = Double.parseDouble(text.strip().replace('−', '-').replace(',', '.'));
            return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static String superscript(int value) {
        StringBuilder text = new StringBuilder();
        if (value < 0) text.append('⁻');
        for (char c : String.valueOf(Math.abs(value)).toCharArray()) text.append(SUPERSCRIPTS.charAt(c - '0'));
        return text.toString();
    }

    private static String normalSuperscripts(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            int digit = SUPERSCRIPTS.indexOf(c);
            if (digit >= 0) out.append((char) ('0' + digit));
            else if (c == '⁻') out.append('-');
            else out.append(c);
        }
        return out.toString();
    }
}

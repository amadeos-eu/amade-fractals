package org.datazuul.amadeos.fractals.ui.main;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatsTest {
    @Test
    void magnificationReadsAsItIsWritten() {
        assertEquals("1", Formats.magnification(1, Locale.ENGLISH));
        assertEquals("2.5", Formats.magnification(2.5, Locale.ENGLISH));
        assertEquals("2,5", Formats.magnification(2.5, Locale.GERMAN));
        assertEquals("12345", Formats.magnification(12345, Locale.ENGLISH));
        assertEquals("1.5 × 10⁶", Formats.magnification(1.5e6, Locale.ENGLISH));
        assertEquals("1 × 10²⁰", Formats.magnification(0.999e20, Locale.ENGLISH));
        assertEquals("5 × 10⁻³", Formats.magnification(0.005, Locale.ENGLISH));
        for (double m : new double[]{1, 2.5, 12345, 1.5e6, 3.2e45, 0.005}) {
            assertEquals(m, Formats.parseMagnification(Formats.magnification(m, Locale.GERMAN)).orElseThrow(),
                    m * 0.01);
        }
        assertEquals(Optional.of(1e6), Formats.parseMagnification("1e6"));
        assertEquals(Optional.of(2e10), Formats.parseMagnification(" 2 x 10^10 "));
        assertEquals(Optional.empty(), Formats.parseMagnification("abc"));
        assertEquals(Optional.empty(), Formats.parseMagnification("-5"));
    }

    @Test
    void coordinatesInPlainDecimals() {
        assertEquals("-0.750000", Formats.coordinate(new BigDecimal("-0.75"), 6));
        assertEquals("0.000", Formats.coordinate(new BigDecimal("-0.0000001"), 3));
        assertEquals("0.00000000000000000001", Formats.coordinate(new BigDecimal("1E-20"), 20));
        assertEquals(Optional.of(new BigDecimal("-1.25")), Formats.parseCoordinate("−1,25"));
        assertEquals(Optional.empty(), Formats.parseCoordinate("1.2.3"));
        assertEquals(Optional.of(0.156), Formats.parseNumber("0,156"));
        assertEquals("-0.8", Formats.seed(-0.8));
    }
}

package eu.amadeos.apps.fractals.domain;

/** How a point outside the set gets its place in the palette. */
public enum Coloring {
    /** Continuous iteration count: no bands. */
    SMOOTH,
    /** Whole iteration counts: bands of equal colour, as in the classic pictures. */
    ESCAPE_TIME,
    /** Bands split by the sign of Im z when escaping: a chequered pattern that shows the field lines. */
    BINARY_DECOMPOSITION,
    /** The closest approach of the orbit to 0; also colours the inside. */
    ORBIT_TRAP
}

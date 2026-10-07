package edu.austral.ingsis.printscript.language;

/** Una regla de linting incumplida, con su ubicacion. */
public record Finding(String message, Location start, Location end) {}

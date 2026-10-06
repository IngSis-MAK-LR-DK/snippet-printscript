package edu.austral.ingsis.printscript;

/** Version del lenguaje PrintScript que soporta este modulo. */
public final class Version {

    private static final String CURRENT = "1.1";

    private Version() {}

    public static String current() {
        return CURRENT;
    }
}

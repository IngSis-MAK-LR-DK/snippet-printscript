package edu.austral.ingsis.printscript.web;

import edu.austral.ingsis.printscript.common.Version;

final class Versions {

    private Versions() {}

    /** Version de PrintScript pedida ("1.0" o "1.1"); si no se indica, la mas nueva. */
    static Version parse(String label) {
        return label == null ? Version.V1_1 : Version.fromLabel(label);
    }
}

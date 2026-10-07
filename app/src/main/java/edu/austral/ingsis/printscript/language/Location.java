package edu.austral.ingsis.printscript.language;

import edu.austral.ingsis.printscript.common.Position;

/** Posicion (linea y columna, desde 1) dentro del codigo recibido. */
public record Location(int line, int column) {

    static Location from(Position position) {
        return new Location(position.line(), position.column());
    }
}

package edu.austral.ingsis.printscript.language;

import java.util.List;

/** Resultado de validar un programa: valido, o la lista de errores que encontro. */
public record ValidationResult(boolean valid, List<Violation> errors) {

    static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    static ValidationResult invalid(List<Violation> errors) {
        return new ValidationResult(false, errors);
    }
}

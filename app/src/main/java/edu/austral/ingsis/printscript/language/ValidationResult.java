package edu.austral.ingsis.printscript.language;

/** Resultado de validar un programa: valido, o el primer error encontrado. */
public record ValidationResult(boolean valid, SourceError error) {

    static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    static ValidationResult invalid(SourceError error) {
        return new ValidationResult(false, error);
    }
}

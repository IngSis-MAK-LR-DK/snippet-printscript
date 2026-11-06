package edu.austral.ingsis.printscript.language;

import java.util.List;

/** Lo que imprimio el programa, en orden, y los errores que lo detuvieron (si hubo). */
public record ExecutionResult(List<String> outputs, List<String> errors) {}

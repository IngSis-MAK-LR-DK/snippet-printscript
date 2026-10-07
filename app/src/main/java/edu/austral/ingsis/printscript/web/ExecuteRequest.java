package edu.austral.ingsis.printscript.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * inputs responde a cada readInput, en orden. env responde a readEnv: nunca se leen las variables
 * de entorno del servidor.
 */
public record ExecuteRequest(
        @NotNull @Size(max = LanguageController.MAX_CODE_LENGTH) String code,
        String version,
        List<String> inputs,
        Map<String, String> env) {}

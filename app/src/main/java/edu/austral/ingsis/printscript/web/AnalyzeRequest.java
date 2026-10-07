package edu.austral.ingsis.printscript.web;

import edu.austral.ingsis.printscript.language.LintOptions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** config es opcional: los campos que falten toman el valor por defecto de PrintScript. */
public record AnalyzeRequest(
        @NotNull @Size(max = LanguageController.MAX_CODE_LENGTH) String code,
        String version,
        LintOptions config) {}

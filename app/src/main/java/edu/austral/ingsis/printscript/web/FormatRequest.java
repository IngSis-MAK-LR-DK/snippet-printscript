package edu.austral.ingsis.printscript.web;

import java.util.List;

import edu.austral.ingsis.printscript.language.RuleSetting;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** rules es opcional: las reglas que no vienen toman el valor por defecto de PrintScript. */
public record FormatRequest(
        @NotNull @Size(max = LanguageController.MAX_CODE_LENGTH) String content,
        String version,
        List<RuleSetting> rules) {}

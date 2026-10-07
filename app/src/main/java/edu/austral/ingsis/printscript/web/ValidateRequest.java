package edu.austral.ingsis.printscript.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidateRequest(
        @NotNull @Size(max = LanguageController.MAX_CODE_LENGTH) String code, String version) {}

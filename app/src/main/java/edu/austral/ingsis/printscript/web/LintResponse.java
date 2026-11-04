package edu.austral.ingsis.printscript.web;

import java.util.List;

import edu.austral.ingsis.printscript.language.Violation;

public record LintResponse(List<Violation> violations) {}

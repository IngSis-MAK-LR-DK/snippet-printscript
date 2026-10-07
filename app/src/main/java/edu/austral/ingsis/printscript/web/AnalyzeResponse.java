package edu.austral.ingsis.printscript.web;

import java.util.List;

import edu.austral.ingsis.printscript.language.Finding;

public record AnalyzeResponse(List<Finding> findings) {}

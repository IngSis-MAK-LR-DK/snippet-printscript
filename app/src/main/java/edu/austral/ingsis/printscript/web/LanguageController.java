package edu.austral.ingsis.printscript.web;

import edu.austral.ingsis.printscript.language.LanguageService;
import edu.austral.ingsis.printscript.language.ValidationResult;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LanguageController {

    /** Tope de tamaño del codigo que se acepta en un pedido. */
    static final int MAX_CODE_LENGTH = 200_000;

    private final LanguageService service;

    public LanguageController(LanguageService service) {
        this.service = service;
    }

    /** Siempre 200: un codigo invalido es una respuesta normal ("valid": false y el error). */
    @PostMapping("/validate")
    public ValidationResult validate(@Valid @RequestBody ValidateRequest request) {
        return service.validate(request.code(), Versions.parse(request.version()));
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return new AnalyzeResponse(
                service.analyze(
                        request.code(), Versions.parse(request.version()), request.config()));
    }

    @PostMapping("/format")
    public FormatResponse format(@Valid @RequestBody FormatRequest request) {
        return new FormatResponse(
                service.format(
                        request.code(), Versions.parse(request.version()), request.config()));
    }

    @PostMapping("/execute")
    public ExecuteResponse execute(@Valid @RequestBody ExecuteRequest request) {
        return new ExecuteResponse(
                service.execute(
                        request.code(),
                        Versions.parse(request.version()),
                        request.inputs(),
                        request.env()));
    }
}

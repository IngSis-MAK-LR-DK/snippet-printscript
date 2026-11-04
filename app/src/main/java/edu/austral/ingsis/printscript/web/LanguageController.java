package edu.austral.ingsis.printscript.web;

import edu.austral.ingsis.printscript.language.ExecutionResult;
import edu.austral.ingsis.printscript.language.LanguageService;
import edu.austral.ingsis.printscript.language.RuleSetting;
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

    /** Siempre 200: un codigo invalido es una respuesta normal ("valid": false y los errores). */
    @PostMapping("/validate")
    public ValidationResult validate(@Valid @RequestBody ValidateRequest request) {
        return service.validate(request.content(), Versions.parse(request.version()));
    }

    @PostMapping("/lint")
    public LintResponse lint(@Valid @RequestBody LintRequest request) {
        return new LintResponse(
                service.lint(
                        request.content(),
                        Versions.parse(request.version()),
                        RuleSetting.toLintOptions(request.rules())));
    }

    @PostMapping("/format")
    public FormatResponse format(@Valid @RequestBody FormatRequest request) {
        return new FormatResponse(
                service.format(
                        request.content(),
                        Versions.parse(request.version()),
                        RuleSetting.toFormatOptions(request.rules())));
    }

    /** Siempre 200: los errores de ejecucion van en "errors", como pide el contrato. */
    @PostMapping("/execute")
    public ExecutionResult execute(@Valid @RequestBody ExecuteRequest request) {
        return service.execute(
                request.content(),
                Versions.parse(request.version()),
                request.inputs(),
                request.env());
    }
}

package edu.austral.ingsis.printscript.web;

import java.util.List;

import edu.austral.ingsis.printscript.language.RuleCatalog;
import edu.austral.ingsis.printscript.language.RuleDefinition;
import edu.austral.ingsis.printscript.language.RuleType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RuleController {

    /** Reglas que ofrece el lenguaje, con su configuracion por defecto. type es LINT o FORMAT. */
    @GetMapping("/rules")
    public List<RuleDefinition> rules(@RequestParam RuleType type) {
        return switch (type) {
            case LINT -> RuleCatalog.lintRules();
            case FORMAT -> RuleCatalog.formatRules();
        };
    }
}

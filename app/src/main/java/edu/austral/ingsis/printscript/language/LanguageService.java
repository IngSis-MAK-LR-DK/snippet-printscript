package edu.austral.ingsis.printscript.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.austral.ingsis.printscript.analyzer.AnalysisFinding;
import edu.austral.ingsis.printscript.analyzer.Analyzer;
import edu.austral.ingsis.printscript.analyzer.AnalyzerConfig;
import edu.austral.ingsis.printscript.analyzer.PrintScriptAnalyzer;
import edu.austral.ingsis.printscript.common.PrintScriptException;
import edu.austral.ingsis.printscript.common.Version;
import edu.austral.ingsis.printscript.common.ast.Statement;
import edu.austral.ingsis.printscript.formatter.Formatter;
import edu.austral.ingsis.printscript.formatter.PrintScriptFormatter;
import edu.austral.ingsis.printscript.interpreter.EnvironmentReader;
import edu.austral.ingsis.printscript.interpreter.ExecutionContext;
import edu.austral.ingsis.printscript.interpreter.Interpreter;
import edu.austral.ingsis.printscript.interpreter.PrintScriptInterpreter;
import edu.austral.ingsis.printscript.interpreter.PrintScriptSemanticAnalyzer;
import edu.austral.ingsis.printscript.interpreter.SemanticAnalyzer;
import edu.austral.ingsis.printscript.lexer.PrintScriptLexer;
import edu.austral.ingsis.printscript.lexer.StringPositionalSource;
import edu.austral.ingsis.printscript.parser.PrintScriptParser;

import org.springframework.stereotype.Service;

/**
 * Usa la libreria PrintScript para validar, analizar, formatear y ejecutar codigo. No guarda nada:
 * todo el estado llega en cada llamada. Validar y ejecutar devuelven sus errores como datos;
 * formatear y lint dejan pasar la {@link PrintScriptException} si el codigo no es valido.
 */
@Service
public class LanguageService {

    private final SemanticAnalyzer semanticAnalyzer = new PrintScriptSemanticAnalyzer();
    private final Analyzer analyzer = new PrintScriptAnalyzer();
    private final Formatter formatter = new PrintScriptFormatter();
    private final Interpreter interpreter = new PrintScriptInterpreter();

    /** Lexico, sintaxis y semantica. Devuelve el error en vez de lanzarlo. */
    public ValidationResult validate(String code, Version version) {
        try {
            semanticAnalyzer.analyze(parse(code, version));
            return ValidationResult.ok();
        } catch (PrintScriptException e) {
            SourceError error = SourceError.of(e);
            return ValidationResult.invalid(
                    List.of(Violation.of(error.type(), error.message(), error.start())));
        }
    }

    /**
     * Reglas de lint incumplidas, cada una con el nombre de la regla. La libreria no dice que regla
     * produjo cada hallazgo, asi que el analizador corre una vez por regla activa.
     */
    public List<Violation> lint(String code, Version version, LintOptions options) {
        LintOptions rules = options != null ? options : new LintOptions(null, null, null, null);
        List<Violation> violations = new ArrayList<>();
        for (Map.Entry<String, AnalyzerConfig> rule :
                singleRuleConfigs(rules.toConfig()).entrySet()) {
            for (AnalysisFinding finding :
                    analyzer.analyze(parse(code, version), rule.getValue())) {
                violations.add(
                        Violation.of(
                                rule.getKey(), finding.message(), Location.from(finding.start())));
            }
        }
        return violations;
    }

    public String format(String code, Version version, FormatOptions options) {
        FormatOptions rules =
                options != null
                        ? options
                        : new FormatOptions(null, null, null, null, null, null, null);
        return formatter.format(parse(code, version), rules.toConfig());
    }

    /**
     * Ejecuta el programa. Cada readInput consume el siguiente valor de inputs; cada println se
     * agrega a la lista de salida. readEnv solo ve las variables de env que manda el cliente (nunca
     * las del servidor). Si el programa falla (tambien si no parsea), el error va en errors y la
     * salida hasta ese punto se conserva.
     */
    public ExecutionResult execute(
            String code, Version version, List<String> inputs, Map<String, String> env) {
        Iterator<String> remainingInputs = (inputs != null ? inputs : List.<String>of()).iterator();
        Map<String, String> variables = env != null ? env : Collections.emptyMap();
        List<String> outputs = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        EnvironmentReader environment = name -> Optional.ofNullable(variables.get(name));
        ExecutionContext context =
                new ExecutionContext(
                        outputs::add,
                        prompt -> {
                            if (!remainingInputs.hasNext()) {
                                throw new MissingInputException(prompt);
                            }
                            return remainingInputs.next();
                        },
                        environment);

        try {
            interpreter.interpret(parse(code, version), context);
        } catch (PrintScriptException e) {
            errors.add(e.getMessage());
        }
        return new ExecutionResult(outputs, errors);
    }

    /** Un analizador por regla: cada uno tiene activa solo esa regla. */
    private static Map<String, AnalyzerConfig> singleRuleConfigs(AnalyzerConfig active) {
        Map<String, AnalyzerConfig> configs = new LinkedHashMap<>();
        if (active.identifierCaseCheckEnabled()) {
            configs.put(
                    RuleCatalog.IDENTIFIER_CASE,
                    new AnalyzerConfig(true, active.identifierCase(), false, false));
        }
        if (active.printlnArgumentMustBeIdentifierOrLiteral()) {
            configs.put(
                    RuleCatalog.PRINTLN_ARGUMENT,
                    new AnalyzerConfig(false, active.identifierCase(), true, false));
        }
        if (active.readArgumentMustBeIdentifierOrLiteral()) {
            configs.put(
                    RuleCatalog.READ_ARGUMENT,
                    new AnalyzerConfig(false, active.identifierCase(), false, true));
        }
        return configs;
    }

    private Iterator<Statement> parse(String code, Version version) {
        var tokens = new PrintScriptLexer().tokenize(new StringPositionalSource(code));
        return new PrintScriptParser(Set.of(), version).parse(tokens);
    }
}

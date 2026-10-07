package edu.austral.ingsis.printscript.language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import edu.austral.ingsis.printscript.analyzer.AnalysisFinding;
import edu.austral.ingsis.printscript.analyzer.Analyzer;
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
 * todo el estado llega en cada llamada. Todos los metodos, salvo {@link #validate}, dejan pasar la
 * {@link PrintScriptException} si el codigo no es valido.
 */
@Service
public class LanguageService {

    private final SemanticAnalyzer semanticAnalyzer = new PrintScriptSemanticAnalyzer();
    private final Analyzer analyzer = new PrintScriptAnalyzer();
    private final Formatter formatter = new PrintScriptFormatter();
    private final Interpreter interpreter = new PrintScriptInterpreter();

    /** Lexico, sintaxis y semantica. Devuelve el primer error en vez de lanzarlo. */
    public ValidationResult validate(String code, Version version) {
        try {
            semanticAnalyzer.analyze(parse(code, version));
            return ValidationResult.ok();
        } catch (PrintScriptException e) {
            return ValidationResult.invalid(SourceError.of(e));
        }
    }

    /** Reglas de linting incumplidas; lista vacia si el codigo las cumple todas. */
    public List<Finding> analyze(String code, Version version, LintOptions options) {
        LintOptions rules = options != null ? options : new LintOptions(null, null, null, null);
        List<AnalysisFinding> findings = analyzer.analyze(parse(code, version), rules.toConfig());
        return findings.stream()
                .map(
                        f ->
                                new Finding(
                                        f.message(),
                                        Location.from(f.start()),
                                        Location.from(f.end())))
                .toList();
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
     * las del servidor).
     */
    public List<String> execute(
            String code, Version version, List<String> inputs, Map<String, String> env) {
        Iterator<String> remainingInputs = (inputs != null ? inputs : List.<String>of()).iterator();
        Map<String, String> variables = env != null ? env : Collections.emptyMap();
        List<String> outputs = new ArrayList<>();

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

        interpreter.interpret(parse(code, version), context);
        return outputs;
    }

    private Iterator<Statement> parse(String code, Version version) {
        var tokens = new PrintScriptLexer().tokenize(new StringPositionalSource(code));
        return new PrintScriptParser(Set.of(), version).parse(tokens);
    }
}

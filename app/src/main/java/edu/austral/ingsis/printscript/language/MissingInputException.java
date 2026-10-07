package edu.austral.ingsis.printscript.language;

/** El programa pidio un valor con readInput y el pedido no traia mas inputs. */
public class MissingInputException extends RuntimeException {

    public MissingInputException(String prompt) {
        super("El programa pidio un input (\"" + prompt + "\") pero no se enviaron mas inputs");
    }
}

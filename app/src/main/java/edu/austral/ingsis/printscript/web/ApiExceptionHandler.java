package edu.austral.ingsis.printscript.web;

import edu.austral.ingsis.printscript.common.PrintScriptException;
import edu.austral.ingsis.printscript.language.SourceError;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    /** El pedido esta bien armado pero el codigo no es PrintScript valido (o falla al ejecutar). */
    @ExceptionHandler(PrintScriptException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse invalidCode(PrintScriptException e) {
        SourceError error = SourceError.of(e);
        return new ErrorResponse(e.getMessage(), error);
    }

    /** Version inexistente o regla con un valor invalido. */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badArgument(IllegalArgumentException e) {
        return ErrorResponse.of(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse invalidBody(MethodArgumentNotValidException e) {
        return ErrorResponse.of("El body del pedido no es valido");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse unreadableBody(HttpMessageNotReadableException e) {
        return ErrorResponse.of("El body del pedido no es un JSON valido");
    }
}

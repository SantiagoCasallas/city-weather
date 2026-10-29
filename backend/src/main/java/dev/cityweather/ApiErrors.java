package dev.cityweather;

import org.springframework.http.*;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(ProviderBudget.Exhausted.class)
  ResponseEntity<Error> quota() {
    return ResponseEntity.status(429)
        .header("Retry-After", "60")
        .body(
            new Error(
                "PROVIDER_BUDGET_EXCEEDED",
                "Alcanzamos el límite temporal de consultas. Intenta de nuevo más tarde."));
  }

  public record Error(String code, String message) {}

  @ExceptionHandler(ProviderException.class)
  ResponseEntity<Error> provider(ProviderException e) {
    return ResponseEntity.status(e.isTimeout() ? 504 : 502)
        .body(
            new Error(
                e.isTimeout() ? "PROVIDER_TIMEOUT" : "PROVIDER_UNAVAILABLE",
                e.isTimeout()
                    ? "El proveedor está tardando demasiado. Intenta de nuevo en unos momentos."
                    : "No pudimos obtener datos del proveedor. Intenta de nuevo más tarde."));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Error> validation(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(new Error("INVALID_REQUEST", e.getReason()));
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<Error> invalid(Exception e) {
    return ResponseEntity.badRequest()
        .body(new Error("INVALID_REQUEST", "Revisa los parámetros de la consulta."));
  }
}

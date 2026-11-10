package fr.corentinbringer.smarttasks.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Traduit les exceptions en réponses HTTP, toutes au même format : { "error": "..." }.
 *
 * Hérite de ResponseEntityExceptionHandler : cette classe de Spring traite déjà les erreurs
 * standard de Spring MVC avec le bon code HTTP (JSON mal formé → 400, paramètre du mauvais type → 400,
 * URL inconnue → 404, mauvaise méthode → 405, fichier trop gros → 413, Content-Type refusé → 415...).
 * Sans elle, notre handler générique Exception.class les attraperait toutes et répondrait 500.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Erreurs métier de l'application : une seule méthode, un switch EXHAUSTIF.
     *
     * SmartTasksException est "sealed" : le compilateur connaît la liste complète de ses
     * sous-classes. Pas besoin de "default" ; et si on ajoute une sous-classe sans la traiter
     * ici, le code ne compile plus.
     */
    @ExceptionHandler(SmartTasksException.class)
    public ResponseEntity<Map<String, String>> handleApplicationError(SmartTasksException ex) {
        return switch (ex) {
            case ResourceNotFoundException e -> error(HttpStatus.NOT_FOUND, "Resource not found");
            case InvalidRequestException e -> error(HttpStatus.BAD_REQUEST, e.getMessage());
            case StorageException e -> {
                log.error("Object storage error", e);
                yield error(HttpStatus.INTERNAL_SERVER_ERROR, "File storage error");
            }
        };
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    /**
     * 400 - Validation errors on @Valid DTOs.
     * Déjà gérée par la classe parente : on la redéfinit (@Override) pour ajouter le détail par champ.
     * (Un @ExceptionHandler(MethodArgumentNotValidException.class) ferait doublon avec le parent :
     * Spring refuserait de démarrer, « Ambiguous @ExceptionHandler method ».)
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "details", errors
        ));
    }

    /**
     * Point de passage de toutes les autres erreurs standard de Spring MVC.
     * On garde le code HTTP et les headers calculés par Spring (ex. "Allow: GET, POST" pour un 405),
     * mais on remplace le corps (un ProblemDetail, RFC 9457) par notre format { "error": "..." }.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response == null) {
            return null; // réponse déjà envoyée au client : on ne peut plus rien écrire
        }

        if (statusCode.is5xxServerError()) {
            log.error("Spring MVC error", ex);
        }

        String message = response.getBody() instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : "Request failed";

        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(Map.of("error", message));
    }

    /**
     * 500 - Generic server error : tout ce qui n'a pas été prévu.
     * Une NoSuchElementException "perdue" (Optional.get() sur un Optional vide...) arrive ici :
     * c'est un bug, pas une ressource introuvable, donc 500 et non 404.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleServerError(Exception ex) {
        log.error("Unexpected server error", ex);

        return Map.of(
                "error", "Internal server error",
                "message", "An unexpected error occurred. Please try again later."
        );
    }
}

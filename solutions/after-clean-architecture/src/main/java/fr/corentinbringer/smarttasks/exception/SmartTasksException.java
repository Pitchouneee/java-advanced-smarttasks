package fr.corentinbringer.smarttasks.exception;

/**
 * Racine des erreurs "prévues" de SmartTasks.
 *
 * "sealed ... permits" : seules les classes listées peuvent en hériter.
 * L'ApiExceptionHandler peut donc faire un switch exhaustif (sans default) sur ce type.
 * Les sous-classes doivent être dans le même package (on n'utilise pas de module Java).
 */
public sealed abstract class SmartTasksException extends RuntimeException
        permits ResourceNotFoundException, InvalidRequestException, StorageException {

    protected SmartTasksException(String message, Throwable cause) {
        super(message, cause);
    }
}

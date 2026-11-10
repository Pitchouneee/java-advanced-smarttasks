package fr.corentinbringer.smarttasks.exception;

/** Requête invalide que la validation des DTO (@Valid) ne peut pas détecter : 400. */
public final class InvalidRequestException extends SmartTasksException {

    public InvalidRequestException(String message) {
        super(message, null);
    }
}

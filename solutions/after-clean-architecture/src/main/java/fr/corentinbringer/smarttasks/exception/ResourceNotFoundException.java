package fr.corentinbringer.smarttasks.exception;

/**
 * Ressource absente, OU appartenant à un autre tenant : dans les deux cas l'API répond 404,
 * pour ne pas révéler l'existence des données des autres utilisateurs.
 */
public final class ResourceNotFoundException extends SmartTasksException {

    public ResourceNotFoundException(String message) {
        super(message, null);
    }
}

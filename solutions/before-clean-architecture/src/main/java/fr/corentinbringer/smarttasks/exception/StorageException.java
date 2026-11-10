package fr.corentinbringer.smarttasks.exception;

/**
 * Erreur technique du stockage objet (serveur S3 injoignable, objet absent du bucket...).
 *
 * Ce n'est pas la faute du client : l'API répond 500 (voir ApiExceptionHandler),
 * et non 404 (ressource inexistante) ou 400 (requête invalide).
 */
public final class StorageException extends SmartTasksException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

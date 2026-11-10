package fr.corentinbringer.smarttasks.project.application.model;

import java.io.InputStream;

/**
 * Un fichier vu par l'application : ses métadonnées et un flux vers son contenu.
 *
 * Remplace MultipartFile (Spring Web) en entrée et InputStreamResource en sortie :
 * le service peut ainsi être appelé depuis un controller REST, un batch ou un consommateur de messages.
 * Le flux n'est pas chargé en mémoire ; c'est à celui qui l'ouvre de le fermer.
 */
public record FileContent(
        String name,
        String contentType,
        long size,
        InputStream content
) {}

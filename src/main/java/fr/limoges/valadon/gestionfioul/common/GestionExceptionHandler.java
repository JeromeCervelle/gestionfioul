package fr.limoges.valadon.gestionfioul.common;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions levées par les services en réponses HTTP au format ProblemDetail (RFC 9457).
 *
 * <p>Les services ne connaissent pas HTTP : ils lèvent des exceptions standard de Java,
 * et c'est ici qu'est choisi le code HTTP.</p>
 * <table>
 *   <caption>Exception levée par un service et réponse HTTP</caption>
 *   <tr><th>Exception</th><th>Code</th><th>Signification</th></tr>
 *   <tr><td>{@link IllegalArgumentException}</td><td>400</td><td>donnée invalide</td></tr>
 *   <tr><td>{@link NoSuchElementException}</td><td>404</td><td>ressource inexistante</td></tr>
 *   <tr><td>{@link IllegalStateException}</td><td>409</td><td>action impossible dans l'état actuel</td></tr>
 * </table>
 */
@RestControllerAdvice
public class GestionExceptionHandler {

    /**
     * Donnée invalide : 400.
     *
     * @param e exception levée par un service
     * @return le ProblemDetail, dont le {@code detail} est le message de l'exception
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail donneeInvalide(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Ressource inexistante : 404.
     *
     * @param e exception levée par un service
     * @return le ProblemDetail, dont le {@code detail} est le message de l'exception
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail introuvable(NoSuchElementException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Action impossible dans l'état actuel (campagne déjà clôturée, doublon...) : 409.
     *
     * @param e exception levée par un service
     * @return le ProblemDetail, dont le {@code detail} est le message de l'exception
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail etatIncompatible(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}

package de.witchcafe.billing.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.witchcafe.billing.client.api.ErrorResponse;
import de.witchcafe.billing.server.ledger.InsufficientCreditsException;
import de.witchcafe.billing.server.ledger.UnknownFeatureException;

/**
 * Uebersetzt Fehler des BillingService in {@link ErrorResponse}s der REST-API.
 */
@RestControllerAdvice(assignableTypes = BillingApiController.class)
class ApiErrorHandler {

    @ExceptionHandler
    ResponseEntity<ErrorResponse> insufficient(InsufficientCreditsException e) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(new ErrorResponse(
                ErrorResponse.INSUFFICIENT_CREDITS, e.getMessage(), e.getBalance(), e.getRequired()));
    }

    @ExceptionHandler
    ResponseEntity<ErrorResponse> unknownFeature(UnknownFeatureException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                ErrorResponse.UNKNOWN_FEATURE, e.getMessage(), null, null));
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class, ArithmeticException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> invalid(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                ErrorResponse.INVALID_REQUEST, e.getMessage(), null, null));
    }
}

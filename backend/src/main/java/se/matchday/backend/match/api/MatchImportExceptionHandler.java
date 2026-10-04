package se.matchday.backend.match.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.matchday.backend.match.application.InvalidMatchDataProviderResponseException;
import se.matchday.backend.match.application.InvalidSeasonMatchDataException;
import se.matchday.backend.match.application.MatchDataProviderUnavailableException;

@RestControllerAdvice(assignableTypes = MatchImportController.class)
class MatchImportExceptionHandler {

  private static final URI INVALID_REQUEST_TYPE =
      URI.create("urn:matchday:problem:invalid-request");
  private static final URI INVALID_PROVIDER_RESPONSE_TYPE =
      URI.create("urn:matchday:problem:invalid-match-provider-response");
  private static final URI PROVIDER_UNAVAILABLE_TYPE =
      URI.create("urn:matchday:problem:match-provider-unavailable");

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST_TYPE,
        "Invalid request",
        "The request body must contain a positive integer season");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST_TYPE,
        "Invalid request",
        "The request body is missing or malformed");
  }

  @ExceptionHandler({
    InvalidMatchDataProviderResponseException.class,
    InvalidSeasonMatchDataException.class
  })
  ProblemDetail handleInvalidProviderResponse(RuntimeException exception) {
    return problem(
        HttpStatus.BAD_GATEWAY,
        INVALID_PROVIDER_RESPONSE_TYPE,
        "Invalid match provider response",
        "The match data provider returned an invalid response");
  }

  @ExceptionHandler(MatchDataProviderUnavailableException.class)
  ProblemDetail handleProviderUnavailable(MatchDataProviderUnavailableException exception) {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        PROVIDER_UNAVAILABLE_TYPE,
        "Match provider unavailable",
        "The match data provider is unavailable");
  }

  private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(type);
    problem.setTitle(title);
    return problem;
  }
}

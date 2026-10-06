package se.matchday.backend.circle.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import se.matchday.backend.circle.application.CircleAlreadyExistsException;
import se.matchday.backend.circle.application.CircleNotFoundForMatchException;
import se.matchday.backend.circle.application.MatchNotFoundException;

@RestControllerAdvice(assignableTypes = CircleController.class)
class CircleExceptionHandler {

  private static final URI MATCH_NOT_FOUND_TYPE =
      URI.create("urn:matchday:problem:match-not-found");
  private static final URI CIRCLE_NOT_FOUND_TYPE =
      URI.create("urn:matchday:problem:circle-not-found");
  private static final URI CIRCLE_ALREADY_EXISTS_TYPE =
      URI.create("urn:matchday:problem:circle-already-exists");
  private static final URI INVALID_REQUEST_TYPE =
      URI.create("urn:matchday:problem:invalid-request");

  @ExceptionHandler(MatchNotFoundException.class)
  ProblemDetail handleMatchNotFound(MatchNotFoundException exception) {
    return problem(
        HttpStatus.NOT_FOUND, MATCH_NOT_FOUND_TYPE, "Match not found", exception.getMessage());
  }

  @ExceptionHandler(CircleAlreadyExistsException.class)
  ProblemDetail handleCircleAlreadyExists(CircleAlreadyExistsException exception) {
    return problem(
        HttpStatus.CONFLICT,
        CIRCLE_ALREADY_EXISTS_TYPE,
        "Circle already exists",
        exception.getMessage());
  }

  @ExceptionHandler(CircleNotFoundForMatchException.class)
  ProblemDetail handleCircleNotFound(CircleNotFoundForMatchException exception) {
    return problem(
        HttpStatus.NOT_FOUND, CIRCLE_NOT_FOUND_TYPE, "Circle not found", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail handleInvalidPathParameter(MethodArgumentTypeMismatchException exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST_TYPE,
        "Invalid request",
        "The request path contains an invalid value");
  }

  private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(type);
    problem.setTitle(title);
    return problem;
  }
}

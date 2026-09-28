package se.matchday.backend.circle.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.matchday.backend.circle.application.CircleAlreadyExistsException;
import se.matchday.backend.circle.application.CurrentUserUnavailableException;
import se.matchday.backend.circle.application.MatchNotFoundException;

@RestControllerAdvice(assignableTypes = CircleController.class)
class CircleExceptionHandler {

  private static final URI AUTHENTICATION_REQUIRED_TYPE =
      URI.create("urn:matchday:problem:authentication-required");
  private static final URI MATCH_NOT_FOUND_TYPE =
      URI.create("urn:matchday:problem:match-not-found");
  private static final URI CIRCLE_ALREADY_EXISTS_TYPE =
      URI.create("urn:matchday:problem:circle-already-exists");

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

  @ExceptionHandler(CurrentUserUnavailableException.class)
  ProblemDetail handleCurrentUserUnavailable(CurrentUserUnavailableException exception) {
    return problem(
        HttpStatus.UNAUTHORIZED,
        AUTHENTICATION_REQUIRED_TYPE,
        "Authentication required",
        exception.getMessage());
  }

  private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(type);
    problem.setTitle(title);
    return problem;
  }
}

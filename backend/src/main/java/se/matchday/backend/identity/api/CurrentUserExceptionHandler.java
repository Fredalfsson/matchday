package se.matchday.backend.identity.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.matchday.backend.identity.application.CurrentUserUnavailableException;

@RestControllerAdvice
class CurrentUserExceptionHandler {

  private static final URI AUTHENTICATION_REQUIRED_TYPE =
      URI.create("urn:matchday:problem:authentication-required");

  @ExceptionHandler(CurrentUserUnavailableException.class)
  ProblemDetail handleCurrentUserUnavailable(CurrentUserUnavailableException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    problem.setType(AUTHENTICATION_REQUIRED_TYPE);
    problem.setTitle("Authentication required");
    return problem;
  }
}

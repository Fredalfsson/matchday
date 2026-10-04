package se.matchday.backend.identity.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.matchday.backend.identity.application.UserDirectoryUnavailableException;

@RestControllerAdvice
class UserDirectoryExceptionHandler {

  private static final URI USER_DIRECTORY_UNAVAILABLE_TYPE =
      URI.create("urn:matchday:problem:user-directory-unavailable");

  @ExceptionHandler(UserDirectoryUnavailableException.class)
  ProblemDetail handleUserDirectoryUnavailable(UserDirectoryUnavailableException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    problem.setType(USER_DIRECTORY_UNAVAILABLE_TYPE);
    problem.setTitle("User directory unavailable");
    return problem;
  }
}

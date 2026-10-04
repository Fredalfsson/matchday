package se.matchday.backend.message.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import se.matchday.backend.circle.application.ActiveCircleMembershipRequiredException;
import se.matchday.backend.circle.application.CircleNotFoundByIdException;
import se.matchday.backend.message.application.InvalidMessageContentException;

@RestControllerAdvice(assignableTypes = MessageController.class)
class MessageExceptionHandler {

  private static final URI CIRCLE_NOT_FOUND_TYPE =
      URI.create("urn:matchday:problem:circle-not-found");
  private static final URI ACTIVE_MEMBERSHIP_REQUIRED_TYPE =
      URI.create("urn:matchday:problem:active-circle-membership-required");
  private static final URI INVALID_MESSAGE_CONTENT_TYPE =
      URI.create("urn:matchday:problem:invalid-message-content");
  private static final URI INVALID_REQUEST_TYPE =
      URI.create("urn:matchday:problem:invalid-request");

  @ExceptionHandler(CircleNotFoundByIdException.class)
  ProblemDetail handleCircleNotFound(CircleNotFoundByIdException exception) {
    return problem(
        HttpStatus.NOT_FOUND, CIRCLE_NOT_FOUND_TYPE, "Circle not found", exception.getMessage());
  }

  @ExceptionHandler(ActiveCircleMembershipRequiredException.class)
  ProblemDetail handleActiveMembershipRequired(ActiveCircleMembershipRequiredException exception) {
    return problem(
        HttpStatus.FORBIDDEN,
        ACTIVE_MEMBERSHIP_REQUIRED_TYPE,
        "Active circle membership required",
        exception.getMessage());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, InvalidMessageContentException.class})
  ProblemDetail handleInvalidMessageContent(Exception exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        INVALID_MESSAGE_CONTENT_TYPE,
        "Invalid message content",
        "Message content must be between 1 and 1000 characters and must not contain null characters");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST_TYPE,
        "Invalid request",
        "The request body is missing or malformed");
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

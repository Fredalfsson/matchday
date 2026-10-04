package se.matchday.backend.message.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.matchday.backend.message.application.MessageCreationResult;
import se.matchday.backend.message.application.MessageCreationService;

@RestController
@RequestMapping("/api/v1/circles/{circleId}/messages")
class MessageController {

  private final MessageCreationService messageCreationService;

  MessageController(MessageCreationService messageCreationService) {
    this.messageCreationService = messageCreationService;
  }

  @PostMapping
  ResponseEntity<MessageResponse> createMessage(
      @PathVariable UUID circleId, @Valid @RequestBody MessageCreationRequest request) {
    MessageCreationResult result =
        messageCreationService.createForCircle(circleId, request.content());
    URI location = URI.create("/api/v1/circles/" + circleId + "/messages/" + result.id());
    return ResponseEntity.created(location).body(MessageResponse.from(result));
  }
}

package se.matchday.backend.message.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.matchday.backend.message.application.MessageCreationResult;
import se.matchday.backend.message.application.MessageCreationService;
import se.matchday.backend.message.application.MessageHistoryResult;
import se.matchday.backend.message.application.MessageHistoryService;

@RestController
@RequestMapping("/api/v1/circles/{circleId}/messages")
class MessageController {

  private final MessageCreationService messageCreationService;
  private final MessageHistoryService messageHistoryService;

  MessageController(
      MessageCreationService messageCreationService, MessageHistoryService messageHistoryService) {
    this.messageCreationService = messageCreationService;
    this.messageHistoryService = messageHistoryService;
  }

  @GetMapping
  ResponseEntity<MessageHistoryResponse> getMessageHistory(
      @PathVariable UUID circleId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    MessageHistoryResult result = messageHistoryService.listForCircle(circleId, page, size);
    return ResponseEntity.ok(MessageHistoryResponse.from(result));
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

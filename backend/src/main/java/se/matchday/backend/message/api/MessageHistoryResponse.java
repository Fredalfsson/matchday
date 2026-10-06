package se.matchday.backend.message.api;

import java.util.List;
import se.matchday.backend.message.application.MessageHistoryResult;

record MessageHistoryResponse(List<MessageResponse> messages, int page, int size, boolean hasNext) {

  MessageHistoryResponse {
    messages = List.copyOf(messages);
  }

  static MessageHistoryResponse from(MessageHistoryResult result) {
    List<MessageResponse> messages =
        result.messages().stream().map(entry -> MessageResponse.from(entry)).toList();
    return new MessageHistoryResponse(messages, result.page(), result.size(), result.hasNext());
  }
}

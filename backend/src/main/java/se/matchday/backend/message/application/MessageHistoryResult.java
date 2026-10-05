package se.matchday.backend.message.application;

import java.util.List;
import java.util.Objects;

public record MessageHistoryResult(
    List<MessageHistoryEntry> messages, int page, int size, boolean hasNext) {

  public MessageHistoryResult {
    Objects.requireNonNull(messages, "messages must not be null");
    messages = List.copyOf(messages);
  }
}

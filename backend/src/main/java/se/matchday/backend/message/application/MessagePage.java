package se.matchday.backend.message.application;

import java.util.List;
import java.util.Objects;
import se.matchday.backend.message.domain.Message;

public record MessagePage(List<Message> messages, boolean hasNext) {

  public MessagePage {
    Objects.requireNonNull(messages, "messages must not be null");
    messages = List.copyOf(messages);
  }
}

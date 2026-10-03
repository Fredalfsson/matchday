package se.matchday.backend.message.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Message(
    UUID id, UUID circleId, UUID authorUserId, String content, Instant createdAt) {

  private static final int MAX_CONTENT_LENGTH = 1_000;

  public Message {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(circleId, "circleId must not be null");
    Objects.requireNonNull(authorUserId, "authorUserId must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");

    validateContent(content);
    if (!content.equals(content.strip())) {
      throw new IllegalArgumentException("content must not have surrounding whitespace");
    }
  }

  public static String normalizeContent(String content) {
    Objects.requireNonNull(content, "content must not be null");
    String normalizedContent = content.strip();
    validateContent(normalizedContent);
    return normalizedContent;
  }

  private static void validateContent(String content) {
    if (content.isBlank()) {
      throw new IllegalArgumentException("content must not be blank");
    }
    if (content.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("content must not contain null characters");
    }
    if (characterCount(content) > MAX_CONTENT_LENGTH) {
      throw new IllegalArgumentException("content must not exceed 1000 characters");
    }
  }

  private static int characterCount(String value) {
    return value.codePointCount(0, value.length());
  }
}

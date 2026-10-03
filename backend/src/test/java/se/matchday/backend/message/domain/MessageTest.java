package se.matchday.backend.message.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageTest {

  private static final UUID MESSAGE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID CIRCLE_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final Instant CREATED_AT = Instant.parse("2026-10-02T18:30:00Z");

  @Test
  void acceptsInternalWhitespaceAndOneThousandUnicodeCharacters() {
    String content = "First line\n" + "😀".repeat(989);

    Message message = new Message(MESSAGE_ID, CIRCLE_ID, USER_ID, content, CREATED_AT);

    assertThat(message.content()).isEqualTo(content);
    assertThat(content.codePointCount(0, content.length())).isEqualTo(1_000);
    assertThat(content.length()).isGreaterThan(1_000);
  }

  @Test
  void rejectsBlankContent() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new Message(MESSAGE_ID, CIRCLE_ID, USER_ID, " \n\t", CREATED_AT))
        .withMessage("content must not be blank");
  }

  @Test
  void rejectsSurroundingWhitespace() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new Message(MESSAGE_ID, CIRCLE_ID, USER_ID, " message ", CREATED_AT))
        .withMessage("content must not have surrounding whitespace");
  }

  @Test
  void rejectsNullCharacters() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new Message(MESSAGE_ID, CIRCLE_ID, USER_ID, "invalid\0message", CREATED_AT))
        .withMessage("content must not contain null characters");
  }

  @Test
  void rejectsContentLongerThanOneThousandUnicodeCharacters() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new Message(MESSAGE_ID, CIRCLE_ID, USER_ID, "😀".repeat(1_001), CREATED_AT))
        .withMessage("content must not exceed 1000 characters");
  }
}

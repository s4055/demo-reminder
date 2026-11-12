package demo.ai.reminder.service;

import demo.ai.reminder.domain.reminder.Reminder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public record CompletedCursor(Instant completedAt, long id) {

    private static final String SEPARATOR = ",";

    public static CompletedCursor of(Reminder reminder) {
        return new CompletedCursor(reminder.getCompletedAt(), reminder.getId());
    }

    public static CompletedCursor decode(String value) {
        String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split(SEPARATOR, -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid completed cursor: " + value);
        }
        try {
            return new CompletedCursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid completed cursor: " + value, e);
        }
    }

    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((completedAt + SEPARATOR + id).getBytes(StandardCharsets.UTF_8));
    }
}

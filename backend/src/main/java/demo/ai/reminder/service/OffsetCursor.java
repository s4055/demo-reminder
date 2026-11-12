package demo.ai.reminder.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record OffsetCursor(int offset) {

    public static final OffsetCursor FIRST = new OffsetCursor(0);

    public OffsetCursor {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative: " + offset);
        }
    }

    public static OffsetCursor decode(String value) {
        String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        try {
            return new OffsetCursor(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid offset cursor: " + value, e);
        }
    }

    public OffsetCursor next(int size) {
        return new OffsetCursor(offset + size);
    }

    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(String.valueOf(offset).getBytes(StandardCharsets.UTF_8));
    }
}

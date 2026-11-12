package demo.ai.reminder.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TestTimes {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private TestTimes() {
    }

    public static Instant seoul(int year, int month, int day, int hour, int minute) {
        return seoul(year, month, day, hour, minute, 0);
    }

    public static Instant seoul(int year, int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, day, hour, minute, second).atZone(SEOUL).toInstant();
    }

    public static Instant seoul(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(SEOUL).toInstant();
    }
}

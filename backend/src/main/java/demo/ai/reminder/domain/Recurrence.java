package demo.ai.reminder.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record Recurrence(RepeatRule rule, int interval, Set<DayOfWeek> daysOfWeek) {

    public static final int MIN_INTERVAL = 1;
    public static final int MAX_INTERVAL = 99;
    public static final Recurrence NONE = new Recurrence(RepeatRule.NONE, MIN_INTERVAL, Set.of());

    public Recurrence {
        rule = Objects.requireNonNullElse(rule, RepeatRule.NONE);
        daysOfWeek = daysOfWeek == null || daysOfWeek.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(daysOfWeek));
        if (interval < MIN_INTERVAL || interval > MAX_INTERVAL) {
            throw new DomainException("Repeat interval must be " + MIN_INTERVAL + " to " + MAX_INTERVAL + ": " + interval);
        }
        if (!daysOfWeek.isEmpty() && rule != RepeatRule.WEEKLY) {
            throw new DomainException("Days of week can only be set for a weekly repeat: " + rule);
        }
        if (!rule.isRepeating()) {
            interval = MIN_INTERVAL;
        }
    }

    public static Recurrence of(RepeatRule rule, Integer interval, Set<DayOfWeek> daysOfWeek) {
        return new Recurrence(rule, Objects.requireNonNullElse(interval, MIN_INTERVAL), daysOfWeek);
    }

    public boolean isRepeating() {
        return rule.isRepeating();
    }

    public LocalDateTime nextDueAt(LocalDateTime dueAt) {
        if (rule != RepeatRule.WEEKLY || daysOfWeek.isEmpty()) {
            return rule.nextDueAt(dueAt, interval);
        }
        int dueDay = dueAt.getDayOfWeek().getValue();
        return daysOfWeek.stream()
                .filter(day -> day.getValue() > dueDay)
                .findFirst()
                .map(day -> dueAt.plusDays(day.getValue() - dueDay))
                .orElseGet(() -> {
                    LocalDateTime weekStart = dueAt.minusDays(dueDay - 1L);
                    int firstDay = daysOfWeek.iterator().next().getValue();
                    return weekStart.plusWeeks(interval).plusDays(firstDay - 1L);
                });
    }
}

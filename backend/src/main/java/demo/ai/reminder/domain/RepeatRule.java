package demo.ai.reminder.domain;

import java.time.LocalDateTime;

public enum RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public LocalDateTime nextDueAt(LocalDateTime dueAt, int interval) {
        return switch (this) {
            case NONE -> throw new IllegalStateException("A non-repeating reminder has no next due date");
            case DAILY -> dueAt.plusDays(interval);
            case WEEKLY -> dueAt.plusWeeks(interval);
            case MONTHLY -> dueAt.plusMonths(interval);
            case YEARLY -> dueAt.plusYears(interval);
        };
    }

    public boolean isRepeating() {
        return this != NONE;
    }
}

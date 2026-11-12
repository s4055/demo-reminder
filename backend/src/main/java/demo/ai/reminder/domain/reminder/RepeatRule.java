package demo.ai.reminder.domain.reminder;

import java.time.ZonedDateTime;

public enum RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public ZonedDateTime nextDueAt(ZonedDateTime dueAt, int interval) {
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

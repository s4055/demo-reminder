package demo.ai.reminder.domain;

import java.time.LocalDateTime;

public enum RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * 주기 단위로 interval만큼 뒤의 마감일시를 계산한다.
     * 그 달(해)에 같은 날짜가 없으면 그 달의 말일로 맞춘다 (1월 31일 → 2월 28/29일, 2월 29일 → 다음 해 2월 28일).
     */
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

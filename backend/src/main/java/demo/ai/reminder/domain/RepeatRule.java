package demo.ai.reminder.domain;

import java.time.LocalDateTime;

// 리마인더 반복 주기. 반복은 마감일시가 있는 리마인더에만 설정할 수 있다.
public enum RepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * 다음 회차의 마감일시를 계산한다.
     * 다음 달(해)에 같은 날짜가 없으면 그 달의 말일로 맞춘다 (1월 31일 → 2월 28/29일, 2월 29일 → 다음 해 2월 28일).
     */
    public LocalDateTime nextDueAt(LocalDateTime dueAt) {
        return switch (this) {
            case NONE -> throw new IllegalStateException("A non-repeating reminder has no next due date");
            case DAILY -> dueAt.plusDays(1);
            case WEEKLY -> dueAt.plusWeeks(1);
            case MONTHLY -> dueAt.plusMonths(1);
            case YEARLY -> dueAt.plusYears(1);
        };
    }

    public boolean isRepeating() {
        return this != NONE;
    }
}

package demo.ai.reminder.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 반복 규칙(주기 + 간격 + 요일)을 묶은 값 객체. 다음 회차의 마감일시를 계산한다.
 * - interval: 주기 단위로 몇 번마다 반복할지 (1~99). 반복하지 않으면 1로 둔다.
 * - daysOfWeek: 매주 반복할 요일. WEEKLY일 때만 지정할 수 있고, 비어 있으면 마감일의 요일로 반복한다.
 */
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
            throw new IllegalArgumentException("Repeat interval must be " + MIN_INTERVAL + " to " + MAX_INTERVAL + ": " + interval);
        }
        if (!daysOfWeek.isEmpty() && rule != RepeatRule.WEEKLY) {
            throw new IllegalArgumentException("Days of week can only be set for a weekly repeat: " + rule);
        }
        if (!rule.isRepeating()) {
            interval = MIN_INTERVAL;
        }
    }

    // 간격 1, 요일 지정 없이 주기만 정한다 (사용자 지정 반복 이전의 동작).
    public static Recurrence of(RepeatRule rule) {
        return new Recurrence(rule, MIN_INTERVAL, Set.of());
    }

    public boolean isRepeating() {
        return rule.isRepeating();
    }

    /**
     * 다음 회차의 마감일시를 계산한다. 시각은 그대로 둔다.
     * - 요일을 지정한 WEEKLY: 같은 주(월요일 시작)에서 마감일 뒤의 가장 가까운 지정 요일, 없으면 interval주 뒤 주의 첫 지정 요일.
     * - 그 밖에는 주기 단위로 interval만큼 더한다 (월말/윤년 처리는 {@link RepeatRule#nextDueAt}와 같다).
     */
    public LocalDateTime nextDueAt(LocalDateTime dueAt) {
        if (rule != RepeatRule.WEEKLY || daysOfWeek.isEmpty()) {
            return rule.nextDueAt(dueAt, interval);
        }
        int dueDay = dueAt.getDayOfWeek().getValue();
        return daysOfWeek.stream()
                .filter(day -> day.getValue() > dueDay)
                .findFirst() // EnumSet은 월요일부터 순서대로 돈다.
                .map(day -> dueAt.plusDays(day.getValue() - dueDay))
                .orElseGet(() -> {
                    LocalDateTime weekStart = dueAt.minusDays(dueDay - 1L);
                    int firstDay = daysOfWeek.iterator().next().getValue();
                    return weekStart.plusWeeks(interval).plusDays(firstDay - 1L);
                });
    }
}

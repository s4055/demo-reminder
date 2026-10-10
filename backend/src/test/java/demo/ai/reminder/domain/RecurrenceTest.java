package demo.ai.reminder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceTest {

    private static final LocalDateTime MONDAY_DUE = LocalDateTime.of(2026, 10, 5, 9, 30);
    private static final LocalDateTime WEDNESDAY_DUE = LocalDateTime.of(2026, 10, 7, 9, 30);
    private static final LocalDateTime FRIDAY_DUE = LocalDateTime.of(2026, 10, 9, 9, 30);
    private static final LocalDateTime SUNDAY_DUE = LocalDateTime.of(2026, 10, 11, 9, 30);
    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(MONDAY, FRIDAY);

    @Test
    @DisplayName("간격을 지정하면 일/주/월/년 단위로 간격만큼 뒤가 다음 마감일시다")
    void nextDueAt_addsIntervalPerUnit() {
        assertThat(new Recurrence(RepeatRule.DAILY, 3, null).nextDueAt(MONDAY_DUE))
                .isEqualTo(LocalDateTime.of(2026, 10, 8, 9, 30));
        assertThat(new Recurrence(RepeatRule.WEEKLY, 2, null).nextDueAt(MONDAY_DUE))
                .isEqualTo(LocalDateTime.of(2026, 10, 19, 9, 30));
        assertThat(new Recurrence(RepeatRule.MONTHLY, 2, null).nextDueAt(MONDAY_DUE))
                .isEqualTo(LocalDateTime.of(2026, 12, 5, 9, 30));
        assertThat(new Recurrence(RepeatRule.YEARLY, 3, null).nextDueAt(MONDAY_DUE))
                .isEqualTo(LocalDateTime.of(2029, 10, 5, 9, 30));
    }

    @Test
    @DisplayName("간격이 있는 월 반복도 그 달에 같은 날짜가 없으면 말일로 맞춘다")
    void nextDueAt_monthlyWithInterval_clampsToMonthEnd() {
        assertThat(new Recurrence(RepeatRule.MONTHLY, 2, null).nextDueAt(LocalDateTime.of(2026, 12, 31, 9, 0)))
                .isEqualTo(LocalDateTime.of(2027, 2, 28, 9, 0));
        assertThat(new Recurrence(RepeatRule.MONTHLY, 3, null).nextDueAt(LocalDateTime.of(2027, 11, 30, 9, 0)))
                .isEqualTo(LocalDateTime.of(2028, 2, 29, 9, 0));
    }

    @Test
    @DisplayName("2월 29일의 4년 간격 반복은 다음 윤년의 2월 29일, 1년 간격이면 평년의 2월 28일이다")
    void nextDueAt_yearlyFromLeapDay_dependsOnInterval() {
        LocalDateTime leapDay = LocalDateTime.of(2028, 2, 29, 9, 0);

        assertThat(new Recurrence(RepeatRule.YEARLY, 4, null).nextDueAt(leapDay))
                .isEqualTo(LocalDateTime.of(2032, 2, 29, 9, 0));
        assertThat(Recurrence.of(RepeatRule.YEARLY).nextDueAt(leapDay))
                .isEqualTo(LocalDateTime.of(2029, 2, 28, 9, 0));
    }

    @Test
    @DisplayName("2주마다 월·수 반복은 월요일 다음이 같은 주 수요일이고 시각은 유지된다")
    void nextDueAt_weeklyDays_returnsNextDayInSameWeek() {
        Recurrence recurrence = new Recurrence(RepeatRule.WEEKLY, 2, Set.of(WEDNESDAY, MONDAY));

        assertThat(recurrence.nextDueAt(MONDAY_DUE)).isEqualTo(WEDNESDAY_DUE);
    }

    @Test
    @DisplayName("2주마다 월·수 반복은 수요일 다음이 2주 뒤 월요일이다")
    void nextDueAt_weeklyDays_jumpsIntervalWeeksToFirstDay() {
        Recurrence recurrence = new Recurrence(RepeatRule.WEEKLY, 2, Set.of(MONDAY, WEDNESDAY));

        assertThat(recurrence.nextDueAt(WEDNESDAY_DUE)).isEqualTo(LocalDateTime.of(2026, 10, 19, 9, 30));
    }

    @Test
    @DisplayName("지정 요일이 아닌 날이 마감일이면 그 주의 다음 지정 요일, 없으면 간격만큼 뒤 주의 첫 지정 요일이다")
    void nextDueAt_weeklyDays_fromUnlistedDay() {
        Recurrence recurrence = new Recurrence(RepeatRule.WEEKLY, 2, Set.of(MONDAY, WEDNESDAY));

        assertThat(recurrence.nextDueAt(LocalDateTime.of(2026, 10, 6, 9, 30))) // 화요일
                .isEqualTo(WEDNESDAY_DUE);
        assertThat(recurrence.nextDueAt(FRIDAY_DUE)).isEqualTo(LocalDateTime.of(2026, 10, 19, 9, 30));
    }

    @Test
    @DisplayName("평일마다 반복은 금요일과 일요일 다음이 다음 주 월요일이다")
    void nextDueAt_weekdays_skipsWeekend() {
        Recurrence weekdays = new Recurrence(RepeatRule.WEEKLY, 1, WEEKDAYS);

        assertThat(weekdays.nextDueAt(MONDAY_DUE)).isEqualTo(LocalDateTime.of(2026, 10, 6, 9, 30));
        assertThat(weekdays.nextDueAt(FRIDAY_DUE)).isEqualTo(LocalDateTime.of(2026, 10, 12, 9, 30));
        assertThat(weekdays.nextDueAt(SUNDAY_DUE)).isEqualTo(LocalDateTime.of(2026, 10, 12, 9, 30));
    }

    @Test
    @DisplayName("간격이 1~99를 벗어나면 만들 수 없다")
    void constructor_throws_whenIntervalOutOfRange() {
        assertThatThrownBy(() -> new Recurrence(RepeatRule.DAILY, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Recurrence(RepeatRule.DAILY, 100, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Recurrence(RepeatRule.DAILY, 99, null).interval()).isEqualTo(99);
    }

    @Test
    @DisplayName("WEEKLY가 아닌데 요일을 지정하면 만들 수 없다")
    void constructor_throws_whenDaysOfWeekWithoutWeekly() {
        assertThatThrownBy(() -> new Recurrence(RepeatRule.DAILY, 1, Set.of(MONDAY)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Recurrence(RepeatRule.NONE, 1, Set.of(MONDAY)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주기를 생략하면 반복 안 함이고, 반복하지 않으면 간격은 1로 맞춘다")
    void constructor_normalizesNoneRule() {
        Recurrence none = new Recurrence(null, 5, null);

        assertThat(none.rule()).isEqualTo(RepeatRule.NONE);
        assertThat(none.interval()).isEqualTo(1);
        assertThat(none.daysOfWeek()).isEmpty();
        assertThat(none.isRepeating()).isFalse();
    }

    @Test
    @DisplayName("반복하지 않으면 다음 마감일시를 계산할 수 없다")
    void nextDueAt_none_throws() {
        assertThatThrownBy(() -> Recurrence.NONE.nextDueAt(MONDAY_DUE))
                .isInstanceOf(IllegalStateException.class);
    }
}

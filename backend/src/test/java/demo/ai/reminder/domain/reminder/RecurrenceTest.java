package demo.ai.reminder.domain.reminder;

import demo.ai.reminder.domain.common.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final Instant MONDAY_DUE = seoul(2026, 10, 5, 9, 30);
    private static final Instant WEDNESDAY_DUE = seoul(2026, 10, 7, 9, 30);
    private static final Instant FRIDAY_DUE = seoul(2026, 10, 9, 9, 30);
    private static final Instant SUNDAY_DUE = seoul(2026, 10, 11, 9, 30);
    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(MONDAY, FRIDAY);

    @Test
    @DisplayName("of는 간격이 없으면 1로 보고, 주어진 간격과 요일은 그대로 담는다")
    void of_defaultsMissingIntervalToMin() {
        assertThat(Recurrence.of(RepeatRule.WEEKLY, null, Set.of(MONDAY)))
                .isEqualTo(new Recurrence(RepeatRule.WEEKLY, Recurrence.MIN_INTERVAL, Set.of(MONDAY)));
        assertThat(Recurrence.of(RepeatRule.DAILY, 3, null)).isEqualTo(new Recurrence(RepeatRule.DAILY, 3, null));
        assertThat(Recurrence.of(null, null, null)).isEqualTo(Recurrence.NONE);
    }

    @Test
    @DisplayName("간격을 지정하면 일/주/월/년 단위로 간격만큼 뒤가 다음 마감일시다")
    void nextDueAt_addsIntervalPerUnit() {
        assertThat(new Recurrence(RepeatRule.DAILY, 3, null).nextDueAt(MONDAY_DUE, SEOUL))
                .isEqualTo(seoul(2026, 10, 8, 9, 30));
        assertThat(new Recurrence(RepeatRule.WEEKLY, 2, null).nextDueAt(MONDAY_DUE, SEOUL))
                .isEqualTo(seoul(2026, 10, 19, 9, 30));
        assertThat(new Recurrence(RepeatRule.MONTHLY, 2, null).nextDueAt(MONDAY_DUE, SEOUL))
                .isEqualTo(seoul(2026, 12, 5, 9, 30));
        assertThat(new Recurrence(RepeatRule.YEARLY, 3, null).nextDueAt(MONDAY_DUE, SEOUL))
                .isEqualTo(seoul(2029, 10, 5, 9, 30));
    }

    @Test
    @DisplayName("간격이 있는 월 반복도 그 달에 같은 날짜가 없으면 말일로 맞춘다")
    void nextDueAt_monthlyWithInterval_clampsToMonthEnd() {
        assertThat(new Recurrence(RepeatRule.MONTHLY, 2, null).nextDueAt(seoul(2026, 12, 31, 9, 0), SEOUL))
                .isEqualTo(seoul(2027, 2, 28, 9, 0));
        assertThat(new Recurrence(RepeatRule.MONTHLY, 3, null).nextDueAt(seoul(2027, 11, 30, 9, 0), SEOUL))
                .isEqualTo(seoul(2028, 2, 29, 9, 0));
    }

    @Test
    @DisplayName("2월 29일의 4년 간격 반복은 다음 윤년의 2월 29일, 1년 간격이면 평년의 2월 28일이다")
    void nextDueAt_yearlyFromLeapDay_dependsOnInterval() {
        Instant leapDay = seoul(2028, 2, 29, 9, 0);

        assertThat(new Recurrence(RepeatRule.YEARLY, 4, null).nextDueAt(leapDay, SEOUL))
                .isEqualTo(seoul(2032, 2, 29, 9, 0));
        assertThat(new Recurrence(RepeatRule.YEARLY, 1, null).nextDueAt(leapDay, SEOUL))
                .isEqualTo(seoul(2029, 2, 28, 9, 0));
    }

    @Test
    @DisplayName("2주마다 월·수 반복은 월요일 다음이 같은 주 수요일이고 시각은 유지된다")
    void nextDueAt_weeklyDays_returnsNextDayInSameWeek() {
        Recurrence recurrence = new Recurrence(RepeatRule.WEEKLY, 2, Set.of(WEDNESDAY, MONDAY));

        assertThat(recurrence.nextDueAt(MONDAY_DUE, SEOUL)).isEqualTo(WEDNESDAY_DUE);
    }

    @Test
    @DisplayName("2주마다 월·수 반복은 수요일 다음이 2주 뒤 월요일이다")
    void nextDueAt_weeklyDays_jumpsIntervalWeeksToFirstDay() {
        Recurrence recurrence = new Recurrence(RepeatRule.WEEKLY, 2, Set.of(MONDAY, WEDNESDAY));

        assertThat(recurrence.nextDueAt(WEDNESDAY_DUE, SEOUL)).isEqualTo(seoul(2026, 10, 19, 9, 30));
    }

    @Test
    @DisplayName("지정 요일이 아닌 날이 마감일이면 그 주의 다음 지정 요일, 없으면 간격만큼 뒤 주의 첫 지정 요일이다")
    void nextDueAt_weeklyDays_fromUnlistedDay() {
        Recurrence recurrence = new Recurrence(RepeatRule.WEEKLY, 2, Set.of(MONDAY, WEDNESDAY));

        assertThat(recurrence.nextDueAt(seoul(2026, 10, 6, 9, 30), SEOUL))
                .isEqualTo(WEDNESDAY_DUE);
        assertThat(recurrence.nextDueAt(FRIDAY_DUE, SEOUL)).isEqualTo(seoul(2026, 10, 19, 9, 30));
    }

    @Test
    @DisplayName("평일마다 반복은 금요일과 일요일 다음이 다음 주 월요일이다")
    void nextDueAt_weekdays_skipsWeekend() {
        Recurrence weekdays = new Recurrence(RepeatRule.WEEKLY, 1, WEEKDAYS);

        assertThat(weekdays.nextDueAt(MONDAY_DUE, SEOUL)).isEqualTo(seoul(2026, 10, 6, 9, 30));
        assertThat(weekdays.nextDueAt(FRIDAY_DUE, SEOUL)).isEqualTo(seoul(2026, 10, 12, 9, 30));
        assertThat(weekdays.nextDueAt(SUNDAY_DUE, SEOUL)).isEqualTo(seoul(2026, 10, 12, 9, 30));
    }

    @Test
    @DisplayName("요일은 UTC가 아니라 리마인더 시간대의 날짜 기준으로 판단한다")
    void nextDueAt_weeklyDays_usesZoneLocalDayOfWeek() {
        Recurrence recurrence = new Recurrence(RepeatRule.WEEKLY, 1, Set.of(MONDAY, WEDNESDAY));
        Instant mondayMorningInSeoul = seoul(2026, 10, 5, 7, 0);

        assertThat(mondayMorningInSeoul).isEqualTo(Instant.parse("2026-10-04T22:00:00Z"));
        assertThat(recurrence.nextDueAt(mondayMorningInSeoul, SEOUL)).isEqualTo(seoul(2026, 10, 7, 7, 0));
    }

    @Test
    @DisplayName("서머타임이 시작되는 날에도 매일 반복은 같은 현지 시각을 유지한다")
    void nextDueAt_daily_keepsLocalTimeAcrossDaylightSavingStart() {
        Instant beforeTransition = newYork(2026, 3, 7, 9, 0);

        Instant next = new Recurrence(RepeatRule.DAILY, 1, null).nextDueAt(beforeTransition, NEW_YORK);

        assertThat(next).isEqualTo(newYork(2026, 3, 8, 9, 0));
        assertThat(Duration.between(beforeTransition, next)).isEqualTo(Duration.ofHours(23));
    }

    @Test
    @DisplayName("서머타임이 끝나는 주를 넘어도 매주·매월 반복은 같은 현지 시각을 유지한다")
    void nextDueAt_weeklyAndMonthly_keepLocalTimeAcrossDaylightSavingEnd() {
        Instant duringDaylightSaving = newYork(2026, 10, 30, 9, 0);

        assertThat(new Recurrence(RepeatRule.WEEKLY, 1, null).nextDueAt(duringDaylightSaving, NEW_YORK))
                .isEqualTo(newYork(2026, 11, 6, 9, 0))
                .isEqualTo(Instant.parse("2026-11-06T14:00:00Z"));
        assertThat(new Recurrence(RepeatRule.MONTHLY, 1, null).nextDueAt(duringDaylightSaving, NEW_YORK))
                .isEqualTo(newYork(2026, 11, 30, 9, 0));
    }

    @Test
    @DisplayName("간격이 1~99를 벗어나면 만들 수 없다")
    void constructor_throws_whenIntervalOutOfRange() {
        assertThatThrownBy(() -> new Recurrence(RepeatRule.DAILY, 0, null))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new Recurrence(RepeatRule.DAILY, 100, null))
                .isInstanceOf(DomainException.class);
        assertThat(new Recurrence(RepeatRule.DAILY, 99, null).interval()).isEqualTo(99);
    }

    @Test
    @DisplayName("WEEKLY가 아닌데 요일을 지정하면 만들 수 없다")
    void constructor_throws_whenDaysOfWeekWithoutWeekly() {
        assertThatThrownBy(() -> new Recurrence(RepeatRule.DAILY, 1, Set.of(MONDAY)))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new Recurrence(RepeatRule.NONE, 1, Set.of(MONDAY)))
                .isInstanceOf(DomainException.class);
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
        assertThatThrownBy(() -> Recurrence.NONE.nextDueAt(MONDAY_DUE, SEOUL))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Instant seoul(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(SEOUL).toInstant();
    }

    private static Instant newYork(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(NEW_YORK).toInstant();
    }
}

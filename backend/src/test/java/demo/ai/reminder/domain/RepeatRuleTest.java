package demo.ai.reminder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepeatRuleTest {

    private static final LocalDateTime DUE_AT = LocalDateTime.of(2026, 10, 4, 9, 30);

    @Test
    @DisplayName("매일 반복이면 다음 마감일시는 하루 뒤 같은 시각이다")
    void nextDueAt_daily_addsOneDay() {
        assertThat(RepeatRule.DAILY.nextDueAt(DUE_AT)).isEqualTo(LocalDateTime.of(2026, 10, 5, 9, 30));
    }

    @Test
    @DisplayName("매주 반복이면 다음 마감일시는 7일 뒤 같은 시각이다")
    void nextDueAt_weekly_addsSevenDays() {
        assertThat(RepeatRule.WEEKLY.nextDueAt(DUE_AT)).isEqualTo(LocalDateTime.of(2026, 10, 11, 9, 30));
    }

    @Test
    @DisplayName("매월 반복이면 다음 마감일시는 다음 달 같은 날짜다")
    void nextDueAt_monthly_addsOneMonth() {
        assertThat(RepeatRule.MONTHLY.nextDueAt(DUE_AT)).isEqualTo(LocalDateTime.of(2026, 11, 4, 9, 30));
    }

    @Test
    @DisplayName("매년 반복이면 다음 마감일시는 다음 해 같은 날짜다")
    void nextDueAt_yearly_addsOneYear() {
        assertThat(RepeatRule.YEARLY.nextDueAt(DUE_AT)).isEqualTo(LocalDateTime.of(2027, 10, 4, 9, 30));
    }

    @Test
    @DisplayName("매월 반복에서 다음 달에 같은 날짜가 없으면 다음 달 말일로 맞춘다")
    void nextDueAt_monthly_fromMonthEnd_clampsToLastDayOfNextMonth() {
        assertThat(RepeatRule.MONTHLY.nextDueAt(LocalDateTime.of(2026, 1, 31, 9, 0)))
                .isEqualTo(LocalDateTime.of(2026, 2, 28, 9, 0));
        assertThat(RepeatRule.MONTHLY.nextDueAt(LocalDateTime.of(2026, 3, 31, 9, 0)))
                .isEqualTo(LocalDateTime.of(2026, 4, 30, 9, 0));
    }

    @Test
    @DisplayName("윤년에는 1월 31일의 다음 달 반복이 2월 29일이다")
    void nextDueAt_monthly_inLeapYear_clampsToFebruary29() {
        assertThat(RepeatRule.MONTHLY.nextDueAt(LocalDateTime.of(2028, 1, 31, 9, 0)))
                .isEqualTo(LocalDateTime.of(2028, 2, 29, 9, 0));
    }

    @Test
    @DisplayName("매년 반복에서 2월 29일의 다음 회차는 평년의 2월 28일이다")
    void nextDueAt_yearly_fromLeapDay_clampsToFebruary28() {
        assertThat(RepeatRule.YEARLY.nextDueAt(LocalDateTime.of(2028, 2, 29, 9, 0)))
                .isEqualTo(LocalDateTime.of(2029, 2, 28, 9, 0));
    }

    @Test
    @DisplayName("매일 반복은 연말을 넘어 다음 해로 이어진다")
    void nextDueAt_daily_crossesYearEnd() {
        assertThat(RepeatRule.DAILY.nextDueAt(LocalDateTime.of(2026, 12, 31, 23, 0)))
                .isEqualTo(LocalDateTime.of(2027, 1, 1, 23, 0));
    }

    @Test
    @DisplayName("반복하지 않으면 다음 마감일시를 계산할 수 없다")
    void nextDueAt_none_throws() {
        assertThatThrownBy(() -> RepeatRule.NONE.nextDueAt(DUE_AT))
                .isInstanceOf(IllegalStateException.class);
    }
}

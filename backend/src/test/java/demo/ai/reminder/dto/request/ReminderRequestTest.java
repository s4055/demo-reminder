package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.DomainException;
import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.service.command.CreateReminderCommand;
import demo.ai.reminder.service.command.UpdateReminderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderRequestTest {

    private static final LocalDateTime DUE_AT = LocalDateTime.of(2026, 10, 5, 9, 30);

    @Test
    @DisplayName("생성 요청은 반복 값을 Recurrence로 묶은 Command로 바뀌고, 간격을 생략하면 1이다")
    void toCommand_bundlesRecurrence_andDefaultsIntervalToOne() {
        ReminderRequest request = new ReminderRequest("우유 사기", "저지방", 3L, DUE_AT, Priority.HIGH,
                List.of("장보기"), 7L, RepeatRule.WEEKLY, null, Set.of(DayOfWeek.MONDAY));

        CreateReminderCommand command = request.toCommand();

        assertThat(command).isEqualTo(new CreateReminderCommand("우유 사기", "저지방", 3L, DUE_AT, Priority.HIGH,
                List.of("장보기"), 7L, new Recurrence(RepeatRule.WEEKLY, 1, Set.of(DayOfWeek.MONDAY))));
    }

    @Test
    @DisplayName("반복을 지정하지 않으면 반복하지 않는 Recurrence가 된다")
    void toCommand_withoutRepeat_isNotRepeating() {
        ReminderRequest request = new ReminderRequest("우유 사기", null, null, null, null, null, null, null, null, null);

        assertThat(request.toCommand().recurrence()).isEqualTo(Recurrence.NONE);
    }

    @Test
    @DisplayName("수정 요청도 같은 규칙으로 Command가 되고 버전을 그대로 담는다")
    void updateRequest_toCommand_bundlesRecurrenceAndKeepsVersion() {
        ReminderUpdateRequest request = new ReminderUpdateRequest("우유 사기", null, DUE_AT, true, null, null,
                RepeatRule.DAILY, 3, null, 5L);

        UpdateReminderCommand command = request.toCommand();

        assertThat(command.recurrence()).isEqualTo(new Recurrence(RepeatRule.DAILY, 3, null));
        assertThat(command.flagged()).isTrue();
        assertThat(command.version()).isEqualTo(5L);
    }

    @Test
    @DisplayName("WEEKLY가 아닌데 요일을 지정하면 도메인 예외가 발생한다")
    void toCommand_throws_whenDaysOfWeekWithoutWeekly() {
        ReminderRequest request = new ReminderRequest("우유 사기", null, null, DUE_AT, null, null, null,
                RepeatRule.DAILY, null, Set.of(DayOfWeek.MONDAY));

        assertThatThrownBy(request::toCommand).isInstanceOf(DomainException.class);
    }
}

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
    @DisplayName("반복과 태그를 지정하지 않으면 반복하지 않는 Recurrence와 빈 태그 목록이 된다")
    void toCommand_withoutRepeatAndTags_isNotRepeating_andHasNoTags() {
        ReminderRequest request = new ReminderRequest("우유 사기", null, null, null, null, null, null, null, null, null);

        CreateReminderCommand command = request.toCommand();

        assertThat(command.recurrence()).isEqualTo(Recurrence.NONE);
        assertThat(command.tagNames()).isEmpty();
    }

    @Test
    @DisplayName("생성·수정 요청은 제목과 태그 이름의 앞뒤 공백을 지운다")
    void toCommand_stripsTitleAndTagNames() {
        ReminderRequest create = new ReminderRequest("  우유 사기 ", " 저지방 ", null, null, null,
                List.of(" 장보기 ", "집"), null, null, null, null);
        ReminderUpdateRequest update = new ReminderUpdateRequest(" 계란 사기 ", null, null, false, null,
                List.of(" 장보기 "), null, null, null, 0L);

        assertThat(create.toCommand().title()).isEqualTo("우유 사기");
        assertThat(create.toCommand().memo()).isEqualTo(" 저지방 ");
        assertThat(create.toCommand().tagNames()).containsExactly("장보기", "집");
        assertThat(update.toCommand().title()).isEqualTo("계란 사기");
        assertThat(update.toCommand().tagNames()).containsExactly("장보기");
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

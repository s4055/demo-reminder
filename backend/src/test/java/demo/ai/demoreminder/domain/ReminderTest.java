package demo.ai.demoreminder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderTest {

    @Test
    @DisplayName("생성자로 만들면 제목과 메모가 설정되고 미완료 상태다")
    void constructor_setsTitleAndMemo_andDefaultsToIncomplete() {
        Reminder reminder = new Reminder("우유 사기", "저지방", null, null);

        assertThat(reminder.getTitle()).isEqualTo("우유 사기");
        assertThat(reminder.getMemo()).isEqualTo("저지방");
        assertThat(reminder.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("메모 없이도 생성할 수 있다")
    void constructor_allowsNullMemo() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        assertThat(reminder.getMemo()).isNull();
    }

    @Test
    @DisplayName("생성 직후에는 생성일과 수정일이 비어있다")
    void constructor_leavesTimestampsNullUntilPersisted() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        assertThat(reminder.getCreatedAt()).isNull();
        assertThat(reminder.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("toggleComplete를 호출하면 완료 상태가 반전된다")
    void toggleComplete_flipsCompletedState() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        reminder.toggleComplete();

        assertThat(reminder.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("생성자로 만들면 마감일시가 설정되고 플래그는 꺼져 있다")
    void constructor_setsDueAt_andDefaultsToNotFlagged() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 9, 20, 18, 0);

        Reminder reminder = new Reminder("우유 사기", null, null, dueAt);

        assertThat(reminder.getDueAt()).isEqualTo(dueAt);
        assertThat(reminder.isFlagged()).isFalse();
    }

    @Test
    @DisplayName("마감일시 없이도 생성할 수 있다")
    void constructor_allowsNullDueAt() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        assertThat(reminder.getDueAt()).isNull();
    }

    @Test
    @DisplayName("toggleFlag를 호출하면 플래그 상태가 반전된다")
    void toggleFlag_flipsFlaggedState() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        reminder.toggleFlag();

        assertThat(reminder.isFlagged()).isTrue();
    }

    @Test
    @DisplayName("toggleFlag를 두 번 호출하면 원래 상태로 돌아온다")
    void toggleFlag_calledTwice_restoresOriginalState() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        reminder.toggleFlag();
        reminder.toggleFlag();

        assertThat(reminder.isFlagged()).isFalse();
    }

    @Test
    @DisplayName("toggleComplete를 두 번 호출하면 원래 상태로 돌아온다")
    void toggleComplete_calledTwice_restoresOriginalState() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        reminder.toggleComplete();
        reminder.toggleComplete();

        assertThat(reminder.isCompleted()).isFalse();
    }
}

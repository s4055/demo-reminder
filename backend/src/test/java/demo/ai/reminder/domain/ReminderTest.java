package demo.ai.reminder.domain;

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

        reminder.toggleComplete(LocalDateTime.now());

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
    @DisplayName("update를 호출하면 제목, 메모, 마감일시, 플래그가 변경되고 완료 상태는 유지된다")
    void update_changesEditableFields_andKeepsCompletedState() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);
        reminder.toggleComplete(LocalDateTime.now());
        LocalDateTime dueAt = LocalDateTime.of(2026, 9, 21, 9, 0);

        reminder.update("계란 사기", "12구", dueAt, true, Priority.NONE);

        assertThat(reminder.getTitle()).isEqualTo("계란 사기");
        assertThat(reminder.getMemo()).isEqualTo("12구");
        assertThat(reminder.getDueAt()).isEqualTo(dueAt);
        assertThat(reminder.isFlagged()).isTrue();
        assertThat(reminder.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("update로 메모와 마감일시를 null로 비울 수 있다")
    void update_allowsClearingMemoAndDueAt() {
        Reminder reminder = new Reminder("우유 사기", "저지방", null, LocalDateTime.of(2026, 9, 21, 9, 0));

        reminder.update("우유 사기", null, null, false, Priority.NONE);

        assertThat(reminder.getMemo()).isNull();
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
    @DisplayName("생성 직후에는 완료일시가 비어있다")
    void constructor_leavesCompletedAtNull() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        assertThat(reminder.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("완료 처리하면 전달받은 시각이 완료일시로 기록된다")
    void toggleComplete_whenCompleting_recordsCompletedAt() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 10, 0);

        reminder.toggleComplete(now);

        assertThat(reminder.getCompletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("완료를 취소하면 완료일시가 비워진다")
    void toggleComplete_whenUncompleting_clearsCompletedAt() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);
        reminder.toggleComplete(LocalDateTime.of(2026, 9, 30, 10, 0));

        reminder.toggleComplete(LocalDateTime.of(2026, 9, 30, 11, 0));

        assertThat(reminder.isCompleted()).isFalse();
        assertThat(reminder.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("update는 완료일시를 바꾸지 않는다")
    void update_keepsCompletedAt() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);
        LocalDateTime completedAt = LocalDateTime.of(2026, 9, 30, 10, 0);
        reminder.toggleComplete(completedAt);

        reminder.update("계란 사기", null, null, false, Priority.NONE);

        assertThat(reminder.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("toggleComplete를 두 번 호출하면 원래 상태로 돌아온다")
    void toggleComplete_calledTwice_restoresOriginalState() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        reminder.toggleComplete(LocalDateTime.now());
        reminder.toggleComplete(LocalDateTime.now());

        assertThat(reminder.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("우선순위를 지정하지 않고 생성하면 우선순위는 없음(NONE)이다")
    void constructor_defaultsPriorityToNone() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        assertThat(reminder.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("우선순위를 지정해 생성하면 해당 우선순위가 설정된다")
    void constructor_setsGivenPriority() {
        Reminder reminder = new Reminder("우유 사기", null, null, null, Priority.HIGH);

        assertThat(reminder.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("우선순위를 null로 생성하면 없음(NONE)으로 설정된다")
    void constructor_treatsNullPriorityAsNone() {
        Reminder reminder = new Reminder("우유 사기", null, null, null, null);

        assertThat(reminder.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("update로 우선순위를 변경할 수 있다")
    void update_changesPriority() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        reminder.update("우유 사기", null, null, false, Priority.MEDIUM);

        assertThat(reminder.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("update에 우선순위를 null로 넘기면 없음(NONE)으로 바뀐다")
    void update_treatsNullPriorityAsNone() {
        Reminder reminder = new Reminder("우유 사기", null, null, null, Priority.HIGH);

        reminder.update("우유 사기", null, null, false, null);

        assertThat(reminder.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("생성 직후 표시 순서는 0이다")
    void constructor_defaultsSortOrderToZero() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        assertThat(reminder.getSortOrder()).isZero();
    }

    @Test
    @DisplayName("changeSortOrder를 호출하면 표시 순서가 변경된다")
    void changeSortOrder_changesSortOrder() {
        Reminder reminder = new Reminder("우유 사기", null, null, null);

        reminder.changeSortOrder(5);

        assertThat(reminder.getSortOrder()).isEqualTo(5);
    }
}

package demo.ai.reminder.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderTest {

    private final User owner = new User("owner@example.com", "password", "테스터");

    @Test
    @DisplayName("생성자로 만들면 제목과 메모가 설정되고 미완료 상태다")
    void constructor_setsTitleAndMemo_andDefaultsToIncomplete() {
        Reminder reminder = new Reminder(owner, "우유 사기", "저지방", null, null);

        assertThat(reminder.getTitle()).isEqualTo("우유 사기");
        assertThat(reminder.getMemo()).isEqualTo("저지방");
        assertThat(reminder.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("메모 없이도 생성할 수 있다")
    void constructor_allowsNullMemo() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThat(reminder.getMemo()).isNull();
    }

    @Test
    @DisplayName("생성 직후에는 생성일과 수정일이 비어있다")
    void constructor_leavesTimestampsNullUntilPersisted() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThat(reminder.getCreatedAt()).isNull();
        assertThat(reminder.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("toggleComplete를 호출하면 완료 상태가 반전된다")
    void toggleComplete_flipsCompletedState() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        reminder.toggleComplete(LocalDateTime.now());

        assertThat(reminder.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("생성자로 만들면 마감일시가 설정되고 플래그는 꺼져 있다")
    void constructor_setsDueAt_andDefaultsToNotFlagged() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 9, 20, 18, 0);

        Reminder reminder = new Reminder(owner, "우유 사기", null, null, dueAt);

        assertThat(reminder.getDueAt()).isEqualTo(dueAt);
        assertThat(reminder.isFlagged()).isFalse();
    }

    @Test
    @DisplayName("마감일시 없이도 생성할 수 있다")
    void constructor_allowsNullDueAt() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThat(reminder.getDueAt()).isNull();
    }

    @Test
    @DisplayName("update를 호출하면 제목, 메모, 마감일시, 플래그가 변경되고 완료 상태는 유지된다")
    void update_changesEditableFields_andKeepsCompletedState() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);
        reminder.toggleComplete(LocalDateTime.now());
        LocalDateTime dueAt = LocalDateTime.of(2026, 9, 21, 9, 0);

        reminder.update("계란 사기", "12구", dueAt, true, Priority.NONE, null);

        assertThat(reminder.getTitle()).isEqualTo("계란 사기");
        assertThat(reminder.getMemo()).isEqualTo("12구");
        assertThat(reminder.getDueAt()).isEqualTo(dueAt);
        assertThat(reminder.isFlagged()).isTrue();
        assertThat(reminder.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("update로 메모와 마감일시를 null로 비울 수 있다")
    void update_allowsClearingMemoAndDueAt() {
        Reminder reminder = new Reminder(owner, "우유 사기", "저지방", null, LocalDateTime.of(2026, 9, 21, 9, 0));

        reminder.update("우유 사기", null, null, false, Priority.NONE, null);

        assertThat(reminder.getMemo()).isNull();
        assertThat(reminder.getDueAt()).isNull();
    }

    @Test
    @DisplayName("toggleFlag를 호출하면 플래그 상태가 반전된다")
    void toggleFlag_flipsFlaggedState() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        reminder.toggleFlag();

        assertThat(reminder.isFlagged()).isTrue();
    }

    @Test
    @DisplayName("toggleFlag를 두 번 호출하면 원래 상태로 돌아온다")
    void toggleFlag_calledTwice_restoresOriginalState() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        reminder.toggleFlag();
        reminder.toggleFlag();

        assertThat(reminder.isFlagged()).isFalse();
    }

    @Test
    @DisplayName("생성 직후에는 완료일시가 비어있다")
    void constructor_leavesCompletedAtNull() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThat(reminder.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("완료 처리하면 전달받은 시각이 완료일시로 기록된다")
    void toggleComplete_whenCompleting_recordsCompletedAt() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 10, 0);

        reminder.toggleComplete(now);

        assertThat(reminder.getCompletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("완료를 취소하면 완료일시가 비워진다")
    void toggleComplete_whenUncompleting_clearsCompletedAt() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);
        reminder.toggleComplete(LocalDateTime.of(2026, 9, 30, 10, 0));

        reminder.toggleComplete(LocalDateTime.of(2026, 9, 30, 11, 0));

        assertThat(reminder.isCompleted()).isFalse();
        assertThat(reminder.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("update는 완료일시를 바꾸지 않는다")
    void update_keepsCompletedAt() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);
        LocalDateTime completedAt = LocalDateTime.of(2026, 9, 30, 10, 0);
        reminder.toggleComplete(completedAt);

        reminder.update("계란 사기", null, null, false, Priority.NONE, null);

        assertThat(reminder.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("toggleComplete를 두 번 호출하면 원래 상태로 돌아온다")
    void toggleComplete_calledTwice_restoresOriginalState() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        reminder.toggleComplete(LocalDateTime.now());
        reminder.toggleComplete(LocalDateTime.now());

        assertThat(reminder.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("우선순위를 지정하지 않고 생성하면 우선순위는 없음(NONE)이다")
    void constructor_defaultsPriorityToNone() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThat(reminder.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("우선순위를 지정해 생성하면 해당 우선순위가 설정된다")
    void constructor_setsGivenPriority() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null, Priority.HIGH);

        assertThat(reminder.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("우선순위를 null로 생성하면 없음(NONE)으로 설정된다")
    void constructor_treatsNullPriorityAsNone() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null, null);

        assertThat(reminder.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("update로 우선순위를 변경할 수 있다")
    void update_changesPriority() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        reminder.update("우유 사기", null, null, false, Priority.MEDIUM, null);

        assertThat(reminder.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("update에 우선순위를 null로 넘기면 없음(NONE)으로 바뀐다")
    void update_treatsNullPriorityAsNone() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null, Priority.HIGH);

        reminder.update("우유 사기", null, null, false, null, null);

        assertThat(reminder.getPriority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("생성 직후 표시 순서는 0이다")
    void constructor_defaultsSortOrderToZero() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThat(reminder.getSortOrder()).isZero();
    }

    @Test
    @DisplayName("changeSortOrder를 호출하면 표시 순서가 변경된다")
    void changeSortOrder_changesSortOrder() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        reminder.changeSortOrder(5);

        assertThat(reminder.getSortOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("생성 직후에는 태그가 없다")
    void constructor_hasNoTags() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThat(reminder.getTags()).isEmpty();
    }

    @Test
    @DisplayName("replaceTags를 호출하면 태그가 주어진 목록으로 교체된다")
    void replaceTags_replacesAllTags() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);
        Tag home = new Tag(owner, "집");
        Tag errand = new Tag(owner, "심부름");
        Tag urgent = new Tag(owner, "급함");
        reminder.replaceTags(List.of(home, errand));

        reminder.replaceTags(List.of(urgent));

        assertThat(reminder.getTags()).containsExactly(urgent);
    }

    @Test
    @DisplayName("removeTag를 호출하면 해당 태그만 떨어진다")
    void removeTag_removesOnlyGivenTag() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);
        Tag home = new Tag(owner, "집");
        Tag errand = new Tag(owner, "심부름");
        reminder.replaceTags(List.of(home, errand));

        reminder.removeTag(home);

        assertThat(reminder.getTags()).containsExactly(errand);
    }

    @Test
    @DisplayName("getTags로 받은 태그 목록은 직접 수정할 수 없다")
    void getTags_isUnmodifiable() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null);

        assertThatThrownBy(() -> reminder.getTags().add(new Tag(owner, "집")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("생성 직후에는 최상위 리마인더이고 하위 작업이 없다")
    void constructor_isTopLevel_andHasNoSubtasks() {
        Reminder reminder = new Reminder(owner, "이사 준비", null, null, null);

        assertThat(reminder.isSubtask()).isFalse();
        assertThat(reminder.getParent()).isNull();
        assertThat(reminder.getSubtasks()).isEmpty();
    }

    @Test
    @DisplayName("addSubtask로 추가한 하위 작업은 부모와 같은 리스트에 속하고 부모 안에서 0부터 순서가 매겨진다")
    void addSubtask_setsParentAndListOfParent_andAssignsSortOrderWithinParent() {
        ReminderList home = new ReminderList(owner, "집", null);
        ReminderList work = new ReminderList(owner, "업무", null);
        Reminder parent = new Reminder(owner, "이사 준비", null, home, null);
        parent.changeSortOrder(7);
        Reminder boxes = new Reminder(owner, "박스 구하기", null, work, null);
        Reminder movers = new Reminder(owner, "이삿짐센터 예약", null, null, null);

        parent.addSubtask(boxes);
        parent.addSubtask(movers);

        assertThat(parent.getSubtasks()).containsExactly(boxes, movers);
        assertThat(boxes.getParent()).isSameAs(parent);
        assertThat(boxes.isSubtask()).isTrue();
        assertThat(boxes.getList()).isSameAs(home);
        assertThat(movers.getList()).isSameAs(home);
        assertThat(boxes.getSortOrder()).isZero();
        assertThat(movers.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("하위 작업에는 하위 작업을 추가할 수 없다")
    void addSubtask_throws_whenReminderIsSubtask() {
        Reminder parent = new Reminder(owner, "이사 준비", null, null, null);
        Reminder subtask = new Reminder(owner, "박스 구하기", null, null, null);
        parent.addSubtask(subtask);

        assertThatThrownBy(() -> subtask.addSubtask(new Reminder(owner, "테이프 사기", null, null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("removeSubtask를 호출하면 해당 하위 작업만 목록에서 빠진다")
    void removeSubtask_removesOnlyGivenSubtask() {
        Reminder parent = new Reminder(owner, "이사 준비", null, null, null);
        Reminder boxes = new Reminder(owner, "박스 구하기", null, null, null);
        Reminder movers = new Reminder(owner, "이삿짐센터 예약", null, null, null);
        parent.addSubtask(boxes);
        parent.addSubtask(movers);

        parent.removeSubtask(boxes);

        assertThat(parent.getSubtasks()).containsExactly(movers);
    }

    @Test
    @DisplayName("moveTo를 호출하면 리스트와 순서가 바뀌고 하위 작업도 같은 리스트로 함께 이동한다")
    void moveTo_changesListAndSortOrder_andMovesSubtasksTogether() {
        ReminderList home = new ReminderList(owner, "집", null);
        ReminderList work = new ReminderList(owner, "업무", null);
        Reminder parent = new Reminder(owner, "이사 준비", null, home, null);
        Reminder boxes = new Reminder(owner, "박스 구하기", null, null, null);
        Reminder movers = new Reminder(owner, "이삿짐센터 예약", null, null, null);
        parent.addSubtask(boxes);
        parent.addSubtask(movers);

        parent.moveTo(work, 5);

        assertThat(parent.getList()).isSameAs(work);
        assertThat(parent.getSortOrder()).isEqualTo(5);
        assertThat(boxes.getList()).isSameAs(work);
        assertThat(movers.getList()).isSameAs(work);
        assertThat(boxes.getSortOrder()).isZero();
        assertThat(movers.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("하위 작업은 단독으로 moveTo할 수 없다")
    void moveTo_throws_whenReminderIsSubtask() {
        ReminderList home = new ReminderList(owner, "집", null);
        ReminderList work = new ReminderList(owner, "업무", null);
        Reminder parent = new Reminder(owner, "이사 준비", null, home, null);
        Reminder subtask = new Reminder(owner, "박스 구하기", null, null, null);
        parent.addSubtask(subtask);

        assertThatThrownBy(() -> subtask.moveTo(work, 0))
                .isInstanceOf(IllegalStateException.class);
        assertThat(subtask.getList()).isSameAs(home);
    }

    @Test
    @DisplayName("getSubtasks로 받은 하위 작업 목록은 직접 수정할 수 없다")
    void getSubtasks_isUnmodifiable() {
        Reminder parent = new Reminder(owner, "이사 준비", null, null, null);

        assertThatThrownBy(() -> parent.getSubtasks().add(new Reminder(owner, "박스 구하기", null, null, null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("부모를 완료하면 미완료 하위 작업도 같은 시각으로 완료되고, 이미 완료된 하위 작업의 완료일시는 유지된다")
    void toggleComplete_whenCompletingParent_completesIncompleteSubtasks() {
        Reminder parent = new Reminder(owner, "이사 준비", null, null, null);
        Reminder boxes = new Reminder(owner, "박스 구하기", null, null, null);
        Reminder movers = new Reminder(owner, "이삿짐센터 예약", null, null, null);
        parent.addSubtask(boxes);
        parent.addSubtask(movers);
        LocalDateTime earlier = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 10, 0);
        boxes.toggleComplete(earlier);

        parent.toggleComplete(now);

        assertThat(parent.isCompleted()).isTrue();
        assertThat(movers.isCompleted()).isTrue();
        assertThat(movers.getCompletedAt()).isEqualTo(now);
        assertThat(boxes.getCompletedAt()).isEqualTo(earlier);
    }

    @Test
    @DisplayName("부모의 완료를 취소해도 하위 작업의 완료 상태는 바뀌지 않는다")
    void toggleComplete_whenUncompletingParent_keepsSubtasksCompleted() {
        Reminder parent = new Reminder(owner, "이사 준비", null, null, null);
        Reminder boxes = new Reminder(owner, "박스 구하기", null, null, null);
        parent.addSubtask(boxes);
        parent.toggleComplete(LocalDateTime.of(2026, 10, 3, 10, 0));

        parent.toggleComplete(LocalDateTime.of(2026, 10, 3, 11, 0));

        assertThat(parent.isCompleted()).isFalse();
        assertThat(boxes.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("하위 작업을 완료해도 부모의 완료 상태는 바뀌지 않는다")
    void toggleComplete_whenCompletingSubtask_doesNotCompleteParent() {
        Reminder parent = new Reminder(owner, "이사 준비", null, null, null);
        Reminder boxes = new Reminder(owner, "박스 구하기", null, null, null);
        parent.addSubtask(boxes);

        boxes.toggleComplete(LocalDateTime.of(2026, 10, 3, 10, 0));

        assertThat(boxes.isCompleted()).isTrue();
        assertThat(parent.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("반복을 지정하지 않으면 '반복 안 함'으로 생성된다")
    void constructor_defaultsRepeatRuleToNone() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, null, Priority.NONE, null);

        assertThat(reminder.getRepeatRule()).isEqualTo(RepeatRule.NONE);
    }

    @Test
    @DisplayName("마감일시 없이 반복을 설정하면 예외가 발생한다")
    void constructorAndUpdate_rejectRepeatWithoutDueAt() {
        assertThatThrownBy(() -> new Reminder(owner, "우유 사기", null, null, null, Priority.NONE, RepeatRule.DAILY))
                .isInstanceOf(IllegalArgumentException.class);

        Reminder reminder = new Reminder(owner, "우유 사기", null, null, LocalDateTime.of(2026, 10, 4, 9, 0));
        assertThatThrownBy(() -> reminder.update("우유 사기", null, null, false, Priority.NONE, RepeatRule.WEEKLY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("반복 리마인더를 완료하면 다음 마감일시로 내용을 복사한 다음 회차를 돌려준다")
    void toggleComplete_whenRepeating_returnsNextOccurrenceWithCopiedFields() {
        ReminderList home = new ReminderList(owner, "집", null);
        Reminder reminder = new Reminder(owner, "분리수거", "캔/페트", home, LocalDateTime.of(2026, 10, 4, 20, 0),
                Priority.HIGH, RepeatRule.WEEKLY);
        reminder.toggleFlag();
        reminder.replaceTags(List.of(new Tag(owner, "집안일")));

        Optional<Reminder> next = reminder.toggleComplete(LocalDateTime.of(2026, 10, 4, 21, 0));

        assertThat(reminder.isCompleted()).isTrue();
        assertThat(next).hasValueSatisfying(occurrence -> {
            assertThat(occurrence.getTitle()).isEqualTo("분리수거");
            assertThat(occurrence.getMemo()).isEqualTo("캔/페트");
            assertThat(occurrence.getList()).isSameAs(home);
            assertThat(occurrence.getDueAt()).isEqualTo(LocalDateTime.of(2026, 10, 11, 20, 0));
            assertThat(occurrence.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(occurrence.isFlagged()).isTrue();
            assertThat(occurrence.getRepeatRule()).isEqualTo(RepeatRule.WEEKLY);
            assertThat(occurrence.getTags()).extracting(Tag::getName).containsExactly("집안일");
            assertThat(occurrence.isCompleted()).isFalse();
            assertThat(occurrence.getCompletedAt()).isNull();
        });
    }

    @Test
    @DisplayName("반복하지 않는 리마인더를 완료하면 다음 회차가 없다")
    void toggleComplete_whenNotRepeating_returnsEmpty() {
        Reminder reminder = new Reminder(owner, "우유 사기", null, null, LocalDateTime.of(2026, 10, 4, 9, 0));

        assertThat(reminder.toggleComplete(LocalDateTime.of(2026, 10, 4, 10, 0))).isEmpty();
    }

    @Test
    @DisplayName("반복 리마인더의 완료를 취소했다가 다시 완료해도 다음 회차는 한 번만 만든다")
    void toggleComplete_whenRecompleting_doesNotCreateNextOccurrenceAgain() {
        Reminder reminder = new Reminder(owner, "분리수거", null, null, LocalDateTime.of(2026, 10, 4, 20, 0),
                Priority.NONE, RepeatRule.DAILY);

        Optional<Reminder> first = reminder.toggleComplete(LocalDateTime.of(2026, 10, 4, 21, 0));
        Optional<Reminder> undo = reminder.toggleComplete(LocalDateTime.of(2026, 10, 4, 21, 1));
        Optional<Reminder> again = reminder.toggleComplete(LocalDateTime.of(2026, 10, 4, 21, 2));

        assertThat(first).isPresent();
        assertThat(undo).isEmpty();
        assertThat(again).isEmpty();
        assertThat(reminder.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("반복하는 하위 작업을 완료하면 다음 회차가 같은 부모의 마지막 하위 작업으로 붙는다")
    void toggleComplete_whenRepeatingSubtask_attachesNextOccurrenceToSameParent() {
        Reminder parent = new Reminder(owner, "운동", null, null, null);
        Reminder stretching = new Reminder(owner, "스트레칭", null, null, LocalDateTime.of(2026, 10, 4, 7, 0),
                Priority.NONE, RepeatRule.DAILY);
        parent.addSubtask(stretching);

        Optional<Reminder> next = stretching.toggleComplete(LocalDateTime.of(2026, 10, 4, 7, 30));

        assertThat(next).hasValueSatisfying(occurrence -> {
            assertThat(occurrence.getParent()).isSameAs(parent);
            assertThat(occurrence.getSortOrder()).isEqualTo(1);
        });
        assertThat(parent.getSubtasks()).containsExactly(stretching, next.get());
    }

    @Test
    @DisplayName("부모 완료로 함께 완료된 반복 하위 작업은 다음 회차를 만들지 않는다")
    void toggleComplete_whenCompletingParent_doesNotRepeatSubtasks() {
        Reminder parent = new Reminder(owner, "운동", null, null, null);
        parent.addSubtask(new Reminder(owner, "스트레칭", null, null, LocalDateTime.of(2026, 10, 4, 7, 0),
                Priority.NONE, RepeatRule.DAILY));

        Optional<Reminder> next = parent.toggleComplete(LocalDateTime.of(2026, 10, 4, 8, 0));

        assertThat(next).isEmpty();
        assertThat(parent.getSubtasks()).hasSize(1);
    }
}

package demo.ai.reminder.service;

import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReminderServiceTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Test
    @DisplayName("전체 리마인더 목록을 조회한다")
    void getReminders_returnsAllReminders() {
        reminderRepository.save(new Reminder("우유 사기", null, null, null));

        List<Reminder> result = reminderService.getReminders(null);

        assertThat(result).extracting(Reminder::getTitle).contains("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하면 해당 리스트의 리마인더만 조회한다")
    void getReminders_returnsOnlyRemindersOfGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", "#FF9500"));
        ReminderList work = reminderListRepository.save(new ReminderList("업무", "#007AFF"));
        reminderRepository.save(new Reminder("우유 사기", null, shopping, null));
        reminderRepository.save(new Reminder("보고서 작성", null, work, null));

        List<Reminder> result = reminderService.getReminders(shopping.getId());

        assertThat(result).extracting(Reminder::getTitle).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하지 않으면 리스트와 상관없이 전체를 조회한다")
    void getReminders_returnsRemindersOfAllListsAndWithoutList_whenListIdIsNull() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));
        reminderRepository.save(new Reminder("우유 사기", null, shopping, null));
        reminderRepository.save(new Reminder("보고서 작성", null, null, null));

        List<Reminder> result = reminderService.getReminders(null);

        assertThat(result).extracting(Reminder::getTitle).contains("우유 사기", "보고서 작성");
    }

    @Test
    @DisplayName("listId를 지정해 리마인더를 생성하면 해당 리스트에 소속된다")
    void createReminder_belongsToGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));

        Reminder result = reminderService.createReminder(new ReminderRequest("우유 사기", null, shopping.getId(), null));

        assertThat(result.getList().getId()).isEqualTo(shopping.getId());
    }

    @Test
    @DisplayName("존재하지 않는 리스트로 리마인더를 생성하면 404 예외가 발생한다")
    void createReminder_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderService.createReminder(new ReminderRequest("우유 사기", null, -1L, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("요청으로 받은 제목과 메모로 리마인더를 생성한다")
    void createReminder_savesReminderWithGivenTitleAndMemo() {
        ReminderRequest request = new ReminderRequest("우유 사기", "저지방", null, null);

        Reminder result = reminderService.createReminder(request);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("우유 사기");
        assertThat(result.getMemo()).isEqualTo("저지방");
        assertThat(result.isCompleted()).isFalse();
        assertThat(reminderRepository.findById(result.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하는 리마인더의 완료 상태를 토글한다")
    void toggleComplete_flipsCompletedState_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        Reminder result = reminderService.toggleComplete(saved.getId());

        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 토글하면 404 예외가 발생한다")
    void toggleComplete_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.toggleComplete(-1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("마감일시를 지정해 리마인더를 생성하면 마감일시가 저장되고 플래그는 꺼져 있다")
    void createReminder_savesDueAt_andIsNotFlagged() {
        LocalDateTime dueAt = LocalDate.now().atTime(18, 30);

        Reminder result = reminderService.createReminder(new ReminderRequest("우유 사기", null, null, dueAt));

        assertThat(result.getDueAt()).isEqualTo(dueAt);
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    @DisplayName("존재하는 리마인더의 제목, 메모, 마감일시, 플래그를 수정한다")
    void updateReminder_changesEditableFields_whenReminderExists() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, shopping, null));
        LocalDateTime dueAt = LocalDate.now().atTime(18, 30);

        Reminder result = reminderService.updateReminder(
                saved.getId(), new ReminderUpdateRequest("계란 사기", "12구", dueAt, true));

        assertThat(result.getTitle()).isEqualTo("계란 사기");
        assertThat(result.getMemo()).isEqualTo("12구");
        assertThat(result.getDueAt()).isEqualTo(dueAt);
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getList().getId()).isEqualTo(shopping.getId());
        assertThat(reminderRepository.findById(saved.getId()).orElseThrow().getTitle()).isEqualTo("계란 사기");
    }

    @Test
    @DisplayName("수정한 마감일시와 플래그는 스마트 뷰 조회에 반영된다")
    void updateReminder_isReflectedInSmartViews() {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        reminderService.updateReminder(
                saved.getId(), new ReminderUpdateRequest("우유 사기", null, LocalDate.now().atTime(9, 0), true));

        assertThat(reminderService.getSmartReminders("today")).extracting(Reminder::getTitle).containsExactly("우유 사기");
        assertThat(reminderService.getSmartReminders("flagged")).extracting(Reminder::getTitle).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 수정하면 404 예외가 발생한다")
    void updateReminder_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.updateReminder(-1L, new ReminderUpdateRequest("우유 사기", null, null, false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("존재하는 리마인더의 플래그 상태를 토글한다")
    void toggleFlag_flipsFlaggedState_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        Reminder result = reminderService.toggleFlag(saved.getId());

        assertThat(result.isFlagged()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 리마인더의 플래그를 토글하면 404 예외가 발생한다")
    void toggleFlag_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.toggleFlag(-1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("today 뷰는 마감일이 오늘인 미완료 리마인더만 조회한다")
    void getSmartReminders_today_returnsIncompleteRemindersDueToday() {
        LocalDate today = LocalDate.now();
        reminderRepository.save(new Reminder("오늘 아침", null, null, today.atTime(0, 0)));
        reminderRepository.save(new Reminder("오늘 저녁", null, null, today.atTime(23, 59)));
        reminderRepository.save(new Reminder("어제", null, null, today.minusDays(1).atTime(23, 59)));
        reminderRepository.save(new Reminder("내일", null, null, today.plusDays(1).atStartOfDay()));
        reminderRepository.save(new Reminder("마감 없음", null, null, null));
        Reminder done = reminderRepository.save(new Reminder("오늘 완료", null, null, today.atTime(12, 0)));
        done.toggleComplete();

        List<Reminder> result = reminderService.getSmartReminders("today");

        assertThat(result).extracting(Reminder::getTitle).containsExactly("오늘 아침", "오늘 저녁");
    }

    @Test
    @DisplayName("scheduled 뷰는 마감일이 설정된 모든 미완료 리마인더를 마감일 순으로 조회한다")
    void getSmartReminders_scheduled_returnsIncompleteRemindersWithDueAt() {
        LocalDate today = LocalDate.now();
        reminderRepository.save(new Reminder("다음주", null, null, today.plusDays(7).atTime(9, 0)));
        reminderRepository.save(new Reminder("오늘", null, null, today.atTime(9, 0)));
        reminderRepository.save(new Reminder("마감 없음", null, null, null));
        Reminder done = reminderRepository.save(new Reminder("완료", null, null, today.atTime(10, 0)));
        done.toggleComplete();

        List<Reminder> result = reminderService.getSmartReminders("scheduled");

        assertThat(result).extracting(Reminder::getTitle).containsExactly("오늘", "다음주");
    }

    @Test
    @DisplayName("all 뷰는 모든 미완료 리마인더를 조회한다")
    void getSmartReminders_all_returnsAllIncompleteReminders() {
        reminderRepository.save(new Reminder("우유 사기", null, null, null));
        Reminder done = reminderRepository.save(new Reminder("완료", null, null, null));
        done.toggleComplete();

        List<Reminder> result = reminderService.getSmartReminders("all");

        assertThat(result).extracting(Reminder::getTitle).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("flagged 뷰는 플래그가 지정된 미완료 리마인더만 조회한다")
    void getSmartReminders_flagged_returnsFlaggedIncompleteReminders() {
        Reminder flagged = reminderRepository.save(new Reminder("중요", null, null, null));
        flagged.toggleFlag();
        Reminder flaggedDone = reminderRepository.save(new Reminder("중요하지만 완료", null, null, null));
        flaggedDone.toggleFlag();
        flaggedDone.toggleComplete();
        reminderRepository.save(new Reminder("일반", null, null, null));

        List<Reminder> result = reminderService.getSmartReminders("flagged");

        assertThat(result).extracting(Reminder::getTitle).containsExactly("중요");
    }

    @Test
    @DisplayName("completed 뷰는 완료된 리마인더만 조회한다")
    void getSmartReminders_completed_returnsCompletedReminders() {
        reminderRepository.save(new Reminder("미완료", null, null, null));
        Reminder done = reminderRepository.save(new Reminder("완료", null, null, null));
        done.toggleComplete();

        List<Reminder> result = reminderService.getSmartReminders("completed");

        assertThat(result).extracting(Reminder::getTitle).containsExactly("완료");
    }

    @Test
    @DisplayName("스마트 뷰 이름은 대소문자를 구분하지 않는다")
    void getSmartReminders_isCaseInsensitive() {
        reminderRepository.save(new Reminder("우유 사기", null, null, null));

        List<Reminder> result = reminderService.getSmartReminders("ALL");

        assertThat(result).extracting(Reminder::getTitle).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("알 수 없는 스마트 뷰를 요청하면 400 예외가 발생한다")
    void getSmartReminders_throwsBadRequest_whenViewIsUnknown() {
        assertThatThrownBy(() -> reminderService.getSmartReminders("unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    @DisplayName("존재하는 리마인더를 삭제한다")
    void deleteReminder_deletesReminder_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null, null));

        reminderService.deleteReminder(saved.getId());

        assertThat(reminderRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 삭제하면 404 예외가 발생한다")
    void deleteReminder_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.deleteReminder(-1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}

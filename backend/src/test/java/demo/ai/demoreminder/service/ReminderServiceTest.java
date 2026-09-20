package demo.ai.demoreminder.service;

import demo.ai.demoreminder.domain.Reminder;
import demo.ai.demoreminder.domain.ReminderList;
import demo.ai.demoreminder.dto.ReminderRequest;
import demo.ai.demoreminder.repository.ReminderListRepository;
import demo.ai.demoreminder.repository.ReminderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        reminderRepository.save(new Reminder("우유 사기", null, null));

        List<Reminder> result = reminderService.getReminders(null);

        assertThat(result).extracting(Reminder::getTitle).contains("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하면 해당 리스트의 리마인더만 조회한다")
    void getReminders_returnsOnlyRemindersOfGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", "#FF9500"));
        ReminderList work = reminderListRepository.save(new ReminderList("업무", "#007AFF"));
        reminderRepository.save(new Reminder("우유 사기", null, shopping));
        reminderRepository.save(new Reminder("보고서 작성", null, work));

        List<Reminder> result = reminderService.getReminders(shopping.getId());

        assertThat(result).extracting(Reminder::getTitle).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하지 않으면 리스트와 상관없이 전체를 조회한다")
    void getReminders_returnsRemindersOfAllListsAndWithoutList_whenListIdIsNull() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));
        reminderRepository.save(new Reminder("우유 사기", null, shopping));
        reminderRepository.save(new Reminder("보고서 작성", null, null));

        List<Reminder> result = reminderService.getReminders(null);

        assertThat(result).extracting(Reminder::getTitle).contains("우유 사기", "보고서 작성");
    }

    @Test
    @DisplayName("listId를 지정해 리마인더를 생성하면 해당 리스트에 소속된다")
    void createReminder_belongsToGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));

        Reminder result = reminderService.createReminder(new ReminderRequest("우유 사기", null, shopping.getId()));

        assertThat(result.getList().getId()).isEqualTo(shopping.getId());
    }

    @Test
    @DisplayName("존재하지 않는 리스트로 리마인더를 생성하면 404 예외가 발생한다")
    void createReminder_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderService.createReminder(new ReminderRequest("우유 사기", null, -1L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("요청으로 받은 제목과 메모로 리마인더를 생성한다")
    void createReminder_savesReminderWithGivenTitleAndMemo() {
        ReminderRequest request = new ReminderRequest("우유 사기", "저지방", null);

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
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null));

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
    @DisplayName("존재하는 리마인더를 삭제한다")
    void deleteReminder_deletesReminder_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder("우유 사기", null, null));

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

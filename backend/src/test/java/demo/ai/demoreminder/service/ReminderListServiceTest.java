package demo.ai.demoreminder.service;

import demo.ai.demoreminder.domain.Reminder;
import demo.ai.demoreminder.domain.ReminderList;
import demo.ai.demoreminder.dto.ReminderListRequest;
import demo.ai.demoreminder.repository.ReminderListRepository;
import demo.ai.demoreminder.repository.ReminderListSummary;
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
class ReminderListServiceTest {

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Test
    @DisplayName("리스트 목록은 리스트별 미완료 리마인더 개수를 포함한다")
    void getLists_includesIncompleteReminderCountPerList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));
        ReminderList work = reminderListRepository.save(new ReminderList("업무", null));
        reminderRepository.save(new Reminder("우유 사기", null, shopping, null));
        reminderRepository.save(new Reminder("계란 사기", null, shopping, null));
        Reminder done = reminderRepository.save(new Reminder("빵 사기", null, shopping, null));
        done.toggleComplete();

        List<ReminderListSummary> result = reminderListService.getLists();

        assertThat(result)
                .filteredOn(summary -> summary.list().getId().equals(shopping.getId()))
                .extracting(ReminderListSummary::reminderCount)
                .containsExactly(2L);
        assertThat(result)
                .filteredOn(summary -> summary.list().getId().equals(work.getId()))
                .extracting(ReminderListSummary::reminderCount)
                .containsExactly(0L);
    }

    @Test
    @DisplayName("요청으로 받은 이름과 색상으로 리스트를 생성한다")
    void createList_savesListWithGivenNameAndColor() {
        ReminderListSummary result = reminderListService.createList(new ReminderListRequest("장보기", "#FF9500"));

        assertThat(result.list().getId()).isNotNull();
        assertThat(result.list().getName()).isEqualTo("장보기");
        assertThat(result.list().getColor()).isEqualTo("#FF9500");
        assertThat(result.reminderCount()).isZero();
        assertThat(reminderListRepository.findById(result.list().getId())).isPresent();
    }

    @Test
    @DisplayName("리스트의 이름과 색상을 수정한다")
    void updateList_changesNameAndColor() {
        ReminderList saved = reminderListRepository.save(new ReminderList("장보기", "#FF9500"));
        reminderRepository.save(new Reminder("우유 사기", null, saved, null));

        ReminderListSummary result = reminderListService.updateList(saved.getId(), new ReminderListRequest("업무", "#007AFF"));

        assertThat(result.list().getName()).isEqualTo("업무");
        assertThat(result.list().getColor()).isEqualTo("#007AFF");
        assertThat(result.reminderCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 리스트를 수정하면 404 예외가 발생한다")
    void updateList_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderListService.updateList(-1L, new ReminderListRequest("업무", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("리스트를 삭제하면 소속 리마인더도 함께 삭제되고 다른 리스트의 리마인더는 유지된다")
    void deleteList_deletesRemindersOfListOnly() {
        ReminderList shopping = reminderListRepository.save(new ReminderList("장보기", null));
        ReminderList work = reminderListRepository.save(new ReminderList("업무", null));
        Reminder milk = reminderRepository.save(new Reminder("우유 사기", null, shopping, null));
        Reminder report = reminderRepository.save(new Reminder("보고서 작성", null, work, null));

        reminderListService.deleteList(shopping.getId());

        assertThat(reminderListRepository.findById(shopping.getId())).isEmpty();
        assertThat(reminderRepository.findById(milk.getId())).isEmpty();
        assertThat(reminderRepository.findById(report.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 리스트를 삭제하면 404 예외가 발생한다")
    void deleteList_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderListService.deleteList(-1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}

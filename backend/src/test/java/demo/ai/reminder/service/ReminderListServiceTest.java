package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.Tag;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderListSummary;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.TagRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void signIn() {
        owner = TestAuth.signIn(userRepository, "owner@example.com");
    }

    @Test
    @DisplayName("리스트 목록은 리스트별 미완료 리마인더 개수를 포함한다")
    void getLists_includesIncompleteReminderCountPerList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null));
        reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null));
        reminderRepository.save(new Reminder(owner, "계란 사기", null, shopping, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "빵 사기", null, shopping, null));
        done.toggleComplete(LocalDateTime.now());

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
    @DisplayName("리스트를 생성하면 JPA Auditing이 생성일과 수정일을 채운다")
    void createList_fillsCreatedAtAndUpdatedAt() {
        LocalDateTime before = LocalDateTime.now();

        ReminderListSummary result = reminderListService.createList(new ReminderListRequest("장보기", null));

        assertThat(result.list().getCreatedAt()).isNotNull().isAfterOrEqualTo(before);
        assertThat(result.list().getUpdatedAt()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("리스트를 수정하면 수정일은 갱신되고 생성일은 유지된다")
    void updateList_refreshesUpdatedAt_andKeepsCreatedAt() {
        ReminderList saved = reminderListRepository.saveAndFlush(new ReminderList(owner, "장보기", null));
        LocalDateTime createdAt = saved.getCreatedAt();
        LocalDateTime updatedAt = saved.getUpdatedAt();

        reminderListService.updateList(saved.getId(), new ReminderListRequest("업무", null));
        reminderListRepository.flush();

        ReminderList result = reminderListRepository.findById(saved.getId()).orElseThrow();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    @DisplayName("리스트의 이름과 색상을 수정한다")
    void updateList_changesNameAndColor() {
        ReminderList saved = reminderListRepository.save(new ReminderList(owner, "장보기", "#FF9500"));
        reminderRepository.save(new Reminder(owner, "우유 사기", null, saved, null));

        ReminderListSummary result = reminderListService.updateList(saved.getId(), new ReminderListRequest("업무", "#007AFF"));

        assertThat(result.list().getName()).isEqualTo("업무");
        assertThat(result.list().getColor()).isEqualTo("#007AFF");
        assertThat(result.reminderCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("존재하지 않는 리스트를 수정하면 404 예외가 발생한다")
    void updateList_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderListService.updateList(-1L, new ReminderListRequest("업무", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("리스트를 삭제하면 소속 리마인더도 함께 삭제되고 다른 리스트의 리마인더는 유지된다")
    void deleteList_deletesRemindersOfListOnly() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null));
        Reminder milk = reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null));
        Reminder report = reminderRepository.save(new Reminder(owner, "보고서 작성", null, work, null));

        reminderListService.deleteList(shopping.getId());

        assertThat(reminderListRepository.findById(shopping.getId())).isEmpty();
        assertThat(reminderRepository.findById(milk.getId())).isEmpty();
        assertThat(reminderRepository.findById(report.getId())).isPresent();
    }

    @Test
    @DisplayName("태그가 붙은 리마인더가 있는 리스트도 삭제할 수 있다")
    void deleteList_deletesTaggedReminders() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        Tag home = tagRepository.save(new Tag(owner, "집"));
        Reminder milk = new Reminder(owner, "우유 사기", null, shopping, null);
        milk.replaceTags(List.of(home));
        reminderRepository.save(milk);
        reminderRepository.flush();

        reminderListService.deleteList(shopping.getId());
        reminderRepository.flush();

        assertThat(reminderRepository.findById(milk.getId())).isEmpty();
        assertThat(tagRepository.findById(home.getId())).isPresent();
    }

    @Test
    @DisplayName("리스트의 리마인더 개수는 최상위 미완료 리마인더만 세고 하위 작업은 제외한다")
    void getLists_countsTopLevelRemindersOnly() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        Reminder parent = new Reminder(owner, "이사 준비", null, home, null);
        parent.addSubtask(new Reminder(owner, "박스 구하기", null, null, null));
        parent.addSubtask(new Reminder(owner, "이삿짐센터 예약", null, null, null));
        reminderRepository.save(parent);
        reminderRepository.save(new Reminder(owner, "빨래", null, home, null));

        List<ReminderListSummary> result = reminderListService.getLists();

        assertThat(result)
                .filteredOn(summary -> summary.list().getId().equals(home.getId()))
                .extracting(ReminderListSummary::reminderCount)
                .containsExactly(2L);
        assertThat(reminderListService.updateList(home.getId(), new ReminderListRequest("우리 집", null)).reminderCount())
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("하위 작업이 있는 리스트를 삭제하면 부모와 하위 작업이 모두 삭제된다")
    void deleteList_deletesParentsAndSubtasks() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        Reminder parent = new Reminder(owner, "이사 준비", null, home, null);
        Reminder boxes = new Reminder(owner, "박스 구하기", null, null, null);
        parent.addSubtask(boxes);
        reminderRepository.save(parent);
        reminderRepository.flush();

        reminderListService.deleteList(home.getId());
        reminderRepository.flush();

        assertThat(reminderRepository.findById(parent.getId())).isEmpty();
        assertThat(reminderRepository.findById(boxes.getId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 리스트를 삭제하면 404 예외가 발생한다")
    void deleteList_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderListService.deleteList(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("리스트를 생성하면 마지막 순서가 부여된다")
    void createList_assignsLastSortOrder() {
        ReminderListSummary first = reminderListService.createList(new ReminderListRequest("장보기", null));
        ReminderListSummary second = reminderListService.createList(new ReminderListRequest("업무", null));

        assertThat(second.list().getSortOrder()).isEqualTo(first.list().getSortOrder() + 1);
    }

    @Test
    @DisplayName("리스트 순서를 변경하면 리스트 목록이 바뀐 순서대로 반환된다")
    void reorderLists_changesListOrder() {
        Long shopping = reminderListService.createList(new ReminderListRequest("장보기", null)).list().getId();
        Long work = reminderListService.createList(new ReminderListRequest("업무", null)).list().getId();
        Long hobby = reminderListService.createList(new ReminderListRequest("취미", null)).list().getId();
        List<Long> ids = new ArrayList<>(List.of(hobby, shopping, work));
        reminderListRepository.findAll().stream()
                .map(ReminderList::getId)
                .filter(id -> !ids.contains(id))
                .forEach(ids::add);

        reminderListService.reorderLists(ids);

        assertThat(reminderListService.getLists())
                .extracting(summary -> summary.list().getId())
                .containsExactlyElementsOf(ids);
    }

    @Test
    @DisplayName("리스트 순서 변경 ids가 전체 리스트와 정확히 일치하지 않으면 400 예외가 발생한다")
    void reorderLists_throwsBadRequest_whenIdsDoNotMatch() {
        Long shopping = reminderListService.createList(new ReminderListRequest("장보기", null)).list().getId();
        List<Long> allIds = reminderListRepository.findAll().stream().map(ReminderList::getId).toList();
        List<Long> missing = allIds.stream().filter(id -> !id.equals(shopping)).toList();
        List<Long> duplicated = new ArrayList<>(missing);
        duplicated.add(missing.isEmpty() ? -1L : missing.getFirst());
        List<Long> unknown = new ArrayList<>(missing);
        unknown.add(-1L);

        for (List<Long> ids : List.of(missing, duplicated, unknown)) {
            assertThatThrownBy(() -> reminderListService.reorderLists(ids))
                    .isInstanceOf(BusinessException.class)
                    .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
        }
    }
}

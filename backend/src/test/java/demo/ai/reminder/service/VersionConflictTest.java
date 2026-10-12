package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.ListGroupRequest;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.dto.ReminderListResponse;
import demo.ai.reminder.dto.ReminderListUpdateRequest;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderResponse;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.support.TestAuth;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class VersionConflictTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private ListGroupService listGroupService;

    @Autowired
    private ListMemberService listMemberService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;

    @BeforeEach
    void signIn() {
        owner = TestAuth.signIn(userRepository, "owner@example.com");
    }

    @Test
    @DisplayName("새로 만든 리마인더와 리스트의 버전은 0이다")
    void create_startsAtVersionZero() {
        ReminderListResponse list = reminderListService.createList(new ReminderListRequest("장보기", null));
        ReminderResponse reminder = reminderService.createReminder(
                new ReminderRequest("우유 사기", null, list.id(), null, null, null, null, null));

        assertThat(list.version()).isZero();
        assertThat(reminder.version()).isZero();
    }

    @Test
    @DisplayName("요청 버전이 현재 버전과 같으면 리마인더를 수정하고 버전을 1 올린다")
    void updateReminder_updatesAndIncrementsVersion_whenVersionMatches() {
        ReminderResponse created = createReminder("우유 사기");

        ReminderResponse updated = reminderService.updateReminder(created.id(), updateRequest("저지방 우유", 0L));

        assertThat(updated.title()).isEqualTo("저지방 우유");
        assertThat(updated.version()).isEqualTo(1L);
        assertThat(reminderService.updateReminder(created.id(), updateRequest("두유", 1L)).version()).isEqualTo(2L);
    }

    @Test
    @DisplayName("요청 버전이 현재 버전과 다르면(다른 사용자가 먼저 저장) 리마인더 수정은 VERSION_CONFLICT이고 앞의 변경이 남는다")
    void updateReminder_throwsVersionConflict_whenVersionIsStale() {
        ReminderResponse created = createReminder("우유 사기");
        reminderService.updateReminder(created.id(), updateRequest("먼저 저장", 0L));

        assertThatThrownBy(() -> reminderService.updateReminder(created.id(), updateRequest("나중 저장", 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.VERSION_CONFLICT);
        entityManager.flush();
        entityManager.clear();
        assertThat(reminderRepository.findById(created.id()).orElseThrow())
                .satisfies(reminder -> {
                    assertThat(reminder.getTitle()).isEqualTo("먼저 저장");
                    assertThat(reminder.getVersion()).isEqualTo(1L);
                });
    }

    @Test
    @DisplayName("완료/플래그 토글과 리스트 이동은 버전을 받지 않고 버전을 올리므로, 그 전에 연 편집은 VERSION_CONFLICT다")
    void toggleAndMove_incrementVersion() {
        Long target = reminderListService.createList(new ReminderListRequest("업무", null)).id();
        ReminderResponse created = createReminder("우유 사기");

        assertThat(reminderService.toggleFlag(created.id()).version()).isEqualTo(1L);
        assertThat(reminderService.toggleComplete(created.id()).version()).isEqualTo(2L);
        assertThat(reminderService.moveReminder(created.id(), target).version()).isEqualTo(3L);
        assertThatThrownBy(() -> reminderService.updateReminder(created.id(), updateRequest("나중 저장", 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.VERSION_CONFLICT);
    }

    @Test
    @DisplayName("리마인더 순서를 바꾸면 순서가 바뀐 리마인더의 버전이 올라간다")
    void reorderReminders_incrementsVersion() {
        Long listId = reminderListService.createList(new ReminderListRequest("장보기", null)).id();
        ReminderResponse first = reminderService.createReminder(
                new ReminderRequest("우유", null, listId, null, null, null, null, null));
        ReminderResponse second = reminderService.createReminder(
                new ReminderRequest("빵", null, listId, null, null, null, null, null));

        reminderService.reorderReminders(listId, List.of(second.id(), first.id()));
        entityManager.flush();

        assertThat(reminderRepository.findById(first.id()).orElseThrow().getVersion()).isEqualTo(1L);
        assertThat(reminderRepository.findById(second.id()).orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("요청 버전이 현재 버전과 같으면 리스트를 수정하고 버전을 올리며, 다르면 VERSION_CONFLICT다")
    void updateList_checksVersion() {
        ReminderListResponse created = reminderListService.createList(new ReminderListRequest("장보기", null));

        ReminderListResponse updated = reminderListService.updateList(created.id(),
                new ReminderListUpdateRequest("마트", "#FF9500", 0L));

        assertThat(updated.name()).isEqualTo("마트");
        assertThat(updated.version()).isEqualTo(1L);
        assertThatThrownBy(() -> reminderListService.updateList(created.id(),
                new ReminderListUpdateRequest("시장", null, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.VERSION_CONFLICT);
    }

    @Test
    @DisplayName("리스트 그룹 지정과 순서 변경은 버전을 받지 않고 리스트 버전을 올린다")
    void changeGroupAndReorderLists_incrementVersion() {
        Long shopping = reminderListService.createList(new ReminderListRequest("장보기", null)).id();
        Long work = reminderListService.createList(new ReminderListRequest("업무", null)).id();
        Long groupId = listGroupService.createGroup(new ListGroupRequest("개인")).id();

        assertThat(reminderListService.changeGroup(shopping, groupId).version()).isEqualTo(1L);
        reminderListService.reorderLists(List.of(work, shopping));
        entityManager.flush();

        assertThat(reminderListRepository.findById(shopping).orElseThrow().getVersion()).isEqualTo(2L);
        assertThat(reminderListRepository.findById(work).orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("하위 작업을 추가/삭제해도 부모의 버전은 그대로라, 편집 창에서 하위 작업을 추가한 뒤 부모를 저장할 수 있다")
    void subtaskChanges_doNotIncrementParentVersion() {
        ReminderResponse parent = createReminder("장보기");
        ReminderResponse subtask = reminderService.createReminder(
                new ReminderRequest("우유", null, null, null, null, null, parent.id(), null));
        reminderService.createReminder(new ReminderRequest("빵", null, null, null, null, null, parent.id(), null));
        reminderService.deleteReminder(subtask.id());
        entityManager.flush();
        entityManager.clear();

        assertThat(reminderRepository.findById(parent.id()).orElseThrow().getVersion()).isZero();
        assertThat(reminderService.updateReminder(parent.id(), updateRequest("마트 장보기", 0L)).version()).isEqualTo(1L);
    }

    @Test
    @DisplayName("멤버를 초대/제거해도 리스트 버전은 그대로다")
    void memberChanges_doNotIncrementListVersion() {
        User bob = userRepository.save(new User("bob@example.com", "password", "밥"));
        Long listId = reminderListService.createList(new ReminderListRequest("가족", null)).id();

        listMemberService.invite(listId, "bob@example.com");
        entityManager.flush();
        listMemberService.removeMember(listId, bob.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(reminderListRepository.findById(listId).orElseThrow().getVersion()).isZero();
    }

    @Test
    @DisplayName("version 컬럼 추가 전에 저장된 행(버전 값 없이 저장)은 버전 0으로 채워져 버전 0으로 수정할 수 있다")
    void legacyRows_defaultToVersionZero() {
        entityManager.createNativeQuery("""
                        insert into reminder (user_id, title, completed, flagged, priority, repeat_rule,
                                              next_occurrence_created, sort_order)
                        values (:userId, '예전 리마인더', false, false, 'NONE', 'NONE', false, 0)
                        """)
                .setParameter("userId", owner.getId())
                .executeUpdate();
        entityManager.createNativeQuery("insert into reminder_list (user_id, name, sort_order) values (:userId, '예전 리스트', 0)")
                .setParameter("userId", owner.getId())
                .executeUpdate();
        entityManager.clear();
        Long reminderId = reminderRepository.findAll().getFirst().getId();

        assertThat(reminderRepository.findById(reminderId).orElseThrow().getVersion()).isZero();
        assertThat(reminderListRepository.findAll()).singleElement()
                .satisfies(list -> assertThat(list.getVersion()).isZero());
        assertThat(reminderService.updateReminder(reminderId, updateRequest("수정", 0L)).version()).isEqualTo(1L);
    }

    private ReminderResponse createReminder(String title) {
        return reminderService.createReminder(new ReminderRequest(title, null, null, null, null, null, null, null));
    }

    private static ReminderUpdateRequest updateRequest(String title, Long version) {
        return new ReminderUpdateRequest(title, null, null, false, null, null, null, version);
    }
}

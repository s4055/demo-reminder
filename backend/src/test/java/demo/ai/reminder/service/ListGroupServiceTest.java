package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.ListGroupRequest;
import demo.ai.reminder.dto.ListGroupResponse;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.dto.ReminderListResponse;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderResponse;
import demo.ai.reminder.repository.ListGroupRepository;
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
class ListGroupServiceTest {

    @Autowired
    private ListGroupService listGroupService;

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ListGroupRepository listGroupRepository;

    @Autowired
    private ReminderRepository reminderRepository;

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
    @DisplayName("그룹을 만들면 이름 앞뒤 공백을 빼고 마지막 순서로 추가되며, 목록은 순서대로 조회된다")
    void createGroup_appendsLast_andGetGroupsReturnsInOrder() {
        ListGroupResponse life = listGroupService.createGroup(new ListGroupRequest("  생활  "));
        ListGroupResponse work = listGroupService.createGroup(new ListGroupRequest("업무"));

        assertThat(life.name()).isEqualTo("생활");
        assertThat(life.sortOrder()).isZero();
        assertThat(work.sortOrder()).isEqualTo(1);
        assertThat(listGroupService.getGroups()).extracting(ListGroupResponse::id)
                .containsExactly(life.id(), work.id());
    }

    @Test
    @DisplayName("그룹 이름을 바꿀 수 있다")
    void renameGroup_changesName() {
        ListGroupResponse group = listGroupService.createGroup(new ListGroupRequest("생활"));

        ListGroupResponse renamed = listGroupService.renameGroup(group.id(), new ListGroupRequest("집안일"));

        assertThat(renamed.name()).isEqualTo("집안일");
        assertThat(listGroupService.getGroups()).extracting(ListGroupResponse::name).containsExactly("집안일");
    }

    @Test
    @DisplayName("그룹 순서를 바꾸면 목록이 바뀐 순서대로 조회된다")
    void reorderGroups_changesOrder() {
        Long life = listGroupService.createGroup(new ListGroupRequest("생활")).id();
        Long work = listGroupService.createGroup(new ListGroupRequest("업무")).id();
        Long hobby = listGroupService.createGroup(new ListGroupRequest("취미")).id();

        listGroupService.reorderGroups(List.of(hobby, life, work));

        assertThat(listGroupService.getGroups()).extracting(ListGroupResponse::id).containsExactly(hobby, life, work);
    }

    @Test
    @DisplayName("그룹 순서 변경 ids가 전체 그룹과 정확히 일치하지 않으면 400 예외가 발생한다")
    void reorderGroups_throwsBadRequest_whenIdsDoNotMatch() {
        Long life = listGroupService.createGroup(new ListGroupRequest("생활")).id();
        Long work = listGroupService.createGroup(new ListGroupRequest("업무")).id();

        for (List<Long> ids : List.of(List.of(life), List.of(life, life), List.of(life, work, -1L))) {
            assertThatThrownBy(() -> listGroupService.reorderGroups(ids))
                    .isInstanceOf(BusinessException.class)
                    .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("리스트를 그룹에 넣으면 응답과 리스트 목록에 groupId가 담기고, null로 그룹에서 뺄 수 있다")
    void changeGroup_assignsAndClearsGroup() {
        Long group = listGroupService.createGroup(new ListGroupRequest("생활")).id();
        Long list = reminderListService.createList(new ReminderListRequest("장보기", null)).id();

        assertThat(reminderListService.changeGroup(list, group).groupId()).isEqualTo(group);
        assertThat(groupIdOf(list)).isEqualTo(group);

        assertThat(reminderListService.changeGroup(list, null).groupId()).isNull();
        assertThat(groupIdOf(list)).isNull();
    }

    @Test
    @DisplayName("그룹을 삭제하면 소속 리스트는 그룹에서 빠질 뿐 리스트와 리마인더는 남는다")
    void deleteGroup_keepsListsAndReminders() {
        Long group = listGroupService.createGroup(new ListGroupRequest("생활")).id();
        Long list = reminderListService.createList(new ReminderListRequest("장보기", null)).id();
        reminderListService.changeGroup(list, group);
        ReminderResponse milk = reminderService.createReminder(
                new ReminderRequest("우유 사기", null, list, null, null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        listGroupService.deleteGroup(group);
        entityManager.flush();
        entityManager.clear();

        assertThat(listGroupRepository.findById(group)).isEmpty();
        assertThat(groupIdOf(list)).isNull();
        assertThat(reminderRepository.findById(milk.id())).isPresent();
        assertThat(reminderListService.getLists()).filteredOn(l -> l.id().equals(list))
                .singleElement().extracting(ReminderListResponse::reminderCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("다른 사용자의 그룹은 조회되지 않고, 이름 변경/삭제/리스트 지정 모두 404다")
    void otherUsersGroup_isIsolated() {
        Long othersGroup = listGroupService.createGroup(new ListGroupRequest("남의 그룹")).id();
        TestAuth.signIn(userRepository, "other@example.com");
        Long myList = reminderListService.createList(new ReminderListRequest("내 리스트", null)).id();

        assertThat(listGroupService.getGroups()).isEmpty();
        assertThatThrownBy(() -> listGroupService.renameGroup(othersGroup, new ListGroupRequest("가로채기")))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
        assertThatThrownBy(() -> listGroupService.deleteGroup(othersGroup))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
        assertThatThrownBy(() -> reminderListService.changeGroup(myList, othersGroup))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
        assertThatThrownBy(() -> listGroupService.reorderGroups(List.of(othersGroup)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("다른 사용자의 리스트를 내 그룹에 넣으려 하면 404다")
    void changeGroup_throwsNotFound_forOtherUsersList() {
        Long othersList = reminderListService.createList(new ReminderListRequest("남의 리스트", null)).id();
        TestAuth.signIn(userRepository, "other@example.com");
        Long myGroup = listGroupService.createGroup(new ListGroupRequest("내 그룹")).id();

        assertThatThrownBy(() -> reminderListService.changeGroup(othersList, myGroup))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("없는 그룹을 지정하거나 이름 변경/삭제하면 404다")
    void missingGroup_throwsNotFound() {
        Long list = reminderListService.createList(new ReminderListRequest("장보기", null)).id();

        assertThatThrownBy(() -> reminderListService.changeGroup(list, -1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
        assertThatThrownBy(() -> listGroupService.deleteGroup(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    private Long groupIdOf(Long listId) {
        return reminderListService.getLists().stream()
                .filter(list -> list.id().equals(listId))
                .findFirst().orElseThrow()
                .groupId();
    }
}

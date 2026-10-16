package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.response.ReminderListResponse;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.dto.response.TagResponse;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.repository.ListMemberRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.service.command.CreateListCommand;
import demo.ai.reminder.service.command.CreateReminderCommand;
import demo.ai.reminder.service.command.UpdateListCommand;
import demo.ai.reminder.service.command.UpdateReminderCommand;
import demo.ai.reminder.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 공유 리스트의 접근 제어를 검증한다.
 * 앨리스(소유자)가 "가족" 리스트를 만들어 밥(편집자)을 초대했고, 캐럴은 멤버가 아니다.
 */
@SpringBootTest
@Transactional
class SharedListAccessTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderQueryService reminderQueryService;

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private ListMemberService listMemberService;

    @Autowired
    private ListGroupService listGroupService;

    @Autowired
    private TagService tagService;

    @Autowired
    private ListMemberRepository listMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private User carol;
    private Long sharedListId;
    private ReminderResponse aliceReminder;
    private LocalDateTime today;

    @BeforeEach
    void setUp() {
        bob = userRepository.save(new User("bob@example.com", "{noop}password", "밥"));
        carol = userRepository.save(new User("carol@example.com", "{noop}password", "캐럴"));
        alice = TestAuth.signIn(userRepository, "alice@example.com");
        today = LocalDate.now().atTime(23, 59);
        sharedListId = reminderListService.createList(new CreateListCommand("가족", "#34C759", null)).id();
        aliceReminder = reminderService.createReminder(
                new CreateReminderCommand("우유", null, sharedListId, today, null, List.of("장보기"), null, null));
        listMemberService.invite(sharedListId, "bob@example.com");
    }

    @Test
    @DisplayName("초대받은 사용자의 리스트 목록에 공유 리스트가 EDITOR 역할로, 자기 리스트 뒤에 나온다")
    void getLists_includesSharedListAfterOwnLists() {
        TestAuth.signIn(bob);
        Long bobListId = reminderListService.createList(new CreateListCommand("밥의 할 일", null, null)).id();

        List<ReminderListResponse> lists = reminderListService.getLists();

        assertThat(lists).extracting(ReminderListResponse::id).containsExactly(bobListId, sharedListId);
        ReminderListResponse shared = lists.get(1);
        assertThat(shared.role()).isEqualTo(ListRole.EDITOR);
        assertThat(shared.memberCount()).isEqualTo(2);
        assertThat(shared.reminderCount()).isEqualTo(1);
        assertThat(lists.get(0).role()).isEqualTo(ListRole.OWNER);
        assertThat(lists.get(0).memberCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("소유자의 리스트 목록에서도 공유한 리스트의 멤버 수가 2로 나온다")
    void getLists_forOwner_showsMemberCount() {
        assertThat(reminderListService.getLists()).singleElement().satisfies(summary -> {
            assertThat(summary.role()).isEqualTo(ListRole.OWNER);
            assertThat(summary.memberCount()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("편집자는 공유 리스트의 리마인더를 조회/추가/수정/완료/플래그/순서 변경/삭제할 수 있다")
    void editor_canManageRemindersInSharedList() {
        TestAuth.signIn(bob);

        assertThat(reminderQueryService.getReminders(sharedListId, null)).extracting(ReminderResponse::id)
                .containsExactly(aliceReminder.id());
        ReminderResponse bobReminder = reminderService.createReminder(
                new CreateReminderCommand("빵", null, sharedListId, null, null, null, null, null));
        ReminderResponse subtask = reminderService.createReminder(
                new CreateReminderCommand("통밀", null, null, null, null, null, aliceReminder.id(), null));
        reminderService.updateReminder(aliceReminder.id(),
                new UpdateReminderCommand("저지방 우유", null, today, false, null, null, null, 0L));
        reminderService.toggleFlag(aliceReminder.id());
        reminderService.reorderReminders(sharedListId, List.of(bobReminder.id(), aliceReminder.id()));
        reminderService.toggleComplete(subtask.id());
        reminderService.deleteReminder(subtask.id());

        TestAuth.signIn(alice);
        List<ReminderResponse> reminders = reminderQueryService.getReminders(sharedListId, null);
        assertThat(reminders).extracting(ReminderResponse::title).containsExactly("빵", "저지방 우유");
        assertThat(reminders.get(1).flagged()).isTrue();
        assertThat(reminders.get(1).subtasks()).isEmpty();
    }

    @Test
    @DisplayName("편집자는 공유 리스트의 완료 항목을 다른 멤버가 만든 것까지 일괄 삭제할 수 있다")
    void editor_canDeleteCompletedRemindersInSharedList() {
        reminderService.toggleComplete(aliceReminder.id());
        TestAuth.signIn(bob);
        ReminderResponse bobReminder = reminderService.createReminder(
                new CreateReminderCommand("빵", null, sharedListId, null, null, null, null, null));

        assertThat(reminderService.deleteCompletedReminders(sharedListId).deletedCount()).isEqualTo(1);

        TestAuth.signIn(alice);
        assertThat(reminderQueryService.getReminders(sharedListId, null)).extracting(ReminderResponse::id)
                .containsExactly(bobReminder.id());
    }

    @Test
    @DisplayName("검색 결과에 공유받은 리스트의 리마인더가 포함되고, 다른 사용자의 리스트 없는 리마인더는 제외된다")
    void searchReminders_includesSharedListReminders() {
        reminderService.createReminder(new CreateReminderCommand("우유 영수증", null, null, null, null, null, null, null));
        TestAuth.signIn(bob);
        ReminderResponse bobReminder = reminderService.createReminder(
                new CreateReminderCommand("우유 쿠폰", null, null, null, null, null, null, null));

        assertThat(reminderQueryService.searchReminders("우유")).extracting(ReminderResponse::id)
                .containsExactlyInAnyOrder(aliceReminder.id(), bobReminder.id());
    }

    @Test
    @DisplayName("편집자는 자기 리스트의 리마인더를 공유 리스트로, 공유 리스트의 리마인더를 자기 리스트로 옮길 수 있다")
    void editor_canMoveRemindersBetweenOwnAndSharedList() {
        TestAuth.signIn(bob);
        Long bobListId = reminderListService.createList(new CreateListCommand("밥의 리스트", null, null)).id();
        ReminderResponse bobReminder = reminderService.createReminder(
                new CreateReminderCommand("빵", null, bobListId, null, null, null, null, null));

        assertThat(reminderService.moveReminder(bobReminder.id(), sharedListId).listId()).isEqualTo(sharedListId);
        assertThat(reminderService.moveReminder(aliceReminder.id(), bobListId).listId()).isEqualTo(bobListId);
    }

    @Test
    @DisplayName("멤버가 아닌 사용자는 자기 리마인더를 공유 리스트로 옮길 수 없다 (404)")
    void nonMember_cannotMoveReminderIntoSharedList() {
        TestAuth.signIn(carol);
        Long carolListId = reminderListService.createList(new CreateListCommand("캐럴의 리스트", null, null)).id();
        ReminderResponse carolReminder = reminderService.createReminder(
                new CreateReminderCommand("커피", null, carolListId, null, null, null, null, null));

        assertThatThrownBy(() -> reminderService.moveReminder(carolReminder.id(), sharedListId))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("소유자는 편집자가 만든 리마인더도 수정/삭제할 수 있다")
    void owner_canManageEditorsReminders() {
        TestAuth.signIn(bob);
        ReminderResponse bobReminder = reminderService.createReminder(
                new CreateReminderCommand("빵", null, sharedListId, null, null, null, null, null));

        TestAuth.signIn(alice);
        reminderService.toggleComplete(bobReminder.id());
        reminderService.deleteReminder(bobReminder.id());

        assertThat(reminderQueryService.getReminders(sharedListId, null)).extracting(ReminderResponse::id)
                .containsExactly(aliceReminder.id());
    }

    @Test
    @DisplayName("편집자는 리스트 이름 변경/삭제를 할 수 없고 (403), 공유 리스트는 사이드바 순서 변경 대상이 아니다 (400)")
    void editor_cannotChangeTheListItself() {
        TestAuth.signIn(bob);

        assertResultCode(() -> reminderListService.updateList(sharedListId, new UpdateListCommand("바꿈", null, null, 0L)),
                ResultCode.FORBIDDEN);
        assertResultCode(() -> reminderListService.deleteList(sharedListId), ResultCode.FORBIDDEN);
        assertResultCode(() -> reminderListService.reorderLists(List.of(sharedListId)), ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("공유 리스트의 그룹은 소유자만 지정하고(편집자 403), 편집자의 리스트 목록에서는 groupId가 null이다")
    void sharedList_groupIsOwnersOnly() {
        Long aliceGroup = listGroupService.createGroup("가족 일").id();
        reminderListService.changeGroup(sharedListId, aliceGroup);

        TestAuth.signIn(bob);
        Long bobGroup = listGroupService.createGroup("밥의 그룹").id();

        assertResultCode(() -> reminderListService.changeGroup(sharedListId, bobGroup), ResultCode.FORBIDDEN);
        assertThat(reminderListService.getLists()).filteredOn(list -> list.id().equals(sharedListId))
                .singleElement().satisfies(list -> assertThat(list.groupId()).isNull());
    }

    @Test
    @DisplayName("스마트 뷰, 태그별/전체 조회, 다가오는 리마인더에 공유 리스트의 리마인더가 포함된다")
    void smartViews_includeSharedListReminders() {
        reminderService.toggleFlag(aliceReminder.id());
        ReminderResponse done = reminderService.createReminder(
                new CreateReminderCommand("세제", null, sharedListId, null, null, null, null, null));
        reminderService.toggleComplete(done.id());
        TestAuth.signIn(bob);

        for (SmartView view : List.of(SmartView.TODAY, SmartView.SCHEDULED, SmartView.ALL, SmartView.FLAGGED)) {
            assertThat(reminderQueryService.getSmartReminders(view)).as(view.name()).extracting(ReminderResponse::id)
                    .containsExactly(aliceReminder.id());
        }
        assertThat(reminderQueryService.getSmartReminders(SmartView.COMPLETED)).extracting(ReminderResponse::id)
                .containsExactly(done.id());
        assertThat(reminderQueryService.getReminders(null, null)).extracting(ReminderResponse::id)
                .containsExactlyInAnyOrder(aliceReminder.id(), done.id());
        assertThat(reminderQueryService.getReminders(null, "장보기")).extracting(ReminderResponse::id)
                .containsExactly(aliceReminder.id());
        LocalDateTime now = LocalDateTime.now();
        assertThat(reminderQueryService.getUpcomingReminders(now.minusDays(1), now.plusDays(2)))
                .extracting(ReminderResponse::id).containsExactly(aliceReminder.id());
    }

    @Test
    @DisplayName("공유 리스트와 관계없는 리마인더(리스트 없음)는 다른 멤버에게 보이지 않는다")
    void remindersWithoutList_stayPrivate() {
        ReminderResponse privateReminder = reminderService.createReminder(
                new CreateReminderCommand("개인 메모", null, null, today, null, null, null, null));
        TestAuth.signIn(bob);

        assertThat(reminderQueryService.getSmartReminders(SmartView.ALL)).extracting(ReminderResponse::id)
                .doesNotContain(privateReminder.id());
        assertResultCode(() -> reminderService.toggleFlag(privateReminder.id()), ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("멤버가 아닌 사용자는 공유 리스트와 그 리마인더에 접근할 수 없다 (404)")
    void nonMember_cannotAccessSharedList() {
        TestAuth.signIn(carol);

        assertThat(reminderListService.getLists()).isEmpty();
        assertThat(reminderQueryService.getSmartReminders(SmartView.ALL)).isEmpty();
        assertThat(reminderQueryService.getReminders(null, "장보기")).isEmpty();
        assertThat(reminderQueryService.searchReminders("우유")).isEmpty();
        List<Executable> attempts = List.of(
                () -> reminderQueryService.getReminders(sharedListId, null),
                () -> reminderService.createReminder(
                        new CreateReminderCommand("몰래", null, sharedListId, null, null, null, null, null)),
                () -> reminderService.createReminder(
                        new CreateReminderCommand("몰래", null, null, null, null, null, aliceReminder.id(), null)),
                () -> reminderService.updateReminder(aliceReminder.id(),
                        new UpdateReminderCommand("몰래", null, null, false, null, null, null, 0L)),
                () -> reminderService.toggleComplete(aliceReminder.id()),
                () -> reminderService.deleteReminder(aliceReminder.id()),
                () -> reminderService.reorderReminders(sharedListId, List.of(aliceReminder.id())),
                () -> reminderService.deleteCompletedReminders(sharedListId),
                () -> reminderListService.updateList(sharedListId, new UpdateListCommand("몰래", null, null, 0L)),
                () -> reminderListService.deleteList(sharedListId)
        );
        for (Executable attempt : attempts) {
            assertResultCode(attempt, ResultCode.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("리스트에서 제거되면 본인이 그 리스트에 만든 리마인더에도 더 이상 접근할 수 없고, 리마인더는 리스트에 남는다")
    void removedMember_losesAccessToOwnRemindersInList() {
        TestAuth.signIn(bob);
        ReminderResponse bobReminder = reminderService.createReminder(
                new CreateReminderCommand("빵", null, sharedListId, today, null, null, null, null));
        TestAuth.signIn(alice);
        listMemberService.removeMember(sharedListId, bob.getId());

        TestAuth.signIn(bob);
        assertThat(reminderQueryService.getSmartReminders(SmartView.ALL)).isEmpty();
        assertResultCode(() -> reminderService.toggleFlag(bobReminder.id()), ResultCode.NOT_FOUND);

        TestAuth.signIn(alice);
        assertThat(reminderQueryService.getReminders(sharedListId, null)).extracting(ReminderResponse::id)
                .containsExactly(aliceReminder.id(), bobReminder.id());
    }

    @Test
    @DisplayName("소유자가 리스트를 삭제하면 멤버 정보와 다른 멤버가 만든 리마인더도 함께 삭제된다")
    void deleteList_removesMembersAndAllReminders() {
        TestAuth.signIn(bob);
        reminderService.createReminder(new CreateReminderCommand("빵", null, sharedListId, null, null, null, null, null));
        TestAuth.signIn(alice);

        reminderListService.deleteList(sharedListId);

        assertThat(listMemberRepository.findByListIdAndUserId(sharedListId, bob.getId())).isEmpty();
        TestAuth.signIn(bob);
        assertThat(reminderListService.getLists()).isEmpty();
        assertThat(reminderQueryService.getSmartReminders(SmartView.ALL)).isEmpty();
    }

    @Test
    @DisplayName("편집자가 리마인더에 붙인 태그는 편집자의 태그로 만들어진다")
    void editorTags_areCreatedForEditor() {
        TestAuth.signIn(bob);
        ReminderResponse bobReminder = reminderService.createReminder(
                new CreateReminderCommand("빵", null, sharedListId, null, null, List.of("빵집"), null, null));

        assertThat(bobReminder.tags()).containsExactly("빵집");
        assertThat(tagService.getTags()).extracting(TagResponse::name).containsExactly("빵집");
    }

    private static void assertResultCode(Executable executable, ResultCode resultCode) {
        assertThatThrownBy(executable::execute)
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(resultCode);
    }
}

package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.repository.ListMemberRepository;
import demo.ai.reminder.repository.ReminderListSummary;
import demo.ai.reminder.repository.UserRepository;
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
    private ReminderListService reminderListService;

    @Autowired
    private ListMemberService listMemberService;

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
    private Reminder aliceReminder;
    private LocalDateTime today;

    @BeforeEach
    void setUp() {
        bob = userRepository.save(new User("bob@example.com", "{noop}password", "밥"));
        carol = userRepository.save(new User("carol@example.com", "{noop}password", "캐럴"));
        alice = TestAuth.signIn(userRepository, "alice@example.com");
        today = LocalDate.now().atTime(23, 59);
        sharedListId = reminderListService.createList(new ReminderListRequest("가족", "#34C759")).list().getId();
        aliceReminder = reminderService.createReminder(
                new ReminderRequest("우유", null, sharedListId, today, null, List.of("장보기"), null, null));
        listMemberService.invite(sharedListId, "bob@example.com");
    }

    @Test
    @DisplayName("초대받은 사용자의 리스트 목록에 공유 리스트가 EDITOR 역할로, 자기 리스트 뒤에 나온다")
    void getLists_includesSharedListAfterOwnLists() {
        TestAuth.signIn(bob);
        Long bobListId = reminderListService.createList(new ReminderListRequest("밥의 할 일", null)).list().getId();

        List<ReminderListSummary> lists = reminderListService.getLists();

        assertThat(lists).extracting(summary -> summary.list().getId()).containsExactly(bobListId, sharedListId);
        ReminderListSummary shared = lists.get(1);
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

        assertThat(reminderService.getReminders(sharedListId, null)).extracting(Reminder::getId)
                .containsExactly(aliceReminder.getId());
        Reminder bobReminder = reminderService.createReminder(
                new ReminderRequest("빵", null, sharedListId, null, null, null, null, null));
        Reminder subtask = reminderService.createReminder(
                new ReminderRequest("통밀", null, null, null, null, null, aliceReminder.getId(), null));
        reminderService.updateReminder(aliceReminder.getId(),
                new ReminderUpdateRequest("저지방 우유", null, today, false, null, null, null));
        reminderService.toggleFlag(aliceReminder.getId());
        reminderService.reorderReminders(sharedListId, List.of(bobReminder.getId(), aliceReminder.getId()));
        reminderService.toggleComplete(subtask.getId());
        reminderService.deleteReminder(subtask.getId());

        TestAuth.signIn(alice);
        List<Reminder> reminders = reminderService.getReminders(sharedListId, null);
        assertThat(reminders).extracting(Reminder::getTitle).containsExactly("빵", "저지방 우유");
        assertThat(reminders.get(1).isFlagged()).isTrue();
        assertThat(reminders.get(1).getSubtasks()).isEmpty();
    }

    @Test
    @DisplayName("소유자는 편집자가 만든 리마인더도 수정/삭제할 수 있다")
    void owner_canManageEditorsReminders() {
        TestAuth.signIn(bob);
        Reminder bobReminder = reminderService.createReminder(
                new ReminderRequest("빵", null, sharedListId, null, null, null, null, null));

        TestAuth.signIn(alice);
        reminderService.toggleComplete(bobReminder.getId());
        reminderService.deleteReminder(bobReminder.getId());

        assertThat(reminderService.getReminders(sharedListId, null)).extracting(Reminder::getId)
                .containsExactly(aliceReminder.getId());
    }

    @Test
    @DisplayName("편집자는 리스트 이름 변경/삭제를 할 수 없고 (403), 공유 리스트는 사이드바 순서 변경 대상이 아니다 (400)")
    void editor_cannotChangeTheListItself() {
        TestAuth.signIn(bob);

        assertResultCode(() -> reminderListService.updateList(sharedListId, new ReminderListRequest("바꿈", null)),
                ResultCode.FORBIDDEN);
        assertResultCode(() -> reminderListService.deleteList(sharedListId), ResultCode.FORBIDDEN);
        assertResultCode(() -> reminderListService.reorderLists(List.of(sharedListId)), ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("스마트 뷰, 태그별/전체 조회, 다가오는 리마인더에 공유 리스트의 리마인더가 포함된다")
    void smartViews_includeSharedListReminders() {
        reminderService.toggleFlag(aliceReminder.getId());
        Reminder done = reminderService.createReminder(
                new ReminderRequest("세제", null, sharedListId, null, null, null, null, null));
        reminderService.toggleComplete(done.getId());
        TestAuth.signIn(bob);

        for (String view : List.of("today", "scheduled", "all", "flagged")) {
            assertThat(reminderService.getSmartReminders(view)).as(view).extracting(Reminder::getId)
                    .containsExactly(aliceReminder.getId());
        }
        assertThat(reminderService.getSmartReminders("completed")).extracting(Reminder::getId)
                .containsExactly(done.getId());
        assertThat(reminderService.getReminders(null, null)).extracting(Reminder::getId)
                .containsExactlyInAnyOrder(aliceReminder.getId(), done.getId());
        assertThat(reminderService.getReminders(null, "장보기")).extracting(Reminder::getId)
                .containsExactly(aliceReminder.getId());
        LocalDateTime now = LocalDateTime.now();
        assertThat(reminderService.getUpcomingReminders(now.minusDays(1), now.plusDays(2)))
                .extracting(Reminder::getId).containsExactly(aliceReminder.getId());
    }

    @Test
    @DisplayName("공유 리스트와 관계없는 리마인더(리스트 없음)는 다른 멤버에게 보이지 않는다")
    void remindersWithoutList_stayPrivate() {
        Reminder privateReminder = reminderService.createReminder(
                new ReminderRequest("개인 메모", null, null, today, null, null, null, null));
        TestAuth.signIn(bob);

        assertThat(reminderService.getSmartReminders("all")).extracting(Reminder::getId)
                .doesNotContain(privateReminder.getId());
        assertResultCode(() -> reminderService.toggleFlag(privateReminder.getId()), ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("멤버가 아닌 사용자는 공유 리스트와 그 리마인더에 접근할 수 없다 (404)")
    void nonMember_cannotAccessSharedList() {
        TestAuth.signIn(carol);

        assertThat(reminderListService.getLists()).isEmpty();
        assertThat(reminderService.getSmartReminders("all")).isEmpty();
        assertThat(reminderService.getReminders(null, "장보기")).isEmpty();
        List<Executable> attempts = List.of(
                () -> reminderService.getReminders(sharedListId, null),
                () -> reminderService.createReminder(
                        new ReminderRequest("몰래", null, sharedListId, null, null, null, null, null)),
                () -> reminderService.createReminder(
                        new ReminderRequest("몰래", null, null, null, null, null, aliceReminder.getId(), null)),
                () -> reminderService.updateReminder(aliceReminder.getId(),
                        new ReminderUpdateRequest("몰래", null, null, false, null, null, null)),
                () -> reminderService.toggleComplete(aliceReminder.getId()),
                () -> reminderService.deleteReminder(aliceReminder.getId()),
                () -> reminderService.reorderReminders(sharedListId, List.of(aliceReminder.getId())),
                () -> reminderListService.updateList(sharedListId, new ReminderListRequest("몰래", null)),
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
        Reminder bobReminder = reminderService.createReminder(
                new ReminderRequest("빵", null, sharedListId, today, null, null, null, null));
        TestAuth.signIn(alice);
        listMemberService.removeMember(sharedListId, bob.getId());

        TestAuth.signIn(bob);
        assertThat(reminderService.getSmartReminders("all")).isEmpty();
        assertResultCode(() -> reminderService.toggleFlag(bobReminder.getId()), ResultCode.NOT_FOUND);

        TestAuth.signIn(alice);
        assertThat(reminderService.getReminders(sharedListId, null)).extracting(Reminder::getId)
                .containsExactly(aliceReminder.getId(), bobReminder.getId());
    }

    @Test
    @DisplayName("소유자가 리스트를 삭제하면 멤버 정보와 다른 멤버가 만든 리마인더도 함께 삭제된다")
    void deleteList_removesMembersAndAllReminders() {
        TestAuth.signIn(bob);
        reminderService.createReminder(new ReminderRequest("빵", null, sharedListId, null, null, null, null, null));
        TestAuth.signIn(alice);

        reminderListService.deleteList(sharedListId);

        assertThat(listMemberRepository.existsByListIdAndUserId(sharedListId, bob.getId())).isFalse();
        TestAuth.signIn(bob);
        assertThat(reminderListService.getLists()).isEmpty();
        assertThat(reminderService.getSmartReminders("all")).isEmpty();
    }

    @Test
    @DisplayName("편집자가 리마인더에 붙인 태그는 편집자의 태그로 만들어진다")
    void editorTags_areCreatedForEditor() {
        TestAuth.signIn(bob);
        Reminder bobReminder = reminderService.createReminder(
                new ReminderRequest("빵", null, sharedListId, null, null, List.of("빵집"), null, null));

        assertThat(bobReminder.getTags()).singleElement()
                .satisfies(tag -> assertThat(tag.getUser().getId()).isEqualTo(bob.getId()));
        assertThat(tagService.getTags()).extracting(summary -> summary.tag().getName()).containsExactly("빵집");
    }

    private static void assertResultCode(Executable executable, ResultCode resultCode) {
        assertThatThrownBy(executable::execute)
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode")
                .isEqualTo(resultCode);
    }
}

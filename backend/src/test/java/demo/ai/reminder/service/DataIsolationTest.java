package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.dto.ReminderListResponse;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderResponse;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.dto.TagResponse;
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

// 두 사용자(앨리스, 밥)의 데이터가 서로 보이지 않고, 다른 사용자의 리소스는 404로 응답하는지 검증한다.
@SpringBootTest
@Transactional
class DataIsolationTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderListService reminderListService;

    @Autowired
    private TagService tagService;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private Long aliceListId;
    private ReminderResponse aliceReminder;
    private Long aliceTagId;

    @BeforeEach
    void setUpAliceData() {
        bob = userRepository.save(new User("bob@example.com", "{noop}password", "밥"));
        alice = TestAuth.signIn(userRepository, "alice@example.com");
        LocalDateTime today = LocalDate.now().atTime(23, 59);
        aliceListId = reminderListService.createList(new ReminderListRequest("앨리스 장보기", null)).id();
        aliceReminder = reminderService.createReminder(
                new ReminderRequest("우유", null, aliceListId, today, null, List.of("집"), null, null));
        reminderService.toggleFlag(aliceReminder.id());
        aliceTagId = tagService.getTags().getFirst().id();
    }

    @Test
    @DisplayName("다른 사용자에게는 리스트/리마인더/태그/스마트 뷰/다가오는 리마인더가 모두 비어 있다")
    void otherUser_seesNoneOfTheData() {
        TestAuth.signIn(bob);

        assertThat(reminderListService.getLists()).isEmpty();
        assertThat(reminderService.getReminders(null, null)).isEmpty();
        assertThat(reminderService.getReminders(null, "집")).isEmpty();
        for (String view : List.of("today", "scheduled", "all", "flagged", "completed")) {
            assertThat(reminderService.getSmartReminders(view)).as(view).isEmpty();
        }
        LocalDateTime now = LocalDateTime.now();
        assertThat(reminderService.getUpcomingReminders(now.minusDays(1), now.plusDays(1))).isEmpty();
        assertThat(tagService.getTags()).isEmpty();
    }

    @Test
    @DisplayName("소유자에게는 자기 데이터가 그대로 보인다")
    void owner_seesOwnData() {
        assertThat(reminderListService.getLists()).extracting(ReminderListResponse::name)
                .containsExactly("앨리스 장보기");
        assertThat(reminderService.getReminders(aliceListId, null)).extracting(ReminderResponse::title)
                .containsExactly("우유");
        assertThat(reminderService.getSmartReminders("flagged")).extracting(ReminderResponse::title)
                .containsExactly("우유");
        assertThat(tagService.getTags()).extracting(TagResponse::name).containsExactly("집");
    }

    @Test
    @DisplayName("다른 사용자의 리스트/리마인더/태그를 조회·수정·삭제하면 404 예외가 발생한다")
    void otherUser_getsNotFound_forEveryResourceOfOwner() {
        TestAuth.signIn(bob);
        Long reminderId = aliceReminder.id();

        List<Executable> attempts = List.of(
                () -> reminderService.getReminders(aliceListId, null),
                () -> reminderService.updateReminder(reminderId,
                        new ReminderUpdateRequest("가로채기", null, null, false, null, null, null)),
                () -> reminderService.toggleComplete(reminderId),
                () -> reminderService.toggleFlag(reminderId),
                () -> reminderService.deleteReminder(reminderId),
                () -> reminderService.reorderReminders(aliceListId, List.of(reminderId)),
                () -> reminderService.createReminder(
                        new ReminderRequest("끼워넣기", null, aliceListId, null, null, null, null, null)),
                () -> reminderService.createReminder(
                        new ReminderRequest("하위 작업", null, null, null, null, null, reminderId, null)),
                () -> reminderListService.updateList(aliceListId, new ReminderListRequest("가로채기", null)),
                () -> reminderListService.deleteList(aliceListId),
                () -> tagService.deleteTag(aliceTagId));
        for (Executable attempt : attempts) {
            assertThatThrownBy(attempt::execute)
                    .isInstanceOf(BusinessException.class)
                    .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
        }

        TestAuth.signIn(alice);
        assertThat(reminderService.getReminders(aliceListId, null))
                .singleElement()
                .satisfies(reminder -> {
                    assertThat(reminder.title()).isEqualTo("우유");
                    assertThat(reminder.completed()).isFalse();
                    assertThat(reminder.flagged()).isTrue();
                });
    }

    @Test
    @DisplayName("리스트 순서 변경에 다른 사용자의 리스트 id를 넣으면 400 예외가 발생한다")
    void reorderLists_withOtherUsersListId_throwsBadRequest() {
        TestAuth.signIn(bob);
        Long bobListId = reminderListService.createList(new ReminderListRequest("밥 업무", null)).id();

        assertThatThrownBy(() -> reminderListService.reorderLists(List.of(bobListId, aliceListId)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("같은 이름의 태그도 사용자마다 따로 만들어진다")
    void tags_withSameName_areSeparatedPerUser() {
        TestAuth.signIn(bob);

        reminderService.createReminder(
                new ReminderRequest("빨래", null, null, null, null, List.of("집"), null, null));

        Long bobTagId = tagService.getTags().getFirst().id();
        assertThat(bobTagId).isNotEqualTo(aliceTagId);
        assertThat(tagService.getTags()).extracting(TagResponse::reminderCount).containsExactly(1L);
    }

    @Test
    @DisplayName("리스트 없는 리마인더와 리스트의 표시 순서는 사용자마다 0부터 매겨진다")
    void sortOrders_startFromZeroPerUser() {
        reminderService.createReminder(new ReminderRequest("앨리스 메모", null, null, null, null, null, null, null));
        TestAuth.signIn(bob);

        ReminderResponse bobReminder = reminderService.createReminder(
                new ReminderRequest("밥 메모", null, null, null, null, null, null, null));
        ReminderListResponse bobList = reminderListService.createList(new ReminderListRequest("밥 업무", null));

        assertThat(bobReminder.sortOrder()).isZero();
        assertThat(bobList.sortOrder()).isZero();
    }
}

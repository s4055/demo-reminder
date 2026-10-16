package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.request.ReminderRequest;
import demo.ai.reminder.dto.request.ReminderUpdateRequest;
import demo.ai.reminder.dto.response.ReminderResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReminderQueryServiceTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderQueryService reminderQueryService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void signIn() {
        owner = TestAuth.signIn(userRepository, "owner@example.com");
    }

    @Test
    @DisplayName("전체 리마인더 목록을 조회한다")
    void getReminders_returnsAllReminders() {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));

        List<ReminderResponse> result = reminderQueryService.getReminders(null, null);

        assertThat(result).extracting(ReminderResponse::title).contains("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하면 해당 리스트의 리마인더만 조회한다")
    void getReminders_returnsOnlyRemindersOfGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", "#FF9500", null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", "#007AFF", null, 0));
        reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null, null, null));
        reminderRepository.save(new Reminder(owner, "보고서 작성", null, work, null, null, null));

        List<ReminderResponse> result = reminderQueryService.getReminders(shopping.getId(), null);

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하지 않으면 리스트와 상관없이 전체를 조회한다")
    void getReminders_returnsRemindersOfAllListsAndWithoutList_whenListIdIsNull() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null, null, null));
        reminderRepository.save(new Reminder(owner, "보고서 작성", null, null, null, null, null));

        List<ReminderResponse> result = reminderQueryService.getReminders(null, null);

        assertThat(result).extracting(ReminderResponse::title).contains("우유 사기", "보고서 작성");
    }

    @Test
    @DisplayName("today 뷰는 마감일이 오늘인 미완료 리마인더만 조회한다")
    void getSmartReminders_today_returnsIncompleteRemindersDueToday() {
        LocalDate today = LocalDate.now();
        reminderRepository.save(new Reminder(owner, "오늘 아침", null, null, today.atTime(0, 0), null, null));
        reminderRepository.save(new Reminder(owner, "오늘 저녁", null, null, today.atTime(23, 59), null, null));
        reminderRepository.save(new Reminder(owner, "어제", null, null, today.minusDays(1).atTime(23, 59), null, null));
        reminderRepository.save(new Reminder(owner, "내일", null, null, today.plusDays(1).atStartOfDay(), null, null));
        reminderRepository.save(new Reminder(owner, "마감 없음", null, null, null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "오늘 완료", null, null, today.atTime(12, 0), null, null));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderQueryService.getSmartReminders("today");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("오늘 아침", "오늘 저녁");
    }

    @Test
    @DisplayName("scheduled 뷰는 마감일이 설정된 모든 미완료 리마인더를 마감일 순으로 조회한다")
    void getSmartReminders_scheduled_returnsIncompleteRemindersWithDueAt() {
        LocalDate today = LocalDate.now();
        reminderRepository.save(new Reminder(owner, "다음주", null, null, today.plusDays(7).atTime(9, 0), null, null));
        reminderRepository.save(new Reminder(owner, "오늘", null, null, today.atTime(9, 0), null, null));
        reminderRepository.save(new Reminder(owner, "마감 없음", null, null, null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "완료", null, null, today.atTime(10, 0), null, null));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderQueryService.getSmartReminders("scheduled");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("오늘", "다음주");
    }

    @Test
    @DisplayName("all 뷰는 모든 미완료 리마인더를 조회한다")
    void getSmartReminders_all_returnsAllIncompleteReminders() {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "완료", null, null, null, null, null));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderQueryService.getSmartReminders("all");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("flagged 뷰는 플래그가 지정된 미완료 리마인더만 조회한다")
    void getSmartReminders_flagged_returnsFlaggedIncompleteReminders() {
        Reminder flagged = reminderRepository.save(new Reminder(owner, "중요", null, null, null, null, null));
        flagged.toggleFlag();
        Reminder flaggedDone = reminderRepository.save(new Reminder(owner, "중요하지만 완료", null, null, null, null, null));
        flaggedDone.toggleFlag();
        flaggedDone.toggleComplete(LocalDateTime.now());
        reminderRepository.save(new Reminder(owner, "일반", null, null, null, null, null));

        List<ReminderResponse> result = reminderQueryService.getSmartReminders("flagged");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("중요");
    }

    @Test
    @DisplayName("completed 뷰는 완료된 리마인더만 조회한다")
    void getSmartReminders_completed_returnsCompletedReminders() {
        reminderRepository.save(new Reminder(owner, "미완료", null, null, null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "완료", null, null, null, null, null));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderQueryService.getSmartReminders("completed");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("완료");
    }

    @Test
    @DisplayName("completed 뷰는 생성 순서와 관계없이 완료일시 최신순으로 조회한다")
    void getSmartReminders_completed_ordersByCompletedAtDesc() {
        Reminder first = reminderRepository.save(new Reminder(owner, "먼저 생성", null, null, null, null, null));
        Reminder second = reminderRepository.save(new Reminder(owner, "나중 생성", null, null, null, null, null));
        second.toggleComplete(LocalDateTime.of(2026, 9, 30, 9, 0));
        first.toggleComplete(LocalDateTime.of(2026, 9, 30, 10, 0));

        List<ReminderResponse> result = reminderQueryService.getSmartReminders("completed");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("먼저 생성", "나중 생성");
    }

    @Test
    @DisplayName("완료된 리마인더를 수정해도 completed 뷰의 순서는 바뀌지 않는다")
    void getSmartReminders_completed_orderIsNotAffectedByUpdate() {
        Reminder older = reminderRepository.save(new Reminder(owner, "먼저 완료", null, null, null, null, null));
        Reminder newer = reminderRepository.save(new Reminder(owner, "나중 완료", null, null, null, null, null));
        older.toggleComplete(LocalDateTime.of(2026, 9, 30, 9, 0));
        newer.toggleComplete(LocalDateTime.of(2026, 9, 30, 10, 0));
        reminderRepository.flush();

        reminderService.updateReminder(older.getId(), new ReminderUpdateRequest("먼저 완료(수정)", null, null, false, null, null, null, older.getVersion()));
        reminderRepository.flush();
        List<ReminderResponse> result = reminderQueryService.getSmartReminders("completed");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("나중 완료", "먼저 완료(수정)");
    }

    @Test
    @DisplayName("스마트 뷰 이름은 대소문자를 구분하지 않는다")
    void getSmartReminders_isCaseInsensitive() {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));

        List<ReminderResponse> result = reminderQueryService.getSmartReminders("ALL");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("알 수 없는 스마트 뷰를 요청하면 400 예외가 발생한다")
    void getSmartReminders_throwsBadRequest_whenViewIsUnknown() {
        assertThatThrownBy(() -> reminderQueryService.getSmartReminders("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("태그로 조회하면 해당 태그가 붙은 리마인더만 생성순으로 반환한다")
    void getReminders_byTag_returnsOnlyTaggedReminders() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        reminderService.createReminder(new ReminderRequest("우유", null, shopping.getId(), null, null, List.of("집"), null, null));
        reminderService.createReminder(new ReminderRequest("보고서", null, null, null, null, List.of("회사"), null, null));
        reminderService.createReminder(new ReminderRequest("빨래", null, null, null, null, List.of("집", "주말"), null, null));

        List<ReminderResponse> result = reminderQueryService.getReminders(null, "집");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유", "빨래");
    }

    @Test
    @DisplayName("리스트별 조회는 최상위 리마인더만 반환하고, 하위 작업은 부모의 subtasks에 순서대로 담긴다")
    void getReminders_byList_returnsTopLevelOnly_withSubtasks() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse parent = create("이사 준비", home.getId());
        createSubtask("박스 구하기", parent.id());
        createSubtask("이삿짐센터 예약", parent.id());
        create("빨래", home.getId());
        entityManager.flush();
        entityManager.clear();

        List<ReminderResponse> result = reminderQueryService.getReminders(home.getId(), null);

        assertThat(result).extracting(ReminderResponse::title).containsExactly("이사 준비", "빨래");
        assertThat(result.get(0).subtasks())
                .extracting(ReminderResponse::title).containsExactly("박스 구하기", "이삿짐센터 예약");
    }

    @Test
    @DisplayName("스마트 뷰는 하위 작업도 개별 항목으로 포함한다")
    void getSmartReminders_includesSubtasksAsIndividualItems() {
        ReminderResponse parent = create("이사 준비", null);
        createSubtask("박스 구하기", parent.id());

        assertThat(reminderQueryService.getSmartReminders("all"))
                .extracting(ReminderResponse::title).contains("이사 준비", "박스 구하기");
    }

    @Test
    @DisplayName("다가오는 리마인더 조회는 from 이상 to 미만에 마감되는 항목을 마감일시 순으로 반환한다")
    void getUpcomingReminders_includesFromAndExcludesTo_sortedByDueAt() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 4, 21, 0);
        LocalDateTime to = from.plusMinutes(2);
        reminderRepository.save(new Reminder(owner, "직전", null, null, from.minusSeconds(1), null, null));
        reminderRepository.save(new Reminder(owner, "마지막", null, null, to.minusSeconds(1), null, null));
        reminderRepository.save(new Reminder(owner, "시작", null, null, from, null, null));
        reminderRepository.save(new Reminder(owner, "끝", null, null, to, null, null));
        reminderRepository.save(new Reminder(owner, "마감일 없음", null, null, null, null, null));

        List<ReminderResponse> result = reminderQueryService.getUpcomingReminders(from, to);

        assertThat(result).extracting(ReminderResponse::title).containsExactly("시작", "마지막");
    }

    @Test
    @DisplayName("다가오는 리마인더 조회는 완료된 항목을 제외한다")
    void getUpcomingReminders_excludesCompleted() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 4, 21, 0);
        Reminder done = reminderRepository.save(new Reminder(owner, "완료함", null, null, from.plusMinutes(1), null, null));
        done.toggleComplete(LocalDateTime.of(2026, 10, 4, 20, 0));
        reminderRepository.save(new Reminder(owner, "남음", null, null, from.plusMinutes(1), null, null));

        assertThat(reminderQueryService.getUpcomingReminders(from, from.plusMinutes(2)))
                .extracting(ReminderResponse::title).containsExactly("남음");
    }

    @Test
    @DisplayName("다가오는 리마인더 조회는 하위 작업도 개별 항목으로 포함한다")
    void getUpcomingReminders_includesSubtasks() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 4, 21, 0);
        ReminderResponse parent = create("이사 준비", null);
        reminderService.createReminder(new ReminderRequest(
                "박스 구하기", null, null, from.plusMinutes(1), null, null, parent.id(), null));

        assertThat(reminderQueryService.getUpcomingReminders(from, from.plusMinutes(2)))
                .extracting(ReminderResponse::title).containsExactly("박스 구하기");
    }

    @Test
    @DisplayName("from이 to보다 늦거나 같으면 400 예외가 발생한다")
    void getUpcomingReminders_throwsBadRequest_whenFromIsNotBeforeTo() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 4, 21, 0);

        assertThatThrownBy(() -> reminderQueryService.getUpcomingReminders(time, time))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
        assertThatThrownBy(() -> reminderQueryService.getUpcomingReminders(time.plusMinutes(1), time))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("검색어가 제목, 메모, 태그 이름 중 하나에 들어 있으면 검색된다")
    void searchReminders_matchesTitleMemoAndTag() {
        create("우유 사기", null);
        reminderService.createReminder(new ReminderRequest("장보기", "우유랑 빵", null, null, null, null, null, null));
        reminderService.createReminder(
                new ReminderRequest("마트", null, null, null, null, List.of("우유팩"), null, null));
        create("보고서 작성", null);

        List<ReminderResponse> result = reminderQueryService.searchReminders("우유");

        assertThat(result).extracting(ReminderResponse::title)
                .containsExactlyInAnyOrder("우유 사기", "장보기", "마트");
    }

    @Test
    @DisplayName("검색은 대소문자를 구분하지 않고, 검색어 앞뒤 공백은 무시한다")
    void searchReminders_isCaseInsensitive_andTrimsQuery() {
        create("Buy MILK", null);
        reminderService.createReminder(
                new ReminderRequest("마트", null, null, null, null, List.of("Grocery"), null, null));

        assertThat(reminderQueryService.searchReminders("  milk ")).extracting(ReminderResponse::title)
                .containsExactly("Buy MILK");
        assertThat(reminderQueryService.searchReminders("GROCERY")).extracting(ReminderResponse::title)
                .containsExactly("마트");
    }

    @Test
    @DisplayName("%와 _는 와일드카드가 아니라 문자 그대로 검색된다")
    void searchReminders_treatsLikeWildcardsLiterally() {
        create("할인 50% 쿠폰", null);
        create("할인 500원", null);
        create("file_name 정리", null);
        create("filename 정리", null);

        assertThat(reminderQueryService.searchReminders("50%")).extracting(ReminderResponse::title)
                .containsExactly("할인 50% 쿠폰");
        assertThat(reminderQueryService.searchReminders("file_")).extracting(ReminderResponse::title)
                .containsExactly("file_name 정리");
    }

    @Test
    @DisplayName("태그가 여러 개 일치해도 리마인더는 한 번만 나오고, 하위 작업도 개별 항목으로 검색된다")
    void searchReminders_returnsEachReminderOnce_includingSubtasks() {
        ReminderResponse parent = reminderService.createReminder(
                new ReminderRequest("이사", null, null, null, null, List.of("짐싸기", "짐정리"), null, null));
        createSubtask("짐 박스 구하기", parent.id());

        assertThat(reminderQueryService.searchReminders("짐")).extracting(ReminderResponse::title)
                .containsExactly("이사", "짐 박스 구하기");
    }

    @Test
    @DisplayName("미완료 항목이 마감일시 순(마감일 없는 항목은 뒤, 같으면 생성순)으로 먼저, 완료 항목이 완료일시 최신순으로 나중에 나온다")
    void searchReminders_ordersIncompleteByDueAtThenCompletedByRecency() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 10, 9, 0);
        ReminderResponse noDue = create("업무 A", null);
        ReminderResponse later = createWithDueAt("업무 B", dueAt.plusDays(1));
        ReminderResponse sooner = createWithDueAt("업무 C", dueAt);
        ReminderResponse noDue2 = create("업무 D", null);
        ReminderResponse completedFirst = create("업무 E", null);
        ReminderResponse completedSecond = create("업무 F", null);
        reminderService.toggleComplete(completedFirst.id());
        entityManager.flush();
        entityManager.createQuery("update Reminder r set r.completedAt = :at where r.id = :id")
                .setParameter("at", dueAt.minusDays(1))
                .setParameter("id", completedFirst.id())
                .executeUpdate();
        entityManager.clear();
        reminderService.toggleComplete(completedSecond.id());

        List<ReminderResponse> result = reminderQueryService.searchReminders("업무");

        assertThat(result).extracting(ReminderResponse::id).containsExactly(
                sooner.id(), later.id(), noDue.id(), noDue2.id(), completedSecond.id(), completedFirst.id());
    }

    @Test
    @DisplayName("검색어가 비어 있거나 100자를 넘으면 400 예외가 발생한다")
    void searchReminders_throwsBadRequest_whenQueryIsBlankOrTooLong() {
        for (String query : new String[]{"", "   ", "가".repeat(101)}) {
            assertThatThrownBy(() -> reminderQueryService.searchReminders(query))
                    .isInstanceOf(BusinessException.class)
                    .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
        }
        assertThat(reminderQueryService.searchReminders("가".repeat(100))).isEmpty();
    }

    private ReminderResponse create(String title, Long listId) {
        return reminderService.createReminder(new ReminderRequest(title, null, listId, null, null, null, null, null));
    }

    private ReminderResponse createWithDueAt(String title, LocalDateTime dueAt) {
        return reminderService.createReminder(new ReminderRequest(title, null, null, dueAt, null, null, null, null));
    }

    private ReminderResponse createSubtask(String title, Long parentId) {
        return reminderService.createReminder(new ReminderRequest(title, null, null, null, null, null, parentId, null));
    }
}

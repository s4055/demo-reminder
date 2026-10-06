package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.domain.Tag;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderResponse;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.TagRepository;
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
class ReminderServiceTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private ReminderListRepository reminderListRepository;

    @Autowired
    private TagRepository tagRepository;

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
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));

        List<ReminderResponse> result = reminderService.getReminders(null, null);

        assertThat(result).extracting(ReminderResponse::title).contains("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하면 해당 리스트의 리마인더만 조회한다")
    void getReminders_returnsOnlyRemindersOfGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", "#FF9500"));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", "#007AFF"));
        reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null));
        reminderRepository.save(new Reminder(owner, "보고서 작성", null, work, null));

        List<ReminderResponse> result = reminderService.getReminders(shopping.getId(), null);

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("listId를 지정하지 않으면 리스트와 상관없이 전체를 조회한다")
    void getReminders_returnsRemindersOfAllListsAndWithoutList_whenListIdIsNull() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null));
        reminderRepository.save(new Reminder(owner, "보고서 작성", null, null, null));

        List<ReminderResponse> result = reminderService.getReminders(null, null);

        assertThat(result).extracting(ReminderResponse::title).contains("우유 사기", "보고서 작성");
    }

    @Test
    @DisplayName("listId를 지정해 리마인더를 생성하면 해당 리스트에 소속된다")
    void createReminder_belongsToGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));

        ReminderResponse result = reminderService.createReminder(new ReminderRequest("우유 사기", null, shopping.getId(), null, null, null, null, null));

        assertThat(result.listId()).isEqualTo(shopping.getId());
    }

    @Test
    @DisplayName("존재하지 않는 리스트로 리마인더를 생성하면 404 예외가 발생한다")
    void createReminder_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderService.createReminder(new ReminderRequest("우유 사기", null, -1L, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("요청으로 받은 제목과 메모로 리마인더를 생성한다")
    void createReminder_savesReminderWithGivenTitleAndMemo() {
        ReminderRequest request = new ReminderRequest("우유 사기", "저지방", null, null, null, null, null, null);

        ReminderResponse result = reminderService.createReminder(request);

        assertThat(result.id()).isNotNull();
        assertThat(result.title()).isEqualTo("우유 사기");
        assertThat(result.memo()).isEqualTo("저지방");
        assertThat(result.completed()).isFalse();
        assertThat(reminderRepository.findById(result.id())).isPresent();
    }

    @Test
    @DisplayName("존재하는 리마인더의 완료 상태를 토글한다")
    void toggleComplete_flipsCompletedState_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));

        ReminderResponse result = reminderService.toggleComplete(saved.getId());

        assertThat(result.completed()).isTrue();
    }

    @Test
    @DisplayName("완료 토글 시 완료일시가 기록되고, 다시 토글하면 비워진다")
    void toggleComplete_recordsAndClearsCompletedAt() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));
        LocalDateTime before = LocalDateTime.now();

        ReminderResponse completed = reminderService.toggleComplete(saved.getId());

        assertThat(completed.completedAt()).isAfterOrEqualTo(before);

        ReminderResponse uncompleted = reminderService.toggleComplete(saved.getId());

        assertThat(uncompleted.completedAt()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 토글하면 404 예외가 발생한다")
    void toggleComplete_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.toggleComplete(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("마감일시를 지정해 리마인더를 생성하면 마감일시가 저장되고 플래그는 꺼져 있다")
    void createReminder_savesDueAt_andIsNotFlagged() {
        LocalDateTime dueAt = LocalDate.now().atTime(18, 30);

        ReminderResponse result = reminderService.createReminder(new ReminderRequest("우유 사기", null, null, dueAt, null, null, null, null));

        assertThat(result.dueAt()).isEqualTo(dueAt);
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("리마인더를 생성하면 JPA Auditing이 생성일과 수정일을 채운다")
    void createReminder_fillsCreatedAtAndUpdatedAt() {
        LocalDateTime before = LocalDateTime.now();

        ReminderResponse result = reminderService.createReminder(new ReminderRequest("우유 사기", null, null, null, null, null, null, null));

        assertThat(result.createdAt()).isNotNull().isAfterOrEqualTo(before);
        assertThat(result.updatedAt()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("리마인더를 수정하면 수정일은 갱신되고 생성일은 유지된다")
    void updateReminder_refreshesUpdatedAt_andKeepsCreatedAt() {
        Reminder saved = reminderRepository.saveAndFlush(new Reminder(owner, "우유 사기", null, null, null));
        LocalDateTime updatedAt = backdateUpdatedAt(saved.getId());
        LocalDateTime createdAt = reminderRepository.findById(saved.getId()).orElseThrow().getCreatedAt();

        reminderService.updateReminder(saved.getId(), new ReminderUpdateRequest("계란 사기", null, null, false, null, null, null));

        Reminder result = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    @DisplayName("수정/완료/플래그 토글의 응답에는 갱신된 수정일이 담긴다")
    void mutatingMethods_returnRefreshedUpdatedAt() {
        Long toUpdate = reminderRepository.saveAndFlush(new Reminder(owner, "우유 사기", null, null, null)).getId();
        Long toComplete = reminderRepository.saveAndFlush(new Reminder(owner, "계란 사기", null, null, null)).getId();
        Long toFlag = reminderRepository.saveAndFlush(new Reminder(owner, "빵 사기", null, null, null)).getId();
        LocalDateTime past = backdateUpdatedAt(toUpdate);
        backdateUpdatedAt(toComplete);
        backdateUpdatedAt(toFlag);

        ReminderResponse updated = reminderService.updateReminder(
                toUpdate, new ReminderUpdateRequest("우유 사기(수정)", null, null, false, null, null, null));
        ReminderResponse completed = reminderService.toggleComplete(toComplete);
        ReminderResponse flagged = reminderService.toggleFlag(toFlag);

        assertThat(updated.updatedAt()).isAfter(past);
        assertThat(completed.updatedAt()).isAfter(past);
        assertThat(flagged.updatedAt()).isAfter(past);
    }

    @Test
    @DisplayName("존재하는 리마인더의 제목, 메모, 마감일시, 플래그를 수정한다")
    void updateReminder_changesEditableFields_whenReminderExists() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null));
        LocalDateTime dueAt = LocalDate.now().atTime(18, 30);

        ReminderResponse result = reminderService.updateReminder(
                saved.getId(), new ReminderUpdateRequest("계란 사기", "12구", dueAt, true, null, null, null));

        assertThat(result.title()).isEqualTo("계란 사기");
        assertThat(result.memo()).isEqualTo("12구");
        assertThat(result.dueAt()).isEqualTo(dueAt);
        assertThat(result.flagged()).isTrue();
        assertThat(result.listId()).isEqualTo(shopping.getId());
        assertThat(reminderRepository.findById(saved.getId()).orElseThrow().getTitle()).isEqualTo("계란 사기");
    }

    @Test
    @DisplayName("수정한 마감일시와 플래그는 스마트 뷰 조회에 반영된다")
    void updateReminder_isReflectedInSmartViews() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));

        reminderService.updateReminder(
                saved.getId(), new ReminderUpdateRequest("우유 사기", null, LocalDate.now().atTime(9, 0), true, null, null, null));

        assertThat(reminderService.getSmartReminders("today")).extracting(ReminderResponse::title).containsExactly("우유 사기");
        assertThat(reminderService.getSmartReminders("flagged")).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("우선순위를 지정해 리마인더를 생성하면 우선순위가 저장된다")
    void createReminder_savesGivenPriority() {
        ReminderResponse result = reminderService.createReminder(new ReminderRequest("우유 사기", null, null, null, Priority.HIGH, null, null, null));
        reminderRepository.flush();

        assertThat(reminderRepository.findById(result.id()).orElseThrow().getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("우선순위를 생략하고 리마인더를 생성하면 없음(NONE)으로 저장된다")
    void createReminder_savesNonePriority_whenPriorityIsOmitted() {
        ReminderResponse result = reminderService.createReminder(new ReminderRequest("우유 사기", null, null, null, null, null, null, null));

        assertThat(result.priority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("리마인더를 수정하면 우선순위가 변경되어 저장된다")
    void updateReminder_savesChangedPriority() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, Priority.LOW));

        reminderService.updateReminder(saved.getId(), new ReminderUpdateRequest("우유 사기", null, null, false, Priority.MEDIUM, null, null));
        reminderRepository.flush();

        assertThat(reminderRepository.findById(saved.getId()).orElseThrow().getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("수정 요청에서 우선순위를 생략하면 없음(NONE)으로 바뀐다")
    void updateReminder_resetsPriorityToNone_whenPriorityIsOmitted() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, Priority.HIGH));

        ReminderResponse result = reminderService.updateReminder(saved.getId(), new ReminderUpdateRequest("우유 사기", null, null, false, null, null, null));

        assertThat(result.priority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 수정하면 404 예외가 발생한다")
    void updateReminder_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.updateReminder(-1L, new ReminderUpdateRequest("우유 사기", null, null, false, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("존재하는 리마인더의 플래그 상태를 토글한다")
    void toggleFlag_flipsFlaggedState_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));

        ReminderResponse result = reminderService.toggleFlag(saved.getId());

        assertThat(result.flagged()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 리마인더의 플래그를 토글하면 404 예외가 발생한다")
    void toggleFlag_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.toggleFlag(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("today 뷰는 마감일이 오늘인 미완료 리마인더만 조회한다")
    void getSmartReminders_today_returnsIncompleteRemindersDueToday() {
        LocalDate today = LocalDate.now();
        reminderRepository.save(new Reminder(owner, "오늘 아침", null, null, today.atTime(0, 0)));
        reminderRepository.save(new Reminder(owner, "오늘 저녁", null, null, today.atTime(23, 59)));
        reminderRepository.save(new Reminder(owner, "어제", null, null, today.minusDays(1).atTime(23, 59)));
        reminderRepository.save(new Reminder(owner, "내일", null, null, today.plusDays(1).atStartOfDay()));
        reminderRepository.save(new Reminder(owner, "마감 없음", null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "오늘 완료", null, null, today.atTime(12, 0)));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderService.getSmartReminders("today");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("오늘 아침", "오늘 저녁");
    }

    @Test
    @DisplayName("scheduled 뷰는 마감일이 설정된 모든 미완료 리마인더를 마감일 순으로 조회한다")
    void getSmartReminders_scheduled_returnsIncompleteRemindersWithDueAt() {
        LocalDate today = LocalDate.now();
        reminderRepository.save(new Reminder(owner, "다음주", null, null, today.plusDays(7).atTime(9, 0)));
        reminderRepository.save(new Reminder(owner, "오늘", null, null, today.atTime(9, 0)));
        reminderRepository.save(new Reminder(owner, "마감 없음", null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "완료", null, null, today.atTime(10, 0)));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderService.getSmartReminders("scheduled");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("오늘", "다음주");
    }

    @Test
    @DisplayName("all 뷰는 모든 미완료 리마인더를 조회한다")
    void getSmartReminders_all_returnsAllIncompleteReminders() {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "완료", null, null, null));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderService.getSmartReminders("all");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("flagged 뷰는 플래그가 지정된 미완료 리마인더만 조회한다")
    void getSmartReminders_flagged_returnsFlaggedIncompleteReminders() {
        Reminder flagged = reminderRepository.save(new Reminder(owner, "중요", null, null, null));
        flagged.toggleFlag();
        Reminder flaggedDone = reminderRepository.save(new Reminder(owner, "중요하지만 완료", null, null, null));
        flaggedDone.toggleFlag();
        flaggedDone.toggleComplete(LocalDateTime.now());
        reminderRepository.save(new Reminder(owner, "일반", null, null, null));

        List<ReminderResponse> result = reminderService.getSmartReminders("flagged");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("중요");
    }

    @Test
    @DisplayName("completed 뷰는 완료된 리마인더만 조회한다")
    void getSmartReminders_completed_returnsCompletedReminders() {
        reminderRepository.save(new Reminder(owner, "미완료", null, null, null));
        Reminder done = reminderRepository.save(new Reminder(owner, "완료", null, null, null));
        done.toggleComplete(LocalDateTime.now());

        List<ReminderResponse> result = reminderService.getSmartReminders("completed");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("완료");
    }

    @Test
    @DisplayName("completed 뷰는 생성 순서와 관계없이 완료일시 최신순으로 조회한다")
    void getSmartReminders_completed_ordersByCompletedAtDesc() {
        Reminder first = reminderRepository.save(new Reminder(owner, "먼저 생성", null, null, null));
        Reminder second = reminderRepository.save(new Reminder(owner, "나중 생성", null, null, null));
        second.toggleComplete(LocalDateTime.of(2026, 9, 30, 9, 0));
        first.toggleComplete(LocalDateTime.of(2026, 9, 30, 10, 0));

        List<ReminderResponse> result = reminderService.getSmartReminders("completed");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("먼저 생성", "나중 생성");
    }

    @Test
    @DisplayName("완료된 리마인더를 수정해도 completed 뷰의 순서는 바뀌지 않는다")
    void getSmartReminders_completed_orderIsNotAffectedByUpdate() {
        Reminder older = reminderRepository.save(new Reminder(owner, "먼저 완료", null, null, null));
        Reminder newer = reminderRepository.save(new Reminder(owner, "나중 완료", null, null, null));
        older.toggleComplete(LocalDateTime.of(2026, 9, 30, 9, 0));
        newer.toggleComplete(LocalDateTime.of(2026, 9, 30, 10, 0));
        reminderRepository.flush();

        reminderService.updateReminder(older.getId(), new ReminderUpdateRequest("먼저 완료(수정)", null, null, false, null, null, null));
        reminderRepository.flush();
        List<ReminderResponse> result = reminderService.getSmartReminders("completed");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("나중 완료", "먼저 완료(수정)");
    }

    @Test
    @DisplayName("스마트 뷰 이름은 대소문자를 구분하지 않는다")
    void getSmartReminders_isCaseInsensitive() {
        reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));

        List<ReminderResponse> result = reminderService.getSmartReminders("ALL");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("알 수 없는 스마트 뷰를 요청하면 400 예외가 발생한다")
    void getSmartReminders_throwsBadRequest_whenViewIsUnknown() {
        assertThatThrownBy(() -> reminderService.getSmartReminders("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("존재하는 리마인더를 삭제한다")
    void deleteReminder_deletesReminder_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null));

        reminderService.deleteReminder(saved.getId());

        assertThat(reminderRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 삭제하면 404 예외가 발생한다")
    void deleteReminder_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.deleteReminder(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("리마인더를 생성하면 같은 리스트 안의 마지막 순서가 부여되고, 리스트마다 순서는 따로 매겨진다")
    void createReminder_assignsLastSortOrderWithinSameList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null));

        ReminderResponse milk = create("우유 사기", shopping.getId());
        ReminderResponse eggs = create("계란 사기", shopping.getId());
        ReminderResponse report = create("보고서 작성", work.getId());

        assertThat(milk.sortOrder()).isZero();
        assertThat(eggs.sortOrder()).isEqualTo(1);
        assertThat(report.sortOrder()).isZero();
    }

    @Test
    @DisplayName("순서를 변경하면 리스트별 조회가 바뀐 순서대로 반환된다")
    void reorderReminders_changesListOrder() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        ReminderResponse milk = create("우유", shopping.getId());
        ReminderResponse eggs = create("계란", shopping.getId());
        ReminderResponse bread = create("빵", shopping.getId());

        reminderService.reorderReminders(shopping.getId(), List.of(bread.id(), milk.id(), eggs.id()));

        assertThat(reminderService.getReminders(shopping.getId(), null))
                .extracting(ReminderResponse::title).containsExactly("빵", "우유", "계란");
    }

    @Test
    @DisplayName("순서 변경 ids가 리스트의 미완료 리마인더와 정확히 일치하지 않으면 400 예외가 발생한다")
    void reorderReminders_throwsBadRequest_whenIdsDoNotMatch() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null));
        ReminderResponse milk = create("우유", shopping.getId());
        ReminderResponse eggs = create("계란", shopping.getId());
        ReminderResponse done = create("빵", shopping.getId());
        reminderService.toggleComplete(done.id());
        ReminderResponse report = create("보고서", work.getId());

        List<List<Long>> invalidIds = List.of(
                List.of(milk.id()),
                List.of(milk.id(), milk.id()),
                List.of(milk.id(), eggs.id(), done.id()),
                List.of(milk.id(), report.id()));

        for (List<Long> ids : invalidIds) {
            assertThatThrownBy(() -> reminderService.reorderReminders(shopping.getId(), ids))
                    .isInstanceOf(BusinessException.class)
                    .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
        }
    }

    @Test
    @DisplayName("존재하지 않는 리스트의 순서를 변경하면 404 예외가 발생한다")
    void reorderReminders_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderService.reorderReminders(-1L, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("순서를 변경해도 스마트 뷰는 기존 정렬(마감일 순)을 유지한다")
    void reorderReminders_doesNotAffectSmartViewOrder() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        LocalDate today = LocalDate.now();
        ReminderResponse early = reminderService.createReminder(
                new ReminderRequest("이른 마감", null, shopping.getId(), today.atTime(9, 0), null, null, null, null));
        ReminderResponse late = reminderService.createReminder(
                new ReminderRequest("늦은 마감", null, shopping.getId(), today.atTime(18, 0), null, null, null, null));

        reminderService.reorderReminders(shopping.getId(), List.of(late.id(), early.id()));

        assertThat(reminderService.getSmartReminders("scheduled"))
                .extracting(ReminderResponse::title).containsExactly("이른 마감", "늦은 마감");
    }

    @Test
    @DisplayName("태그 이름으로 리마인더를 생성하면 없는 태그는 새로 만들고 있는 태그는 재사용한다")
    void createReminder_withTagNames_createsMissingTags_andReusesExisting() {
        Tag home = tagRepository.save(new Tag(owner, "집"));

        ReminderResponse result = reminderService.createReminder(
                new ReminderRequest("우유 사기", null, null, null, null, List.of("집", " 심부름 ", "집"), null, null));

        assertThat(result.tags()).containsExactlyInAnyOrder("집", "심부름");
        assertThat(reminderRepository.findById(result.id()).orElseThrow().getTags())
                .extracting(Tag::getId).contains(home.getId());
        assertThat(tagRepository.findByUserIdAndNameIn(owner.getId(), List.of("집"))).hasSize(1);
    }

    @Test
    @DisplayName("리마인더를 수정하면 태그가 요청 목록으로 교체되고, 태그를 생략하면 모두 떨어진다")
    void updateReminder_replacesTags() {
        ReminderResponse saved = reminderService.createReminder(
                new ReminderRequest("우유 사기", null, null, null, null, List.of("집", "심부름"), null, null));

        ReminderResponse replaced = reminderService.updateReminder(saved.id(),
                new ReminderUpdateRequest("우유 사기", null, null, false, null, List.of("급함"), null));
        assertThat(replaced.tags()).containsExactly("급함");

        ReminderResponse cleared = reminderService.updateReminder(saved.id(),
                new ReminderUpdateRequest("우유 사기", null, null, false, null, null, null));
        assertThat(cleared.tags()).isEmpty();
    }

    @Test
    @DisplayName("태그로 조회하면 해당 태그가 붙은 리마인더만 생성순으로 반환한다")
    void getReminders_byTag_returnsOnlyTaggedReminders() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null));
        reminderService.createReminder(new ReminderRequest("우유", null, shopping.getId(), null, null, List.of("집"), null, null));
        reminderService.createReminder(new ReminderRequest("보고서", null, null, null, null, List.of("회사"), null, null));
        reminderService.createReminder(new ReminderRequest("빨래", null, null, null, null, List.of("집", "주말"), null, null));

        List<ReminderResponse> result = reminderService.getReminders(null, "집");

        assertThat(result).extracting(ReminderResponse::title).containsExactly("우유", "빨래");
    }

    @Test
    @DisplayName("태그가 붙은 리마인더도 삭제할 수 있고, 태그는 남는다")
    void deleteReminder_withTags_keepsTags() {
        ReminderResponse saved = reminderService.createReminder(
                new ReminderRequest("우유 사기", null, null, null, null, List.of("집"), null, null));
        entityManager.flush();
        entityManager.clear();

        reminderService.deleteReminder(saved.id());
        entityManager.flush();

        assertThat(reminderRepository.findById(saved.id())).isEmpty();
        assertThat(tagRepository.findByUserIdAndNameIn(owner.getId(), List.of("집"))).hasSize(1);
    }

    @Test
    @DisplayName("parentId로 생성하면 하위 작업이 되고, listId와 관계없이 부모의 리스트에 속하며 부모 안에서 순서가 매겨진다")
    void createReminder_withParentId_createsSubtaskInParentList() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null));
        ReminderResponse parent = create("이사 준비", home.getId());

        ReminderResponse boxes = createSubtask("박스 구하기", parent.id());
        ReminderResponse movers = reminderService.createReminder(
                new ReminderRequest("이삿짐센터 예약", null, work.getId(), null, null, null, parent.id(), null));

        assertThat(boxes.parentId()).isEqualTo(parent.id());
        assertThat(boxes.listId()).isEqualTo(home.getId());
        assertThat(movers.listId()).isEqualTo(home.getId());
        assertThat(boxes.sortOrder()).isZero();
        assertThat(movers.sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 부모로 하위 작업을 생성하면 404 예외가 발생한다")
    void createReminder_throwsNotFound_whenParentDoesNotExist() {
        assertThatThrownBy(() -> createSubtask("박스 구하기", -1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("하위 작업 아래에 하위 작업을 생성하면 400 예외가 발생한다")
    void createReminder_throwsBadRequest_whenParentIsSubtask() {
        ReminderResponse parent = create("이사 준비", null);
        ReminderResponse subtask = createSubtask("박스 구하기", parent.id());

        assertThatThrownBy(() -> createSubtask("테이프 사기", subtask.id()))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("하위 작업을 추가해도 최상위 리마인더의 순서 부여에는 영향을 주지 않는다")
    void createReminder_topLevelSortOrder_ignoresSubtasks() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        ReminderResponse parent = create("이사 준비", home.getId());
        createSubtask("박스 구하기", parent.id());
        createSubtask("이삿짐센터 예약", parent.id());

        ReminderResponse next = create("빨래", home.getId());

        assertThat(next.sortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("리스트별 조회는 최상위 리마인더만 반환하고, 하위 작업은 부모의 subtasks에 순서대로 담긴다")
    void getReminders_byList_returnsTopLevelOnly_withSubtasks() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        ReminderResponse parent = create("이사 준비", home.getId());
        createSubtask("박스 구하기", parent.id());
        createSubtask("이삿짐센터 예약", parent.id());
        create("빨래", home.getId());
        entityManager.flush();
        entityManager.clear();

        List<ReminderResponse> result = reminderService.getReminders(home.getId(), null);

        assertThat(result).extracting(ReminderResponse::title).containsExactly("이사 준비", "빨래");
        assertThat(result.get(0).subtasks())
                .extracting(ReminderResponse::title).containsExactly("박스 구하기", "이삿짐센터 예약");
    }

    @Test
    @DisplayName("스마트 뷰는 하위 작업도 개별 항목으로 포함한다")
    void getSmartReminders_includesSubtasksAsIndividualItems() {
        ReminderResponse parent = create("이사 준비", null);
        createSubtask("박스 구하기", parent.id());

        assertThat(reminderService.getSmartReminders("all"))
                .extracting(ReminderResponse::title).contains("이사 준비", "박스 구하기");
    }

    @Test
    @DisplayName("부모를 완료하면 하위 작업도 모두 완료되어 completed 뷰에 나타난다")
    void toggleComplete_parent_completesSubtasks() {
        ReminderResponse parent = create("이사 준비", null);
        ReminderResponse boxes = createSubtask("박스 구하기", parent.id());
        ReminderResponse movers = createSubtask("이삿짐센터 예약", parent.id());
        entityManager.flush();
        entityManager.clear();

        reminderService.toggleComplete(parent.id());
        entityManager.flush();
        entityManager.clear();

        assertThat(reminderRepository.findById(boxes.id())).get().extracting(Reminder::isCompleted).isEqualTo(true);
        assertThat(reminderRepository.findById(movers.id())).get().extracting(Reminder::isCompleted).isEqualTo(true);
        assertThat(reminderService.getSmartReminders("completed"))
                .extracting(ReminderResponse::title).contains("이사 준비", "박스 구하기", "이삿짐센터 예약");
    }

    @Test
    @DisplayName("부모를 삭제하면 하위 작업도 함께 삭제된다")
    void deleteReminder_parent_deletesSubtasks() {
        ReminderResponse parent = create("이사 준비", null);
        ReminderResponse boxes = createSubtask("박스 구하기", parent.id());
        entityManager.flush();
        entityManager.clear();

        reminderService.deleteReminder(parent.id());
        entityManager.flush();

        assertThat(reminderRepository.findById(parent.id())).isEmpty();
        assertThat(reminderRepository.findById(boxes.id())).isEmpty();
    }

    @Test
    @DisplayName("하위 작업을 삭제하면 그 하위 작업만 삭제되고 부모와 다른 하위 작업은 유지된다")
    void deleteReminder_subtask_keepsParentAndSiblings() {
        ReminderResponse parent = create("이사 준비", null);
        ReminderResponse boxes = createSubtask("박스 구하기", parent.id());
        ReminderResponse movers = createSubtask("이삿짐센터 예약", parent.id());
        entityManager.flush();
        entityManager.clear();

        reminderService.deleteReminder(boxes.id());
        entityManager.flush();
        entityManager.clear();

        assertThat(reminderRepository.findById(boxes.id())).isEmpty();
        assertThat(reminderRepository.findById(parent.id())).get()
                .extracting(reminder -> reminder.getSubtasks().stream().map(Reminder::getId).toList())
                .isEqualTo(List.of(movers.id()));
    }

    @Test
    @DisplayName("순서 변경 대상은 최상위 미완료 리마인더뿐이라 하위 작업 id를 넣으면 400 예외가 발생한다")
    void reorderReminders_targetsTopLevelOnly() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        ReminderResponse parent = create("이사 준비", home.getId());
        ReminderResponse laundry = create("빨래", home.getId());
        ReminderResponse boxes = createSubtask("박스 구하기", parent.id());

        reminderService.reorderReminders(home.getId(), List.of(laundry.id(), parent.id()));

        assertThat(reminderService.getReminders(home.getId(), null))
                .extracting(ReminderResponse::title).containsExactly("빨래", "이사 준비");
        assertThatThrownBy(() -> reminderService.reorderReminders(home.getId(),
                List.of(laundry.id(), parent.id(), boxes.id())))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("반복을 지정해 생성하면 반복 주기가 저장되고, 생략하면 '반복 안 함'이다")
    void createReminder_savesRepeatRule() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 4, 20, 0);

        ReminderResponse weekly = reminderService.createReminder(
                new ReminderRequest("분리수거", null, null, dueAt, null, null, null, RepeatRule.WEEKLY));
        ReminderResponse once = create("우유 사기", null);

        assertThat(weekly.repeatRule()).isEqualTo(RepeatRule.WEEKLY);
        assertThat(once.repeatRule()).isEqualTo(RepeatRule.NONE);
    }

    @Test
    @DisplayName("마감일시 없이 반복을 지정해 생성하면 400 예외가 발생한다")
    void createReminder_throwsBadRequest_whenRepeatingWithoutDueAt() {
        assertThatThrownBy(() -> reminderService.createReminder(
                new ReminderRequest("분리수거", null, null, null, null, null, null, RepeatRule.DAILY)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("반복 리마인더의 마감일시를 지우면서 반복을 유지하도록 수정하면 400 예외가 발생한다")
    void updateReminder_throwsBadRequest_whenRepeatingWithoutDueAt() {
        ReminderResponse saved = createRepeating("분리수거", null, LocalDateTime.of(2026, 10, 4, 20, 0), RepeatRule.WEEKLY);

        assertThatThrownBy(() -> reminderService.updateReminder(saved.id(),
                new ReminderUpdateRequest("분리수거", null, null, false, null, null, RepeatRule.WEEKLY)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("수정으로 반복 주기를 바꾸거나 해제할 수 있다")
    void updateReminder_changesRepeatRule() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 4, 20, 0);
        ReminderResponse saved = createRepeating("분리수거", null, dueAt, RepeatRule.WEEKLY);

        ReminderResponse monthly = reminderService.updateReminder(saved.id(),
                new ReminderUpdateRequest("분리수거", null, dueAt, false, null, null, RepeatRule.MONTHLY));
        assertThat(monthly.repeatRule()).isEqualTo(RepeatRule.MONTHLY);

        ReminderResponse once = reminderService.updateReminder(saved.id(),
                new ReminderUpdateRequest("분리수거", null, null, false, null, null, null));
        assertThat(once.repeatRule()).isEqualTo(RepeatRule.NONE);
    }

    @Test
    @DisplayName("매주 반복 리마인더를 완료하면 현재 항목은 완료되고 7일 뒤 마감의 새 항목이 같은 리스트 마지막 순서로 저장된다")
    void toggleComplete_whenWeekly_savesNextOccurrenceSevenDaysLater() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        create("빨래", home.getId());
        ReminderResponse trash = reminderService.createReminder(new ReminderRequest("분리수거", "캔/페트", home.getId(),
                LocalDateTime.of(2026, 10, 4, 20, 0), Priority.HIGH, List.of("집안일"), null, RepeatRule.WEEKLY));

        reminderService.toggleComplete(trash.id());
        entityManager.flush();
        entityManager.clear();

        List<ReminderResponse> reminders = reminderService.getReminders(home.getId(), null);
        assertThat(reminders).extracting(ReminderResponse::title).containsExactly("빨래", "분리수거", "분리수거");
        ReminderResponse completed = reminders.get(1);
        ReminderResponse next = reminders.get(2);
        assertThat(completed.completed()).isTrue();
        assertThat(next.completed()).isFalse();
        assertThat(next.id()).isNotEqualTo(trash.id());
        assertThat(next.dueAt()).isEqualTo(LocalDateTime.of(2026, 10, 11, 20, 0));
        assertThat(next.memo()).isEqualTo("캔/페트");
        assertThat(next.priority()).isEqualTo(Priority.HIGH);
        assertThat(next.repeatRule()).isEqualTo(RepeatRule.WEEKLY);
        assertThat(next.tags()).containsExactly("집안일");
        assertThat(next.sortOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("반복 리마인더 완료 후 다음 회차는 '예정됨'에, 현재 항목은 '완료됨'에 나타난다")
    void toggleComplete_whenRepeating_nextOccurrenceAppearsInScheduled() {
        ReminderResponse trash = createRepeating("분리수거", null, LocalDateTime.of(2026, 10, 4, 20, 0), RepeatRule.WEEKLY);

        reminderService.toggleComplete(trash.id());
        entityManager.flush();

        assertThat(reminderService.getSmartReminders("completed")).extracting(ReminderResponse::id).contains(trash.id());
        assertThat(reminderService.getSmartReminders("scheduled"))
                .filteredOn(reminder -> reminder.title().equals("분리수거"))
                .extracting(ReminderResponse::dueAt)
                .containsExactly(LocalDateTime.of(2026, 10, 11, 20, 0));
    }

    @Test
    @DisplayName("반복 리마인더의 완료를 취소해도 이미 만든 다음 회차는 유지되고, 다시 완료해도 중복으로 만들지 않는다")
    void toggleComplete_whenUndone_keepsNextOccurrence_andDoesNotDuplicate() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        ReminderResponse trash = createRepeating("분리수거", home.getId(), LocalDateTime.of(2026, 10, 4, 20, 0), RepeatRule.DAILY);

        reminderService.toggleComplete(trash.id());
        ReminderResponse undone = reminderService.toggleComplete(trash.id());
        assertThat(undone.completed()).isFalse();
        assertThat(reminderService.getReminders(home.getId(), null)).hasSize(2);

        reminderService.toggleComplete(trash.id());
        assertThat(reminderService.getReminders(home.getId(), null)).hasSize(2);
    }

    @Test
    @DisplayName("반복하는 하위 작업을 완료하면 다음 회차가 같은 부모의 하위 작업으로 저장된다")
    void toggleComplete_whenRepeatingSubtask_savesNextOccurrenceUnderSameParent() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null));
        ReminderResponse parent = create("운동", home.getId());
        ReminderResponse stretching = reminderService.createReminder(new ReminderRequest("스트레칭", null, null,
                LocalDateTime.of(2026, 10, 4, 7, 0), null, null, parent.id(), RepeatRule.DAILY));

        reminderService.toggleComplete(stretching.id());
        entityManager.flush();
        entityManager.clear();

        List<ReminderResponse> reminders = reminderService.getReminders(home.getId(), null);
        assertThat(reminders).extracting(ReminderResponse::title).containsExactly("운동");
        assertThat(reminders.getFirst().subtasks())
                .extracting(ReminderResponse::dueAt)
                .containsExactly(LocalDateTime.of(2026, 10, 4, 7, 0), LocalDateTime.of(2026, 10, 5, 7, 0));
    }

    @Test
    @DisplayName("다가오는 리마인더 조회는 from 이상 to 미만에 마감되는 항목을 마감일시 순으로 반환한다")
    void getUpcomingReminders_includesFromAndExcludesTo_sortedByDueAt() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 4, 21, 0);
        LocalDateTime to = from.plusMinutes(2);
        reminderRepository.save(new Reminder(owner, "직전", null, null, from.minusSeconds(1)));
        reminderRepository.save(new Reminder(owner, "마지막", null, null, to.minusSeconds(1)));
        reminderRepository.save(new Reminder(owner, "시작", null, null, from));
        reminderRepository.save(new Reminder(owner, "끝", null, null, to));
        reminderRepository.save(new Reminder(owner, "마감일 없음", null, null, null));

        List<ReminderResponse> result = reminderService.getUpcomingReminders(from, to);

        assertThat(result).extracting(ReminderResponse::title).containsExactly("시작", "마지막");
    }

    @Test
    @DisplayName("다가오는 리마인더 조회는 완료된 항목을 제외한다")
    void getUpcomingReminders_excludesCompleted() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 4, 21, 0);
        Reminder done = reminderRepository.save(new Reminder(owner, "완료함", null, null, from.plusMinutes(1)));
        done.toggleComplete(LocalDateTime.of(2026, 10, 4, 20, 0));
        reminderRepository.save(new Reminder(owner, "남음", null, null, from.plusMinutes(1)));

        assertThat(reminderService.getUpcomingReminders(from, from.plusMinutes(2)))
                .extracting(ReminderResponse::title).containsExactly("남음");
    }

    @Test
    @DisplayName("다가오는 리마인더 조회는 하위 작업도 개별 항목으로 포함한다")
    void getUpcomingReminders_includesSubtasks() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 4, 21, 0);
        ReminderResponse parent = create("이사 준비", null);
        reminderService.createReminder(new ReminderRequest(
                "박스 구하기", null, null, from.plusMinutes(1), null, null, parent.id(), null));

        assertThat(reminderService.getUpcomingReminders(from, from.plusMinutes(2)))
                .extracting(ReminderResponse::title).containsExactly("박스 구하기");
    }

    @Test
    @DisplayName("from이 to보다 늦거나 같으면 400 예외가 발생한다")
    void getUpcomingReminders_throwsBadRequest_whenFromIsNotBeforeTo() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 4, 21, 0);

        assertThatThrownBy(() -> reminderService.getUpcomingReminders(time, time))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
        assertThatThrownBy(() -> reminderService.getUpcomingReminders(time.plusMinutes(1), time))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    // 저장 직후 수정하면 시계 해상도에 따라 수정일이 같을 수 있으므로, 수정일을 과거로 돌려 두고 갱신 여부를 확인한다.
    private LocalDateTime backdateUpdatedAt(Long reminderId) {
        LocalDateTime past = LocalDateTime.of(2026, 1, 1, 0, 0);
        entityManager.createQuery("update Reminder r set r.updatedAt = :past where r.id = :id")
                .setParameter("past", past)
                .setParameter("id", reminderId)
                .executeUpdate();
        entityManager.clear();
        return past;
    }

    private ReminderResponse createRepeating(String title, Long listId, LocalDateTime dueAt, RepeatRule repeatRule) {
        return reminderService.createReminder(
                new ReminderRequest(title, null, listId, dueAt, null, null, null, repeatRule));
    }

    private ReminderResponse create(String title, Long listId) {
        return reminderService.createReminder(new ReminderRequest(title, null, listId, null, null, null, null, null));
    }

    private ReminderResponse createSubtask(String title, Long parentId) {
        return reminderService.createReminder(new ReminderRequest(title, null, null, null, null, null, parentId, null));
    }
}

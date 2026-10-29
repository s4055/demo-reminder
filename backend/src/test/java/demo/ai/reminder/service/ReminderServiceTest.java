package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.DomainException;
import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.domain.Tag;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.response.DeletedCountResponse;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.repository.TagRepository;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.service.command.CreateReminderCommand;
import demo.ai.reminder.service.command.UpdateReminderCommand;
import demo.ai.reminder.support.TestAuth;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReminderServiceTest {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private ReminderQueryService reminderQueryService;

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
    @DisplayName("listId를 지정해 리마인더를 생성하면 해당 리스트에 소속된다")
    void createReminder_belongsToGivenList() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));

        ReminderResponse result = reminderService.createReminder(new CreateReminderCommand("우유 사기", null, shopping.getId(), null, null, null, null, null));

        assertThat(result.listId()).isEqualTo(shopping.getId());
    }

    @Test
    @DisplayName("존재하지 않는 리스트로 리마인더를 생성하면 404 예외가 발생한다")
    void createReminder_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderService.createReminder(new CreateReminderCommand("우유 사기", null, -1L, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("요청으로 받은 제목과 메모로 리마인더를 생성한다")
    void createReminder_savesReminderWithGivenTitleAndMemo() {
        CreateReminderCommand command = new CreateReminderCommand("우유 사기", "저지방", null, null, null, null, null, null);

        ReminderResponse result = reminderService.createReminder(command);

        assertThat(result.id()).isNotNull();
        assertThat(result.title()).isEqualTo("우유 사기");
        assertThat(result.memo()).isEqualTo("저지방");
        assertThat(result.completed()).isFalse();
        assertThat(reminderRepository.findById(result.id())).isPresent();
    }

    @Test
    @DisplayName("존재하는 리마인더의 완료 상태를 토글한다")
    void toggleComplete_flipsCompletedState_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));

        ReminderResponse result = reminderService.toggleComplete(saved.getId());

        assertThat(result.completed()).isTrue();
    }

    @Test
    @DisplayName("완료 토글 시 완료일시가 기록되고, 다시 토글하면 비워진다")
    void toggleComplete_recordsAndClearsCompletedAt() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));
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

        ReminderResponse result = reminderService.createReminder(new CreateReminderCommand("우유 사기", null, null, dueAt, null, null, null, null));

        assertThat(result.dueAt()).isEqualTo(dueAt);
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("리마인더를 생성하면 JPA Auditing이 생성일과 수정일을 채운다")
    void createReminder_fillsCreatedAtAndUpdatedAt() {
        LocalDateTime before = LocalDateTime.now();

        ReminderResponse result = reminderService.createReminder(new CreateReminderCommand("우유 사기", null, null, null, null, null, null, null));

        assertThat(result.createdAt()).isNotNull().isAfterOrEqualTo(before);
        assertThat(result.updatedAt()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("리마인더를 수정하면 수정일은 갱신되고 생성일은 유지된다")
    void updateReminder_refreshesUpdatedAt_andKeepsCreatedAt() {
        Reminder saved = reminderRepository.saveAndFlush(new Reminder(owner, "우유 사기", null, null, null, null, null));
        LocalDateTime updatedAt = backdateUpdatedAt(saved.getId());
        LocalDateTime createdAt = reminderRepository.findById(saved.getId()).orElseThrow().getCreatedAt();

        reminderService.updateReminder(saved.getId(), new UpdateReminderCommand("계란 사기", null, null, false, null, null, null, 0L));

        Reminder result = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    @DisplayName("수정/완료/플래그 토글의 응답에는 갱신된 수정일이 담긴다")
    void mutatingMethods_returnRefreshedUpdatedAt() {
        Long toUpdate = reminderRepository.saveAndFlush(new Reminder(owner, "우유 사기", null, null, null, null, null)).getId();
        Long toComplete = reminderRepository.saveAndFlush(new Reminder(owner, "계란 사기", null, null, null, null, null)).getId();
        Long toFlag = reminderRepository.saveAndFlush(new Reminder(owner, "빵 사기", null, null, null, null, null)).getId();
        LocalDateTime past = backdateUpdatedAt(toUpdate);
        backdateUpdatedAt(toComplete);
        backdateUpdatedAt(toFlag);

        ReminderResponse updated = reminderService.updateReminder(
                toUpdate, new UpdateReminderCommand("우유 사기(수정)", null, null, false, null, null, null, 0L));
        ReminderResponse completed = reminderService.toggleComplete(toComplete);
        ReminderResponse flagged = reminderService.toggleFlag(toFlag);

        assertThat(updated.updatedAt()).isAfter(past);
        assertThat(completed.updatedAt()).isAfter(past);
        assertThat(flagged.updatedAt()).isAfter(past);
    }

    @Test
    @DisplayName("존재하는 리마인더의 제목, 메모, 마감일시, 플래그를 수정한다")
    void updateReminder_changesEditableFields_whenReminderExists() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, shopping, null, null, null));
        LocalDateTime dueAt = LocalDate.now().atTime(18, 30);

        ReminderResponse result = reminderService.updateReminder(
                saved.getId(), new UpdateReminderCommand("계란 사기", "12구", dueAt, true, null, null, null, 0L));

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
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));

        reminderService.updateReminder(
                saved.getId(), new UpdateReminderCommand("우유 사기", null, LocalDate.now().atTime(9, 0), true, null, null, null, 0L));

        assertThat(reminderQueryService.getSmartReminders(SmartView.TODAY)).extracting(ReminderResponse::title).containsExactly("우유 사기");
        assertThat(reminderQueryService.getSmartReminders(SmartView.FLAGGED)).extracting(ReminderResponse::title).containsExactly("우유 사기");
    }

    @Test
    @DisplayName("우선순위를 지정해 리마인더를 생성하면 우선순위가 저장된다")
    void createReminder_savesGivenPriority() {
        ReminderResponse result = reminderService.createReminder(new CreateReminderCommand("우유 사기", null, null, null, Priority.HIGH, null, null, null));
        reminderRepository.flush();

        assertThat(reminderRepository.findById(result.id()).orElseThrow().getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("우선순위를 생략하고 리마인더를 생성하면 없음(NONE)으로 저장된다")
    void createReminder_savesNonePriority_whenPriorityIsOmitted() {
        ReminderResponse result = reminderService.createReminder(new CreateReminderCommand("우유 사기", null, null, null, null, null, null, null));

        assertThat(result.priority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("리마인더를 수정하면 우선순위가 변경되어 저장된다")
    void updateReminder_savesChangedPriority() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, Priority.LOW, null));

        reminderService.updateReminder(saved.getId(), new UpdateReminderCommand("우유 사기", null, null, false, Priority.MEDIUM, null, null, 0L));
        reminderRepository.flush();

        assertThat(reminderRepository.findById(saved.getId()).orElseThrow().getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("수정 요청에서 우선순위를 생략하면 없음(NONE)으로 바뀐다")
    void updateReminder_resetsPriorityToNone_whenPriorityIsOmitted() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, Priority.HIGH, null));

        ReminderResponse result = reminderService.updateReminder(saved.getId(), new UpdateReminderCommand("우유 사기", null, null, false, null, null, null, 0L));

        assertThat(result.priority()).isEqualTo(Priority.NONE);
    }

    @Test
    @DisplayName("존재하지 않는 리마인더를 수정하면 404 예외가 발생한다")
    void updateReminder_throwsNotFound_whenReminderDoesNotExist() {
        assertThatThrownBy(() -> reminderService.updateReminder(-1L, new UpdateReminderCommand("우유 사기", null, null, false, null, null, null, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("존재하는 리마인더의 플래그 상태를 토글한다")
    void toggleFlag_flipsFlaggedState_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));

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
    @DisplayName("존재하는 리마인더를 삭제한다")
    void deleteReminder_deletesReminder_whenReminderExists() {
        Reminder saved = reminderRepository.save(new Reminder(owner, "우유 사기", null, null, null, null, null));

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
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null, null, 0));

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
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        ReminderResponse milk = create("우유", shopping.getId());
        ReminderResponse eggs = create("계란", shopping.getId());
        ReminderResponse bread = create("빵", shopping.getId());

        reminderService.reorderReminders(shopping.getId(), List.of(bread.id(), milk.id(), eggs.id()));

        assertThat(reminderQueryService.getReminders(shopping.getId(), null))
                .extracting(ReminderResponse::title).containsExactly("빵", "우유", "계란");
    }

    @Test
    @DisplayName("순서 변경 ids가 리스트의 미완료 리마인더와 정확히 일치하지 않으면 400 예외가 발생한다")
    void reorderReminders_throwsBadRequest_whenIdsDoNotMatch() {
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null, null, 0));
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
        ReminderList shopping = reminderListRepository.save(new ReminderList(owner, "장보기", null, null, 0));
        LocalDate today = LocalDate.now();
        ReminderResponse early = reminderService.createReminder(
                new CreateReminderCommand("이른 마감", null, shopping.getId(), today.atTime(9, 0), null, null, null, null));
        ReminderResponse late = reminderService.createReminder(
                new CreateReminderCommand("늦은 마감", null, shopping.getId(), today.atTime(18, 0), null, null, null, null));

        reminderService.reorderReminders(shopping.getId(), List.of(late.id(), early.id()));

        assertThat(reminderQueryService.getSmartReminders(SmartView.SCHEDULED))
                .extracting(ReminderResponse::title).containsExactly("이른 마감", "늦은 마감");
    }

    @Test
    @DisplayName("태그 이름으로 리마인더를 생성하면 없는 태그는 새로 만들고 있는 태그는 재사용한다")
    void createReminder_withTagNames_createsMissingTags_andReusesExisting() {
        Tag home = tagRepository.save(new Tag(owner, "집"));

        ReminderResponse result = reminderService.createReminder(
                new CreateReminderCommand("우유 사기", null, null, null, null, List.of("집", "심부름", "집"), null, null));

        assertThat(result.tags()).containsExactlyInAnyOrder("집", "심부름");
        assertThat(reminderRepository.findById(result.id()).orElseThrow().getTags())
                .extracting(Tag::getId).contains(home.getId());
        assertThat(tagRepository.findByUserIdAndNameIn(owner.getId(), List.of("집"))).hasSize(1);
    }

    @Test
    @DisplayName("리마인더를 수정하면 태그가 요청 목록으로 교체되고, 태그를 생략하면 모두 떨어진다")
    void updateReminder_replacesTags() {
        ReminderResponse saved = reminderService.createReminder(
                new CreateReminderCommand("우유 사기", null, null, null, null, List.of("집", "심부름"), null, null));

        ReminderResponse replaced = reminderService.updateReminder(saved.id(),
                new UpdateReminderCommand("우유 사기", null, null, false, null, List.of("급함"), null, 0L));
        assertThat(replaced.tags()).containsExactly("급함");

        ReminderResponse cleared = reminderService.updateReminder(saved.id(),
                new UpdateReminderCommand("우유 사기", null, null, false, null, null, null, replaced.version()));
        assertThat(cleared.tags()).isEmpty();
    }

    @Test
    @DisplayName("이미 붙은 태그를 유지한 채 태그를 바꿔 저장해도 유니크 제약에 걸리지 않고, 유지한 태그의 연결 행은 그대로 남는다")
    void updateReminder_keepingSomeTags_keepsExistingReminderTagRows() {
        ReminderResponse saved = reminderService.createReminder(
                new CreateReminderCommand("우유 사기", null, null, null, null, List.of("집", "심부름"), null, null));
        entityManager.flush();
        entityManager.clear();
        Long homeRowId = reminderTagRowId(saved.id(), "집");

        reminderService.updateReminder(saved.id(),
                new UpdateReminderCommand("우유 사기", null, null, false, null, List.of("급함", "집"), null, saved.version()));
        entityManager.flush();
        entityManager.clear();

        assertThat(reminderRepository.findById(saved.id()).orElseThrow().getTags())
                .extracting(Tag::getName).containsExactlyInAnyOrder("집", "급함");
        assertThat(reminderTagRowId(saved.id(), "집")).isEqualTo(homeRowId);
        assertThat(countRows("ReminderTag", saved.id())).isEqualTo(2L);
    }

    @Test
    @DisplayName("태그가 붙은 리마인더도 삭제할 수 있고, 태그는 남는다")
    void deleteReminder_withTags_keepsTags() {
        ReminderResponse saved = reminderService.createReminder(
                new CreateReminderCommand("우유 사기", null, null, null, null, List.of("집"), null, null));
        entityManager.flush();
        entityManager.clear();

        reminderService.deleteReminder(saved.id());
        entityManager.flush();

        assertThat(reminderRepository.findById(saved.id())).isEmpty();
        assertThat(tagRepository.findByUserIdAndNameIn(owner.getId(), List.of("집"))).hasSize(1);
    }

    @Test
    @DisplayName("리마인더를 삭제하면 태그 연결 행과 반복 요일 행도 함께 삭제된다")
    void deleteReminder_deletesReminderTagAndRepeatDayRows() {
        ReminderResponse saved = reminderService.createReminder(new CreateReminderCommand("운동", null, null,
                LocalDateTime.of(2026, 10, 5, 7, 0), null, List.of("건강"), null,
                new Recurrence(RepeatRule.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))));
        entityManager.flush();
        entityManager.clear();
        assertThat(countRows("ReminderTag", saved.id())).isEqualTo(1L);
        assertThat(countRows("ReminderRepeatDay", saved.id())).isEqualTo(2L);

        reminderService.deleteReminder(saved.id());
        entityManager.flush();

        assertThat(countRows("ReminderTag", saved.id())).isZero();
        assertThat(countRows("ReminderRepeatDay", saved.id())).isZero();
    }

    @Test
    @DisplayName("parentId로 생성하면 하위 작업이 되고, listId와 관계없이 부모의 리스트에 속하며 부모 안에서 순서가 매겨진다")
    void createReminder_withParentId_createsSubtaskInParentList() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null, null, 0));
        ReminderResponse parent = create("이사 준비", home.getId());

        ReminderResponse boxes = createSubtask("박스 구하기", parent.id());
        ReminderResponse movers = reminderService.createReminder(
                new CreateReminderCommand("이삿짐센터 예약", null, work.getId(), null, null, null, parent.id(), null));

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
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("하위 작업을 추가해도 최상위 리마인더의 순서 부여에는 영향을 주지 않는다")
    void createReminder_topLevelSortOrder_ignoresSubtasks() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse parent = create("이사 준비", home.getId());
        createSubtask("박스 구하기", parent.id());
        createSubtask("이삿짐센터 예약", parent.id());

        ReminderResponse next = create("빨래", home.getId());

        assertThat(next.sortOrder()).isEqualTo(1);
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
        assertThat(reminderQueryService.getSmartReminders(SmartView.COMPLETED))
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
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse parent = create("이사 준비", home.getId());
        ReminderResponse laundry = create("빨래", home.getId());
        ReminderResponse boxes = createSubtask("박스 구하기", parent.id());

        reminderService.reorderReminders(home.getId(), List.of(laundry.id(), parent.id()));

        assertThat(reminderQueryService.getReminders(home.getId(), null))
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
                new CreateReminderCommand("분리수거", null, null, dueAt, null, null, null, new Recurrence(RepeatRule.WEEKLY, 1, null)));
        ReminderResponse once = create("우유 사기", null);

        assertThat(weekly.repeatRule()).isEqualTo(RepeatRule.WEEKLY);
        assertThat(once.repeatRule()).isEqualTo(RepeatRule.NONE);
    }

    @Test
    @DisplayName("마감일시 없이 반복을 지정해 생성하면 400 예외가 발생한다")
    void createReminder_throwsBadRequest_whenRepeatingWithoutDueAt() {
        assertThatThrownBy(() -> reminderService.createReminder(
                new CreateReminderCommand("분리수거", null, null, null, null, null, null, new Recurrence(RepeatRule.DAILY, 1, null))))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("반복 리마인더의 마감일시를 지우면서 반복을 유지하도록 수정하면 400 예외가 발생한다")
    void updateReminder_throwsBadRequest_whenRepeatingWithoutDueAt() {
        ReminderResponse saved = createRepeating("분리수거", null, LocalDateTime.of(2026, 10, 4, 20, 0), RepeatRule.WEEKLY);

        assertThatThrownBy(() -> reminderService.updateReminder(saved.id(),
                new UpdateReminderCommand("분리수거", null, null, false, null, null, new Recurrence(RepeatRule.WEEKLY, 1, null), 0L)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("수정으로 반복 주기를 바꾸거나 해제할 수 있다")
    void updateReminder_changesRepeatRule() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 4, 20, 0);
        ReminderResponse saved = createRepeating("분리수거", null, dueAt, RepeatRule.WEEKLY);

        ReminderResponse monthly = reminderService.updateReminder(saved.id(),
                new UpdateReminderCommand("분리수거", null, dueAt, false, null, null, new Recurrence(RepeatRule.MONTHLY, 1, null), 0L));
        assertThat(monthly.repeatRule()).isEqualTo(RepeatRule.MONTHLY);

        ReminderResponse once = reminderService.updateReminder(saved.id(),
                new UpdateReminderCommand("분리수거", null, null, false, null, null, null, monthly.version()));
        assertThat(once.repeatRule()).isEqualTo(RepeatRule.NONE);
    }

    @Test
    @DisplayName("간격과 요일을 지정해 생성하면 저장되고, 생략하면 간격 1·요일 없음이다")
    void createReminder_savesRepeatIntervalAndDaysOfWeek() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 5, 7, 0);

        ReminderResponse custom = createCustomRepeating("운동", dueAt, RepeatRule.WEEKLY, 2,
                Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY));
        ReminderResponse weekly = createRepeating("분리수거", null, dueAt, RepeatRule.WEEKLY);
        entityManager.flush();
        entityManager.clear();

        ReminderResponse reloaded = reminderQueryService.getReminders(null, null).stream()
                .filter(reminder -> reminder.id().equals(custom.id())).findFirst().orElseThrow();
        assertThat(reloaded.repeatInterval()).isEqualTo(2);
        assertThat(reloaded.repeatDaysOfWeek()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        assertThat(weekly.repeatInterval()).isEqualTo(1);
        assertThat(weekly.repeatDaysOfWeek()).isEmpty();
    }

    @Test
    @DisplayName("수정으로 간격과 요일을 바꿀 수 있고, 반복을 해제하면 간격 1·요일 없음이 된다")
    void updateReminder_changesRepeatIntervalAndDaysOfWeek() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 5, 7, 0);
        ReminderResponse saved = createRepeating("운동", null, dueAt, RepeatRule.WEEKLY);

        ReminderResponse custom = reminderService.updateReminder(saved.id(), new UpdateReminderCommand("운동", null, dueAt, false, null, null, new Recurrence(RepeatRule.WEEKLY, 3, Set.of(DayOfWeek.FRIDAY)), 0L));
        assertThat(custom.repeatInterval()).isEqualTo(3);
        assertThat(custom.repeatDaysOfWeek()).containsExactly(DayOfWeek.FRIDAY);

        ReminderResponse once = reminderService.updateReminder(saved.id(),
                new UpdateReminderCommand("운동", null, dueAt, false, null, null, null, custom.version()));
        assertThat(once.repeatRule()).isEqualTo(RepeatRule.NONE);
        assertThat(once.repeatInterval()).isEqualTo(1);
        assertThat(once.repeatDaysOfWeek()).isEmpty();
    }

    @Test
    @DisplayName("요일 일부를 유지한 채 반복 요일을 바꿔 저장해도 유니크 제약에 걸리지 않고, 바뀐 요일만 행이 지워지고 추가된다")
    void updateReminder_keepingSomeDaysOfWeek_replacesOnlyChangedRows() {
        LocalDateTime dueAt = LocalDateTime.of(2026, 10, 5, 7, 0);
        ReminderResponse saved = createCustomRepeating("운동", dueAt, RepeatRule.WEEKLY, 1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        entityManager.flush();
        entityManager.clear();
        Long wednesdayRowId = repeatDayRowId(saved.id(), DayOfWeek.WEDNESDAY);

        ReminderResponse updated = reminderService.updateReminder(saved.id(), new UpdateReminderCommand("운동", null, dueAt,
                false, null, null, new Recurrence(RepeatRule.WEEKLY, 1, Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)), saved.version()));
        entityManager.flush();
        entityManager.clear();

        assertThat(updated.repeatDaysOfWeek()).containsExactly(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        assertThat(repeatDayRowId(saved.id(), DayOfWeek.WEDNESDAY)).isEqualTo(wednesdayRowId);
        assertThat(countRows("ReminderRepeatDay", saved.id())).isEqualTo(2L);
    }

    @Test
    @DisplayName("2주마다 월·수 리마인더는 월요일 회차를 완료하면 같은 주 수요일, 그 수요일을 완료하면 2주 뒤 월요일 회차가 '예정됨'에 생기고 규칙이 복사된다")
    void toggleComplete_whenEveryTwoWeeksOnMondayAndWednesday_createsNextOccurrencesByRule() {
        ReminderResponse monday = createCustomRepeating("운동", LocalDateTime.of(2026, 10, 5, 7, 0),
                RepeatRule.WEEKLY, 2, Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

        reminderService.toggleComplete(monday.id());
        entityManager.flush();
        entityManager.clear();
        ReminderResponse wednesday = scheduledByTitle("운동").getFirst();
        assertThat(wednesday.dueAt()).isEqualTo(LocalDateTime.of(2026, 10, 7, 7, 0));
        assertThat(wednesday.repeatInterval()).isEqualTo(2);
        assertThat(wednesday.repeatDaysOfWeek()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        reminderService.toggleComplete(wednesday.id());
        entityManager.flush();
        entityManager.clear();
        assertThat(scheduledByTitle("운동")).extracting(ReminderResponse::dueAt)
                .containsExactly(LocalDateTime.of(2026, 10, 19, 7, 0));
    }

    @Test
    @DisplayName("매주 반복 리마인더를 완료하면 현재 항목은 완료되고 7일 뒤 마감의 새 항목이 같은 리스트 마지막 순서로 저장된다")
    void toggleComplete_whenWeekly_savesNextOccurrenceSevenDaysLater() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        create("빨래", home.getId());
        ReminderResponse trash = reminderService.createReminder(new CreateReminderCommand("분리수거", "캔/페트", home.getId(), LocalDateTime.of(2026, 10, 4, 20, 0), Priority.HIGH, List.of("집안일"), null, new Recurrence(RepeatRule.WEEKLY, 1, null)));

        reminderService.toggleComplete(trash.id());
        entityManager.flush();
        entityManager.clear();

        List<ReminderResponse> reminders = reminderQueryService.getReminders(home.getId(), null);
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

        assertThat(reminderQueryService.getSmartReminders(SmartView.COMPLETED)).extracting(ReminderResponse::id).contains(trash.id());
        assertThat(reminderQueryService.getSmartReminders(SmartView.SCHEDULED))
                .filteredOn(reminder -> reminder.title().equals("분리수거"))
                .extracting(ReminderResponse::dueAt)
                .containsExactly(LocalDateTime.of(2026, 10, 11, 20, 0));
    }

    @Test
    @DisplayName("반복 리마인더의 완료를 취소해도 이미 만든 다음 회차는 유지되고, 다시 완료해도 중복으로 만들지 않는다")
    void toggleComplete_whenUndone_keepsNextOccurrence_andDoesNotDuplicate() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse trash = createRepeating("분리수거", home.getId(), LocalDateTime.of(2026, 10, 4, 20, 0), RepeatRule.DAILY);

        reminderService.toggleComplete(trash.id());
        ReminderResponse undone = reminderService.toggleComplete(trash.id());
        assertThat(undone.completed()).isFalse();
        assertThat(reminderQueryService.getReminders(home.getId(), null)).hasSize(2);

        reminderService.toggleComplete(trash.id());
        assertThat(reminderQueryService.getReminders(home.getId(), null)).hasSize(2);
    }

    @Test
    @DisplayName("반복하는 하위 작업을 완료하면 다음 회차가 같은 부모의 하위 작업으로 저장된다")
    void toggleComplete_whenRepeatingSubtask_savesNextOccurrenceUnderSameParent() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse parent = create("운동", home.getId());
        ReminderResponse stretching = reminderService.createReminder(new CreateReminderCommand("스트레칭", null, null, LocalDateTime.of(2026, 10, 4, 7, 0), null, null, parent.id(), new Recurrence(RepeatRule.DAILY, 1, null)));

        reminderService.toggleComplete(stretching.id());
        entityManager.flush();
        entityManager.clear();

        List<ReminderResponse> reminders = reminderQueryService.getReminders(home.getId(), null);
        assertThat(reminders).extracting(ReminderResponse::title).containsExactly("운동");
        assertThat(reminders.getFirst().subtasks())
                .extracting(ReminderResponse::dueAt)
                .containsExactly(LocalDateTime.of(2026, 10, 4, 7, 0), LocalDateTime.of(2026, 10, 5, 7, 0));
    }

    @Test
    @DisplayName("완료 항목 일괄 삭제는 해당 리스트의 완료된 리마인더만 삭제하고 삭제 개수를 반환한다")
    void deleteCompletedReminders_deletesOnlyCompletedRemindersOfList() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null, null, 0));
        ReminderResponse done = create("빨래", home.getId());
        ReminderResponse todo = create("청소", home.getId());
        ReminderResponse otherListDone = create("보고서", work.getId());
        reminderService.toggleComplete(done.id());
        reminderService.toggleComplete(otherListDone.id());
        entityManager.flush();
        entityManager.clear();

        DeletedCountResponse result = reminderService.deleteCompletedReminders(home.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(result.deletedCount()).isEqualTo(1);
        assertThat(reminderRepository.findById(done.id())).isEmpty();
        assertThat(reminderRepository.findById(todo.id())).isPresent();
        assertThat(reminderRepository.findById(otherListDone.id())).isPresent();
    }

    @Test
    @DisplayName("완료된 부모는 하위 작업과 함께 삭제되고, 미완료 부모 아래에서는 완료된 하위 작업만 삭제된다")
    void deleteCompletedReminders_handlesSubtasks() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse doneParent = create("이사 준비", home.getId());
        ReminderResponse reopenedSubtask = createSubtask("박스 구하기", doneParent.id());
        ReminderResponse todoParent = create("파티 준비", home.getId());
        ReminderResponse doneSubtask = createSubtask("초대장 보내기", todoParent.id());
        ReminderResponse todoSubtask = createSubtask("케이크 주문", todoParent.id());
        entityManager.flush();
        entityManager.clear();
        reminderService.toggleComplete(doneParent.id());
        reminderService.toggleComplete(reopenedSubtask.id());
        reminderService.toggleComplete(doneSubtask.id());
        entityManager.flush();
        entityManager.clear();

        DeletedCountResponse result = reminderService.deleteCompletedReminders(home.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(result.deletedCount()).isEqualTo(3);
        assertThat(reminderRepository.findById(doneParent.id())).isEmpty();
        assertThat(reminderRepository.findById(reopenedSubtask.id())).isEmpty();
        assertThat(reminderRepository.findById(doneSubtask.id())).isEmpty();
        assertThat(reminderQueryService.getReminders(home.getId(), null)).singleElement().satisfies(parent -> {
            assertThat(parent.id()).isEqualTo(todoParent.id());
            assertThat(parent.subtasks()).extracting(ReminderResponse::id).containsExactly(todoSubtask.id());
        });
    }

    @Test
    @DisplayName("부모가 지연 로딩 프록시여도 완료된 하위 작업은 부모와 함께 한 번만 삭제된다")
    void deleteCompletedReminders_countsCompletedSubtaskOnce_whenParentIsProxy() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse parent = create("이사 준비", home.getId());
        ReminderResponse child = createSubtask("박스 구하기", parent.id());
        reminderService.toggleComplete(parent.id());
        entityManager.flush();
        entityManager.clear();

        Reminder parentProxy = entityManager.getReference(Reminder.class, parent.id());
        Reminder loadedChild = reminderRepository.findById(child.id()).orElseThrow();
        assertThat(loadedChild.getParent()).isSameAs(parentProxy);
        assertThat(entityManager.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(parentProxy)).isFalse();
        assertThat(loadedChild.isDeletedOnCompletedClear()).isFalse();

        DeletedCountResponse result = reminderService.deleteCompletedReminders(home.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(reminderRepository.findById(parent.id())).isEmpty();
        assertThat(reminderRepository.findById(child.id())).isEmpty();
    }

    @Test
    @DisplayName("완료 항목이 없으면 아무것도 삭제하지 않고 0을 반환한다")
    void deleteCompletedReminders_returnsZero_whenNothingCompleted() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse todo = create("청소", home.getId());

        DeletedCountResponse result = reminderService.deleteCompletedReminders(home.getId());

        assertThat(result.deletedCount()).isZero();
        assertThat(reminderRepository.findById(todo.id())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 리스트의 완료 항목을 삭제하면 404 예외가 발생한다")
    void deleteCompletedReminders_throwsNotFound_whenListDoesNotExist() {
        assertThatThrownBy(() -> reminderService.deleteCompletedReminders(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("리마인더를 다른 리스트로 옮기면 하위 작업과 함께 대상 리스트의 마지막 순서가 되고 양쪽 개수가 바뀐다")
    void moveReminder_movesWithSubtasksToEndOfTargetList_andUpdatesCounts() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null, null, 0));
        ReminderResponse moving = create("이사 준비", home.getId());
        ReminderResponse boxes = createSubtask("박스 구하기", moving.id());
        ReminderResponse movers = createSubtask("이삿짐센터 예약", moving.id());
        create("청소", home.getId());
        ReminderResponse report = create("보고서", work.getId());
        ReminderResponse meeting = create("회의", work.getId());
        entityManager.flush();
        entityManager.clear();

        ReminderResponse result = reminderService.moveReminder(moving.id(), work.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(result.listId()).isEqualTo(work.getId());
        assertThat(result.subtasks()).extracting(ReminderResponse::listId).containsOnly(work.getId());
        assertThat(reminderQueryService.getReminders(work.getId(), null)).extracting(ReminderResponse::id)
                .containsExactly(report.id(), meeting.id(), moving.id());
        assertThat(reminderQueryService.getReminders(work.getId(), null).get(2).subtasks())
                .extracting(ReminderResponse::id).containsExactly(boxes.id(), movers.id());
        assertThat(reminderRepository.findById(boxes.id()).orElseThrow().getList().getId()).isEqualTo(work.getId());
        assertThat(reminderListRepository.findSummary(owner.getId(), home.getId()).orElseThrow().reminderCount())
                .isEqualTo(1);
        assertThat(reminderListRepository.findSummary(owner.getId(), work.getId()).orElseThrow().reminderCount())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("리스트 없는 리마인더도 리스트로 옮길 수 있다")
    void moveReminder_movesReminderWithoutListIntoList() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse noList = create("우유 사기", null);

        ReminderResponse result = reminderService.moveReminder(noList.id(), home.getId());

        assertThat(result.listId()).isEqualTo(home.getId());
        assertThat(result.sortOrder()).isZero();
    }

    @Test
    @DisplayName("이미 대상 리스트에 있으면 순서를 포함해 아무것도 바뀌지 않는다")
    void moveReminder_keepsSortOrder_whenMovingToSameList() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderResponse first = create("빨래", home.getId());
        ReminderResponse second = create("청소", home.getId());

        ReminderResponse result = reminderService.moveReminder(first.id(), home.getId());

        assertThat(result.sortOrder()).isEqualTo(first.sortOrder());
        assertThat(reminderQueryService.getReminders(home.getId(), null)).extracting(ReminderResponse::id)
                .containsExactly(first.id(), second.id());
    }

    @Test
    @DisplayName("하위 작업을 단독으로 옮기면 다른 리스트든 같은 리스트든 도메인 예외가 발생하고 그대로 남는다")
    void moveReminder_throwsDomainException_whenReminderIsSubtask() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        ReminderList work = reminderListRepository.save(new ReminderList(owner, "업무", null, null, 0));
        ReminderResponse parent = create("이사 준비", home.getId());
        ReminderResponse subtask = createSubtask("박스 구하기", parent.id());

        assertThatThrownBy(() -> reminderService.moveReminder(subtask.id(), work.getId()))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> reminderService.moveReminder(subtask.id(), home.getId()))
                .isInstanceOf(DomainException.class);
        assertThat(reminderRepository.findById(subtask.id()).orElseThrow().getList().getId()).isEqualTo(home.getId());
    }

    @Test
    @DisplayName("멤버가 아닌 리스트나 없는 리스트로 옮기면 404 예외가 발생한다")
    void moveReminder_throwsNotFound_whenTargetListIsNotAccessible() {
        ReminderList home = reminderListRepository.save(new ReminderList(owner, "집", null, null, 0));
        User other = userRepository.save(new User("other@example.com", "{noop}password", "다른 사람"));
        ReminderList othersList = reminderListRepository.save(new ReminderList(other, "남의 리스트", null, null, 0));
        ReminderResponse reminder = create("빨래", home.getId());

        assertThatThrownBy(() -> reminderService.moveReminder(reminder.id(), othersList.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
        assertThatThrownBy(() -> reminderService.moveReminder(reminder.id(), -1L))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.NOT_FOUND);
    }

    private LocalDateTime backdateUpdatedAt(Long reminderId) {
        LocalDateTime past = LocalDateTime.of(2026, 1, 1, 0, 0);
        entityManager.createQuery("update Reminder r set r.updatedAt = :past where r.id = :id")
                .setParameter("past", past)
                .setParameter("id", reminderId)
                .executeUpdate();
        entityManager.clear();
        return past;
    }

    private long countRows(String entityName, Long reminderId) {
        return entityManager.createQuery("select count(e) from " + entityName + " e where e.reminder.id = :id", Long.class)
                .setParameter("id", reminderId)
                .getSingleResult();
    }

    private Long reminderTagRowId(Long reminderId, String tagName) {
        return entityManager.createQuery("""
                        select rt.id from ReminderTag rt where rt.reminder.id = :id and rt.tag.name = :name
                        """, Long.class)
                .setParameter("id", reminderId)
                .setParameter("name", tagName)
                .getSingleResult();
    }

    private Long repeatDayRowId(Long reminderId, DayOfWeek dayOfWeek) {
        return entityManager.createQuery("""
                        select d.id from ReminderRepeatDay d where d.reminder.id = :id and d.dayOfWeek = :day
                        """, Long.class)
                .setParameter("id", reminderId)
                .setParameter("day", dayOfWeek)
                .getSingleResult();
    }

    private ReminderResponse createRepeating(String title, Long listId, LocalDateTime dueAt, RepeatRule repeatRule) {
        return reminderService.createReminder(
                new CreateReminderCommand(title, null, listId, dueAt, null, null, null, new Recurrence(repeatRule, 1, null)));
    }

    private ReminderResponse createCustomRepeating(String title, LocalDateTime dueAt, RepeatRule repeatRule,
                                                   Integer repeatInterval, Set<DayOfWeek> repeatDaysOfWeek) {
        return reminderService.createReminder(new CreateReminderCommand(title, null, null, dueAt, null, null, null, new Recurrence(repeatRule, repeatInterval, repeatDaysOfWeek)));
    }

    private List<ReminderResponse> scheduledByTitle(String title) {
        return reminderQueryService.getSmartReminders(SmartView.SCHEDULED).stream()
                .filter(reminder -> reminder.title().equals(title))
                .toList();
    }

    private ReminderResponse create(String title, Long listId) {
        return reminderService.createReminder(new CreateReminderCommand(title, null, listId, null, null, null, null, null));
    }

    private ReminderResponse createSubtask(String title, Long parentId) {
        return reminderService.createReminder(new CreateReminderCommand(title, null, null, null, null, null, parentId, null));
    }
}

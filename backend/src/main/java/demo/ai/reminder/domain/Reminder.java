package demo.ai.reminder.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OptimisticLock;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;

    private String memo;

    private boolean completed;

    private boolean flagged;

    private LocalDateTime dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    private LocalDateTime completedAt;

    @Getter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatRule repeatRule;

    // 반복 간격(1~99). 컬럼 추가 전에 저장된 행은 기본값 1로 채워져 예전처럼 동작한다.
    @Getter(AccessLevel.NONE)
    @ColumnDefault("1")
    @Column(nullable = false)
    private int repeatInterval = Recurrence.MIN_INTERVAL;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "reminder", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<ReminderRepeatDay> repeatDays = new ArrayList<>();

    @Column(nullable = false)
    private boolean nextOccurrenceCreated;

    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private ReminderList list;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "reminder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 100)
    private List<ReminderTag> reminderTags = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Reminder parent;

    // 하위 작업은 각자 버전을 가지므로 추가/삭제해도 부모의 버전은 올리지 않는다 (편집 창에서 하위 작업을 추가한 뒤 부모를 저장할 수 있어야 한다).
    @OptimisticLock(excluded = true)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 100)
    private List<Reminder> subtasks = new ArrayList<>();

    // 동시 수정 충돌 감지용 버전. 변경이 저장될 때마다 1씩 늘어난다. 컬럼 추가 전에 저장된 행은 기본값 0으로 채워진다.
    @Version
    @ColumnDefault("0")
    @Column(nullable = false)
    private Long version;

    public Reminder(User user, String title, String memo, ReminderList list, LocalDateTime dueAt, Priority priority,
                    Recurrence recurrence) {
        this.user = user;
        this.title = title;
        this.memo = memo;
        this.list = list;
        this.dueAt = dueAt;
        this.priority = priorityOrNone(priority);
        applyRecurrence(recurrence, dueAt);
    }

    public void update(String title, String memo, LocalDateTime dueAt, boolean flagged, Priority priority,
                       Recurrence recurrence) {
        this.title = title;
        this.memo = memo;
        this.dueAt = dueAt;
        this.flagged = flagged;
        this.priority = priorityOrNone(priority);
        applyRecurrence(recurrence, dueAt);
    }

    public Recurrence getRecurrence() {
        Set<DayOfWeek> daysOfWeek = repeatDays.stream().map(ReminderRepeatDay::getDayOfWeek).collect(Collectors.toSet());
        return new Recurrence(repeatRule, repeatInterval, daysOfWeek);
    }

    // 이미 붙어 있는 태그는 그대로 두고 빠진 태그만 떼고 새 태그만 붙인다.
    // (전부 지우고 다시 붙이면 flush 때 INSERT가 DELETE보다 먼저 실행돼 (reminder_id, tag_id) 유니크 제약에 걸린다.)
    public void replaceTags(Collection<Tag> tags) {
        reminderTags.removeIf(reminderTag -> tags.stream().noneMatch(reminderTag::isFor));
        tags.forEach(this::addTag);
    }

    public void removeTag(Tag tag) {
        reminderTags.removeIf(reminderTag -> reminderTag.isFor(tag));
    }

    public Set<Tag> getTags() {
        return reminderTags.stream()
                .map(ReminderTag::getTag)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    }

    private void addTag(Tag tag) {
        if (reminderTags.stream().noneMatch(reminderTag -> reminderTag.isFor(tag))) {
            reminderTags.add(new ReminderTag(this, tag));
        }
    }

    /**
     * 하위 작업으로 추가한다. 하위 작업은 부모와 같은 리스트에 속하고 부모의 하위 작업 중 마지막 순서가 된다.
     */
    public void addSubtask(Reminder subtask) {
        if (isSubtask()) {
            throw new DomainException("A subtask cannot have subtasks");
        }
        int lastSortOrder = subtasks.stream().mapToInt(Reminder::getSortOrder).max().orElse(-1);
        subtask.parent = this;
        subtask.list = this.list;
        subtask.sortOrder = lastSortOrder + 1;
        subtasks.add(subtask);
    }

    /**
     * 부모의 하위 작업 목록에서 빠지고 부모와의 연결을 끊는다. 최상위 리마인더이면 아무것도 하지 않는다.
     */
    public void detachFromParent() {
        if (parent == null) {
            return;
        }
        parent.removeSubtask(this);
    }

    public void removeSubtask(Reminder subtask) {
        if (subtasks.remove(subtask)) {
            subtask.parent = null;
        }
    }

    public List<Reminder> getSubtasks() {
        return Collections.unmodifiableList(subtasks);
    }

    public boolean isSubtask() {
        return parent != null;
    }

    public boolean isDeletedOnCompletedClear() {
        return completed && !(isSubtask() && parent.isCompleted());
    }

    public int countWithSubtasks() {
        return 1 + subtasks.size();
    }

    /**
     * 다른 리스트로 옮기고 그 리스트에서의 표시 순서를 정한다. 하위 작업도 함께 같은 리스트로 옮긴다(하위 작업 안 순서는 유지).
     * 하위 작업은 부모를 따라서만 이동하므로 단독으로 옮길 수 없다. 이미 그 리스트에 있으면 순서를 바꾸지 않는다.
     */
    public void moveTo(ReminderList list, IntSupplier sortOrderInList) {
        if (isSubtask()) {
            throw new DomainException("A subtask cannot be moved on its own");
        }
        if (isIn(list)) {
            return;
        }
        // 새 순서를 조회하는 쿼리가 자동 flush를 일으키므로, 리스트를 바꾸기 전에 구해야 이 리마인더가 계산에 끼지 않는다.
        this.sortOrder = sortOrderInList.getAsInt();
        this.list = list;
        subtasks.forEach(subtask -> subtask.list = list);
    }

    private boolean isIn(ReminderList other) {
        return list == other || (list != null && list.getId() != null && list.getId().equals(other.getId()));
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void toggleFlag() {
        this.flagged = !this.flagged;
    }

    /**
     * 완료 상태를 반전한다. 완료 처리하면 아직 완료되지 않은 하위 작업도 함께 완료한다. 완료 취소는 하위 작업에 전파하지 않는다.
     * 반복 리마인더를 완료하면 다음 회차를 만들어 돌려준다. 저장과 리스트 안 순서 지정은 호출하는 쪽이 한다.
     * 완료를 취소해도 이미 만든 다음 회차는 그대로 두며, 다시 완료해도 다음 회차를 또 만들지 않는다.
     * 부모 완료로 함께 완료된 하위 작업은 반복이어도 다음 회차를 만들지 않는다.
     */
    public Optional<Reminder> toggleComplete(LocalDateTime now) {
        if (completed) {
            this.completed = false;
            this.completedAt = null;
            return Optional.empty();
        }
        complete(now);
        subtasks.stream()
                .filter(subtask -> !subtask.completed)
                .forEach(subtask -> subtask.complete(now));
        return createNextOccurrence();
    }

    // 다음 마감일시로 소유자/제목/메모/플래그/우선순위/태그/리스트/반복 규칙(주기·간격·요일)을 복사한 새 회차를 만든다. 하위 작업은 복사하지 않는다.
    // 하위 작업의 다음 회차는 같은 부모의 하위 작업으로 붙인다.
    private Optional<Reminder> createNextOccurrence() {
        Recurrence recurrence = getRecurrence();
        if (!recurrence.isRepeating() || nextOccurrenceCreated) {
            return Optional.empty();
        }
        this.nextOccurrenceCreated = true;
        Reminder next = new Reminder(user, title, memo, list, recurrence.nextDueAt(dueAt), priority, recurrence);
        next.flagged = flagged;
        next.replaceTags(getTags());
        if (isSubtask()) {
            parent.addSubtask(next);
        }
        return Optional.of(next);
    }

    private void complete(LocalDateTime now) {
        this.completed = true;
        this.completedAt = now;
    }

    private void applyRecurrence(Recurrence recurrence, LocalDateTime dueAt) {
        Recurrence rule = Objects.requireNonNullElse(recurrence, Recurrence.NONE);
        if (rule.isRepeating() && dueAt == null) {
            throw new DomainException("A repeating reminder requires a due date");
        }
        this.repeatRule = rule.rule();
        this.repeatInterval = rule.interval();
        // 태그와 같은 이유로 바뀐 요일만 지우고 더한다.
        repeatDays.removeIf(repeatDay -> !rule.daysOfWeek().contains(repeatDay.getDayOfWeek()));
        rule.daysOfWeek().stream()
                .filter(day -> repeatDays.stream().noneMatch(repeatDay -> repeatDay.getDayOfWeek() == day))
                .forEach(day -> repeatDays.add(new ReminderRepeatDay(this, day)));
    }

    private static Priority priorityOrNone(Priority priority) {
        return Objects.requireNonNullElse(priority, Priority.NONE);
    }
}

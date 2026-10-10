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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유자. 리스트 없이 만든 리마인더도 소유자를 구분해야 하므로 리스트와 별도로 둔다.
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

    // 완료 처리한 시각. 미완료 상태에서는 null이다.
    private LocalDateTime completedAt;

    // 반복 주기. 반복하지 않으면 NONE이며, 반복은 마감일시가 있을 때만 설정할 수 있다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatRule repeatRule;

    // 이 회차를 완료하면서 다음 회차를 이미 만들었는지. 완료 취소 후 다시 완료해도 다음 회차를 중복으로 만들지 않는다.
    @Column(nullable = false)
    private boolean nextOccurrenceCreated;

    // 같은 리스트 안에서의 표시 순서. 작을수록 위에 표시된다.
    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private ReminderList list;

    @ManyToMany
    @JoinTable(
            name = "reminder_tag",
            joinColumns = @JoinColumn(name = "reminder_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @BatchSize(size = 100)
    private Set<Tag> tags = new LinkedHashSet<>();

    // 상위 리마인더. 최상위 리마인더이면 null이다. 깊이는 1단계로 제한한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Reminder parent;

    // 하위 작업. 부모를 삭제하면 함께 삭제되고, sortOrder는 부모 안에서의 순서다.
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 100)
    private List<Reminder> subtasks = new ArrayList<>();

    public Reminder(User user, String title, String memo, ReminderList list, LocalDateTime dueAt) {
        this(user, title, memo, list, dueAt, Priority.NONE);
    }

    public Reminder(User user, String title, String memo, ReminderList list, LocalDateTime dueAt, Priority priority) {
        this(user, title, memo, list, dueAt, priority, RepeatRule.NONE);
    }

    public Reminder(User user, String title, String memo, ReminderList list, LocalDateTime dueAt, Priority priority,
                    RepeatRule repeatRule) {
        this.user = user;
        this.title = title;
        this.memo = memo;
        this.list = list;
        this.dueAt = dueAt;
        this.priority = priorityOrNone(priority);
        this.repeatRule = repeatRuleOrNone(repeatRule, dueAt);
        this.completed = false;
        this.flagged = false;
    }

    public void update(String title, String memo, LocalDateTime dueAt, boolean flagged, Priority priority,
                       RepeatRule repeatRule) {
        this.title = title;
        this.memo = memo;
        this.dueAt = dueAt;
        this.flagged = flagged;
        this.priority = priorityOrNone(priority);
        this.repeatRule = repeatRuleOrNone(repeatRule, dueAt);
    }

    public void replaceTags(Collection<Tag> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * 하위 작업으로 추가한다. 하위 작업은 부모와 같은 리스트에 속하고 부모의 하위 작업 중 마지막 순서가 된다.
     */
    public void addSubtask(Reminder subtask) {
        if (isSubtask()) {
            throw new IllegalStateException("A subtask cannot have subtasks");
        }
        int lastSortOrder = subtasks.stream().mapToInt(Reminder::getSortOrder).max().orElse(-1);
        subtask.parent = this;
        subtask.list = this.list;
        subtask.sortOrder = lastSortOrder + 1;
        subtasks.add(subtask);
    }

    public void removeSubtask(Reminder subtask) {
        subtasks.remove(subtask);
    }

    public List<Reminder> getSubtasks() {
        return Collections.unmodifiableList(subtasks);
    }

    public boolean isSubtask() {
        return parent != null;
    }

    /**
     * 다른 리스트로 옮기고 그 리스트에서의 표시 순서를 정한다. 하위 작업도 함께 같은 리스트로 옮긴다(하위 작업 안 순서는 유지).
     * 하위 작업은 부모를 따라서만 이동하므로 단독으로 옮길 수 없다.
     */
    public void moveTo(ReminderList list, int sortOrder) {
        if (isSubtask()) {
            throw new IllegalStateException("A subtask cannot be moved on its own");
        }
        this.list = list;
        this.sortOrder = sortOrder;
        subtasks.forEach(subtask -> subtask.list = list);
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

    // 다음 마감일시로 소유자/제목/메모/플래그/우선순위/태그/리스트/반복 주기를 복사한 새 회차를 만든다. 하위 작업은 복사하지 않는다.
    // 하위 작업의 다음 회차는 같은 부모의 하위 작업으로 붙인다.
    private Optional<Reminder> createNextOccurrence() {
        if (!repeatRule.isRepeating() || nextOccurrenceCreated) {
            return Optional.empty();
        }
        this.nextOccurrenceCreated = true;
        Reminder next = new Reminder(user, title, memo, list, repeatRule.nextDueAt(dueAt), priority, repeatRule);
        next.flagged = flagged;
        next.tags.addAll(tags);
        if (isSubtask()) {
            parent.addSubtask(next);
        }
        return Optional.of(next);
    }

    private void complete(LocalDateTime now) {
        this.completed = true;
        this.completedAt = now;
    }

    // 반복을 지정하지 않으면(null) '반복 안 함'으로 둔다. 마감일시 없이 반복을 설정할 수는 없다.
    private static RepeatRule repeatRuleOrNone(RepeatRule repeatRule, LocalDateTime dueAt) {
        RepeatRule rule = Objects.requireNonNullElse(repeatRule, RepeatRule.NONE);
        if (rule.isRepeating() && dueAt == null) {
            throw new IllegalArgumentException("A repeating reminder requires a due date");
        }
        return rule;
    }

    // 우선순위를 지정하지 않으면(null) '없음'으로 둔다.
    private static Priority priorityOrNone(Priority priority) {
        return Objects.requireNonNullElse(priority, Priority.NONE);
    }
}

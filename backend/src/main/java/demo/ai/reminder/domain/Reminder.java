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
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Reminder(String title, String memo, ReminderList list, LocalDateTime dueAt) {
        this(title, memo, list, dueAt, Priority.NONE);
    }

    public Reminder(String title, String memo, ReminderList list, LocalDateTime dueAt, Priority priority) {
        this.title = title;
        this.memo = memo;
        this.list = list;
        this.dueAt = dueAt;
        this.priority = priorityOrNone(priority);
        this.completed = false;
        this.flagged = false;
    }

    public void update(String title, String memo, LocalDateTime dueAt, boolean flagged, Priority priority) {
        this.title = title;
        this.memo = memo;
        this.dueAt = dueAt;
        this.flagged = flagged;
        this.priority = priorityOrNone(priority);
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

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void toggleFlag() {
        this.flagged = !this.flagged;
    }

    // 완료 처리하면 아직 완료되지 않은 하위 작업도 함께 완료한다. 완료 취소는 하위 작업에 전파하지 않는다.
    public void toggleComplete(LocalDateTime now) {
        if (completed) {
            this.completed = false;
            this.completedAt = null;
            return;
        }
        complete(now);
        subtasks.stream()
                .filter(subtask -> !subtask.completed)
                .forEach(subtask -> subtask.complete(now));
    }

    private void complete(LocalDateTime now) {
        this.completed = true;
        this.completedAt = now;
    }

    // 우선순위를 지정하지 않으면(null) '없음'으로 둔다.
    private static Priority priorityOrNone(Priority priority) {
        return Objects.requireNonNullElse(priority, Priority.NONE);
    }
}

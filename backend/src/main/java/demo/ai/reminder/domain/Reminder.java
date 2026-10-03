package demo.ai.reminder.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
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

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void toggleFlag() {
        this.flagged = !this.flagged;
    }

    public void toggleComplete(LocalDateTime now) {
        this.completed = !this.completed;
        this.completedAt = this.completed ? now : null;
    }

    // 우선순위를 지정하지 않으면(null) '없음'으로 둔다.
    private static Priority priorityOrNone(Priority priority) {
        return Objects.requireNonNullElse(priority, Priority.NONE);
    }
}

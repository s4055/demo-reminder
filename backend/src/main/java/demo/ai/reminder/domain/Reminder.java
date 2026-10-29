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

    @OptimisticLock(excluded = true)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 100)
    private List<Reminder> subtasks = new ArrayList<>();

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

    public void moveTo(ReminderList list, IntSupplier sortOrderInList) {
        if (isSubtask()) {
            throw new DomainException("A subtask cannot be moved on its own");
        }
        if (isIn(list)) {
            return;
        }
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
        repeatDays.removeIf(repeatDay -> !rule.daysOfWeek().contains(repeatDay.getDayOfWeek()));
        rule.daysOfWeek().stream()
                .filter(day -> repeatDays.stream().noneMatch(repeatDay -> repeatDay.getDayOfWeek() == day))
                .forEach(day -> repeatDays.add(new ReminderRepeatDay(this, day)));
    }

    private static Priority priorityOrNone(Priority priority) {
        return Objects.requireNonNullElse(priority, Priority.NONE);
    }
}

package demo.ai.reminder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String memo;

    private boolean completed;

    private boolean flagged;

    private LocalDateTime dueAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private ReminderList list;

    @CreationTimestamp(source = SourceType.DB)
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime updatedAt;

    public Reminder(String title, String memo, ReminderList list, LocalDateTime dueAt) {
        this.title = title;
        this.memo = memo;
        this.list = list;
        this.dueAt = dueAt;
        this.completed = false;
        this.flagged = false;
    }

    public void update(String title, String memo, LocalDateTime dueAt, boolean flagged) {
        this.title = title;
        this.memo = memo;
        this.dueAt = dueAt;
        this.flagged = flagged;
    }

    public void toggleFlag() {
        this.flagged = !this.flagged;
    }

    public void toggleComplete() {
        this.completed = !this.completed;
    }
}

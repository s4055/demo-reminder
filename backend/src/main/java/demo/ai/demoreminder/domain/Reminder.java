package demo.ai.demoreminder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @CreationTimestamp(source = SourceType.DB)
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime updatedAt;

    public Reminder(String title, String memo) {
        this.title = title;
        this.memo = memo;
        this.completed = false;
    }

    public void toggleComplete() {
        this.completed = !this.completed;
    }
}

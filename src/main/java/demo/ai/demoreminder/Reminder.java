package demo.ai.demoreminder;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    private String memo;

    private LocalDateTime remindAt;

    private boolean completed;

    public Reminder(String title, String memo, LocalDateTime remindAt) {
        this.title = title;
        this.memo = memo;
        this.remindAt = remindAt;
        this.completed = false;
    }
}

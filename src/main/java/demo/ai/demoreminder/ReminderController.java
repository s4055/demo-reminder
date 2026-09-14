package demo.ai.demoreminder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderRepository reminderRepository;

    public ReminderController(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @GetMapping
    public List<Reminder> findAll() {
        return reminderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Reminder findById(@PathVariable Long id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reminder create(@Valid @RequestBody ReminderRequest request) {
        Reminder reminder = new Reminder(request.title(), request.memo(), request.remindAt());
        return reminderRepository.save(reminder);
    }

    @PutMapping("/{id}")
    public Reminder update(@PathVariable Long id, @Valid @RequestBody ReminderRequest request) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found: " + id));
        reminder.setTitle(request.title());
        reminder.setMemo(request.memo());
        reminder.setRemindAt(request.remindAt());
        return reminderRepository.save(reminder);
    }

    @PatchMapping("/{id}/complete")
    public Reminder complete(@PathVariable Long id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found: " + id));
        reminder.setCompleted(true);
        return reminderRepository.save(reminder);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!reminderRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found: " + id);
        }
        reminderRepository.deleteById(id);
    }

    public record ReminderRequest(@NotBlank String title, String memo, LocalDateTime remindAt) {
    }
}

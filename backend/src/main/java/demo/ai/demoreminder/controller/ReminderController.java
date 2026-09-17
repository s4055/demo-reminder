package demo.ai.demoreminder.controller;

import demo.ai.demoreminder.dto.ReminderRequest;
import demo.ai.demoreminder.dto.ReminderResponse;
import demo.ai.demoreminder.domain.Reminder;
import demo.ai.demoreminder.repository.ReminderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderRepository reminderRepository;

    @GetMapping
    public List<ReminderResponse> getReminders() {
        return reminderRepository.findAll().stream()
                .map(ReminderResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse createReminder(@Valid @RequestBody ReminderRequest request) {
        Reminder reminder = new Reminder(request.title(), request.memo());
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @PatchMapping("/{id}/complete")
    public ReminderResponse toggleComplete(@PathVariable Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleComplete();
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReminder(@PathVariable Long id) {
        if (!reminderRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found: " + id);
        }
        reminderRepository.deleteById(id);
    }

    private Reminder findReminderOrThrow(Long id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found: " + id));
    }
}

package demo.ai.demoreminder.controller;

import demo.ai.demoreminder.dto.ReminderRequest;
import demo.ai.demoreminder.dto.ReminderResponse;
import demo.ai.demoreminder.service.ReminderService;
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

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public List<ReminderResponse> getReminders() {
        return reminderService.getReminders().stream()
                .map(ReminderResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse createReminder(@Valid @RequestBody ReminderRequest request) {
        return ReminderResponse.from(reminderService.createReminder(request));
    }

    @PatchMapping("/{id}/complete")
    public ReminderResponse toggleComplete(@PathVariable Long id) {
        return ReminderResponse.from(reminderService.toggleComplete(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReminder(@PathVariable Long id) {
        reminderService.deleteReminder(id);
    }
}

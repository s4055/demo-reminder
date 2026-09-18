package demo.ai.demoreminder.service;

import demo.ai.demoreminder.domain.Reminder;
import demo.ai.demoreminder.dto.ReminderRequest;
import demo.ai.demoreminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public List<Reminder> getReminders() {
        return reminderRepository.findAll();
    }

    @Transactional
    public Reminder createReminder(ReminderRequest request) {
        Reminder reminder = new Reminder(request.title(), request.memo());
        return reminderRepository.save(reminder);
    }

    @Transactional
    public Reminder toggleComplete(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleComplete();
        return reminder;
    }

    @Transactional
    public void deleteReminder(Long id) {
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

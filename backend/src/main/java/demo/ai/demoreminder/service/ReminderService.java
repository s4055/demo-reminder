package demo.ai.demoreminder.service;

import demo.ai.demoreminder.domain.Reminder;
import demo.ai.demoreminder.domain.ReminderList;
import demo.ai.demoreminder.dto.ReminderRequest;
import demo.ai.demoreminder.repository.ReminderListRepository;
import demo.ai.demoreminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private static final Sort DEFAULT_SORT = Sort.by("id");

    private final ReminderRepository reminderRepository;
    private final ReminderListRepository reminderListRepository;

    public List<Reminder> getReminders(Long listId) {
        if (listId == null) {
            return reminderRepository.findAll(DEFAULT_SORT);
        }
        return reminderRepository.findByListId(listId, DEFAULT_SORT);
    }

    @Transactional
    public Reminder createReminder(ReminderRequest request) {
        ReminderList list = request.listId() == null ? null : findListOrThrow(request.listId());
        Reminder reminder = new Reminder(request.title(), request.memo(), list);
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

    private ReminderList findListOrThrow(Long listId) {
        return reminderListRepository.findById(listId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found: " + listId));
    }

    private Reminder findReminderOrThrow(Long id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found: " + id));
    }
}

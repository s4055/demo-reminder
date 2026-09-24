package demo.ai.reminder.service;

import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderListSummary;
import demo.ai.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderListService {

    private final ReminderListRepository reminderListRepository;
    private final ReminderRepository reminderRepository;

    public List<ReminderListSummary> getLists() {
        return reminderListRepository.findAllWithReminderCount();
    }

    @Transactional
    public ReminderListSummary createList(ReminderListRequest request) {
        ReminderList list = reminderListRepository.save(new ReminderList(request.name(), request.color()));
        return new ReminderListSummary(list, 0);
    }

    @Transactional
    public ReminderListSummary updateList(Long id, ReminderListRequest request) {
        ReminderList list = findListOrThrow(id);
        list.update(request.name(), request.color());
        return new ReminderListSummary(list, reminderRepository.countByListIdAndCompletedFalse(id));
    }

    @Transactional
    public void deleteList(Long id) {
        ReminderList list = findListOrThrow(id);
        reminderRepository.deleteAllByListId(id);
        reminderListRepository.delete(list);
    }

    private ReminderList findListOrThrow(Long id) {
        return reminderListRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found: " + id));
    }
}

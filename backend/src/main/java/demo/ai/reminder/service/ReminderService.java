package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private static final Sort DEFAULT_SORT = Sort.by("id");
    private static final Sort DUE_DATE_SORT = Sort.by("dueAt", "id");
    private static final Sort COMPLETED_SORT = Sort.by(Sort.Order.desc("completedAt"), Sort.Order.desc("id"));

    private final ReminderRepository reminderRepository;
    private final ReminderListRepository reminderListRepository;

    public List<Reminder> getReminders(Long listId) {
        if (listId == null) {
            return reminderRepository.findAll(DEFAULT_SORT);
        }
        return reminderRepository.findByListId(listId, DEFAULT_SORT);
    }

    public List<Reminder> getSmartReminders(String view) {
        return switch (SmartView.from(view)) {
            case TODAY -> {
                LocalDate today = LocalDate.now();
                yield reminderRepository.findByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThan(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay(), DUE_DATE_SORT);
            }
            case SCHEDULED -> reminderRepository.findByCompletedFalseAndDueAtIsNotNull(DUE_DATE_SORT);
            case ALL -> reminderRepository.findByCompletedFalse(DEFAULT_SORT);
            case FLAGGED -> reminderRepository.findByCompletedFalseAndFlaggedTrue(DEFAULT_SORT);
            case COMPLETED -> reminderRepository.findByCompletedTrue(COMPLETED_SORT);
        };
    }

    @Transactional
    public Reminder createReminder(ReminderRequest request) {
        ReminderList list = request.listId() == null ? null : findListOrThrow(request.listId());
        Reminder reminder = new Reminder(request.title(), request.memo(), list, request.dueAt());
        return reminderRepository.save(reminder);
    }

    @Transactional
    public Reminder updateReminder(Long id, ReminderUpdateRequest request) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.update(request.title(), request.memo(), request.dueAt(), request.flagged());
        return reminder;
    }

    @Transactional
    public Reminder toggleComplete(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleComplete(LocalDateTime.now());
        return reminder;
    }

    @Transactional
    public Reminder toggleFlag(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleFlag();
        return reminder;
    }

    @Transactional
    public void deleteReminder(Long id) {
        if (!reminderRepository.existsById(id)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Reminder not found: " + id);
        }
        reminderRepository.deleteById(id);
    }

    private ReminderList findListOrThrow(Long listId) {
        return reminderListRepository.findById(listId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List not found: " + listId));
    }

    private Reminder findReminderOrThrow(Long id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Reminder not found: " + id));
    }
}

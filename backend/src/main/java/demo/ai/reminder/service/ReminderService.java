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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private static final Sort DEFAULT_SORT = Sort.by("id");
    private static final Sort LIST_SORT = Sort.by("sortOrder", "id");
    private static final Sort DUE_DATE_SORT = Sort.by("dueAt", "id");
    private static final Sort COMPLETED_SORT = Sort.by(Sort.Order.desc("completedAt"), Sort.Order.desc("id"));

    private final ReminderRepository reminderRepository;
    private final ReminderListRepository reminderListRepository;

    // 리스트를 지정하면 표시 순서대로, 지정하지 않으면 생성순으로 조회한다.
    public List<Reminder> getReminders(Long listId) {
        if (listId == null) {
            return reminderRepository.findAll(DEFAULT_SORT);
        }
        return reminderRepository.findByListId(listId, LIST_SORT);
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

    // 새 리마인더는 같은 리스트(리스트 없음도 하나의 범위)의 마지막 순서로 추가한다.
    @Transactional
    public Reminder createReminder(ReminderRequest request) {
        ReminderList list = request.listId() == null ? null : findListOrThrow(request.listId());
        Reminder reminder = new Reminder(request.title(), request.memo(), list, request.dueAt(), request.priority());
        reminder.changeSortOrder(nextSortOrder(list));
        return reminderRepository.save(reminder);
    }

    @Transactional
    public Reminder updateReminder(Long id, ReminderUpdateRequest request) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.update(request.title(), request.memo(), request.dueAt(), request.flagged(), request.priority());
        return reminder;
    }

    // ids 순서대로 리스트의 미완료 리마인더 표시 순서를 0부터 다시 매긴다.
    @Transactional
    public void reorderReminders(Long listId, List<Long> ids) {
        findListOrThrow(listId);
        Map<Long, Reminder> reminders = reminderRepository.findByListIdAndCompletedFalse(listId).stream()
                .collect(Collectors.toMap(Reminder::getId, Function.identity()));
        SortOrders.validateIds(ids, reminders.keySet(), "incomplete reminder of the list");
        for (int i = 0; i < ids.size(); i++) {
            reminders.get(ids.get(i)).changeSortOrder(i);
        }
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

    private int nextSortOrder(ReminderList list) {
        if (list == null) {
            return reminderRepository.findMaxSortOrderWithoutList() + 1;
        }
        return reminderRepository.findMaxSortOrderInList(list.getId()) + 1;
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

package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderListSummary;
import demo.ai.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderListService {

    private final ReminderListRepository reminderListRepository;
    private final ReminderRepository reminderRepository;

    public List<ReminderListSummary> getLists() {
        return reminderListRepository.findAllWithReminderCount();
    }

    // 새 리스트는 사이드바 맨 아래(마지막 순서)에 추가한다.
    @Transactional
    public ReminderListSummary createList(ReminderListRequest request) {
        int sortOrder = reminderListRepository.findMaxSortOrder() + 1;
        ReminderList list = reminderListRepository.save(new ReminderList(request.name(), request.color(), sortOrder));
        return new ReminderListSummary(list, 0);
    }

    @Transactional
    public ReminderListSummary updateList(Long id, ReminderListRequest request) {
        ReminderList list = findListOrThrow(id);
        list.update(request.name(), request.color());
        return new ReminderListSummary(list, reminderRepository.countByListIdAndCompletedFalse(id));
    }

    // ids 순서대로 리스트의 표시 순서를 0부터 다시 매긴다.
    @Transactional
    public void reorderLists(List<Long> ids) {
        Map<Long, ReminderList> lists = reminderListRepository.findAll().stream()
                .collect(Collectors.toMap(ReminderList::getId, Function.identity()));
        SortOrders.validateIds(ids, lists.keySet(), "list");
        for (int i = 0; i < ids.size(); i++) {
            lists.get(ids.get(i)).changeSortOrder(i);
        }
    }

    @Transactional
    public void deleteList(Long id) {
        ReminderList list = findListOrThrow(id);
        reminderRepository.deleteAllByListId(id);
        reminderListRepository.delete(list);
    }

    private ReminderList findListOrThrow(Long id) {
        return reminderListRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List not found: " + id));
    }
}

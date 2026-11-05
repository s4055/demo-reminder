package demo.ai.reminder.service;

import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.dto.request.ReminderListRequest;
import demo.ai.reminder.dto.request.ReminderListUpdateRequest;
import demo.ai.reminder.dto.response.ReminderListResponse;
import demo.ai.reminder.repository.list.ReminderListRepository;
import demo.ai.reminder.repository.reminder.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderListService {

    private final ReminderListRepository reminderListRepository;
    private final ReminderRepository reminderRepository;
    private final ListGroupService listGroupService;
    private final ListAccess listAccess;
    private final CurrentUser currentUser;

    public List<ReminderListResponse> getLists() {
        return reminderListRepository.findAllWithReminderCount(currentUser.id()).stream()
                .map(ReminderListResponse::from)
                .toList();
    }

    @Transactional
    public ReminderListResponse createList(ReminderListRequest request) {
        int sortOrder = reminderListRepository.findNextSortOrder(currentUser.id());
        ReminderList list = reminderListRepository.save(
                new ReminderList(currentUser.reference(), request.strippedName(), request.color(), request.icon(), sortOrder));
        return ReminderListResponse.from(reminderListRepository.findSummary(currentUser.id(), list.getId())
                .orElseThrow(() -> new IllegalStateException("No membership for a created list: " + list.getId())));
    }

    @Transactional
    public void updateList(Long id, ReminderListUpdateRequest request) {
        ReminderList list = listAccess.ownedList(id);
        Versions.verify(list.getVersion(), request.version(), "List " + id);
        list.update(request.strippedName(), request.color(), request.icon());
    }

    @Transactional
    public void changeGroup(Long id, Long groupId) {
        ReminderList list = listAccess.ownedList(id);
        if (groupId == null) {
            list.leaveGroup();
        } else {
            list.changeGroup(listGroupService.ownedGroup(groupId));
        }
    }

    @Transactional
    public void reorderLists(List<Long> ids) {
        SortOrders.reorder(ids, reminderListRepository.findByUserId(currentUser.id()),
                ReminderList::getId, ReminderList::changeSortOrder, "list");
    }

    @Transactional
    public void deleteList(Long id) {
        ReminderList list = listAccess.ownedList(id);
        reminderRepository.deleteAllByList(list);
        reminderListRepository.delete(list);
    }
}

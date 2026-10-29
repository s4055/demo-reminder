package demo.ai.reminder.service;

import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.response.ReminderListResponse;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import demo.ai.reminder.service.command.CreateListCommand;
import demo.ai.reminder.service.command.UpdateListCommand;
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
    public ReminderListResponse createList(CreateListCommand command) {
        int sortOrder = reminderListRepository.findNextSortOrder(currentUser.id());
        ReminderList list = reminderListRepository.save(
                new ReminderList(currentUser.reference(), command.name(), command.color(), command.icon(), sortOrder));
        return toResponse(list);
    }

    @Transactional
    public ReminderListResponse updateList(Long id, UpdateListCommand command) {
        ReminderList list = listAccess.ownedList(id);
        Versions.verify(list.getVersion(), command.version(), "List " + id);
        list.update(command.name(), command.color(), command.icon());
        return toResponse(list);
    }

    @Transactional
    public ReminderListResponse changeGroup(Long id, Long groupId) {
        ReminderList list = listAccess.ownedList(id);
        if (groupId == null) {
            list.leaveGroup();
        } else {
            list.changeGroup(listGroupService.ownedGroup(groupId));
        }
        return toResponse(list);
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

    private ReminderListResponse toResponse(ReminderList list) {
        reminderListRepository.flush();
        return ReminderListResponse.from(reminderListRepository.findSummary(currentUser.id(), list.getId())
                .orElseThrow(() -> new IllegalStateException("No membership for an accessed list: " + list.getId())));
    }
}

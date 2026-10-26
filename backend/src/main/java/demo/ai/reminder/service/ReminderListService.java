package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListGroup;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.response.ReminderListResponse;
import demo.ai.reminder.repository.ListGroupRepository;
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
    private final ListGroupRepository listGroupRepository;
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
        ListGroup group = groupId == null ? null : listGroupRepository.findByIdAndUserId(groupId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List group not found: " + groupId));
        list.changeGroup(group);
        return toResponse(list);
    }

    @Transactional
    public void reorderLists(List<Long> ids) {
        SortOrders.reorder(ids, reminderListRepository.findByUserId(currentUser.id()),
                ReminderList::getId, ReminderList::changeSortOrder, "list");
    }

    // 리스트 목록과 같은 요약 쿼리로 응답을 만들어 개수/역할 규칙을 한 곳에 둔다.
    // 갱신된 수정일(updatedAt)과 새 리스트/멤버가 반영되도록 flush한 뒤 조회한다.
    private ReminderListResponse toResponse(ReminderList list) {
        reminderListRepository.flush();
        return ReminderListResponse.from(reminderListRepository.findSummary(currentUser.id(), list.getId())
                .orElseThrow(() -> new IllegalStateException("No membership for an accessed list: " + list.getId())));
    }

    @Transactional
    public void deleteList(Long id) {
        ReminderList list = listAccess.ownedList(id);
        reminderRepository.deleteAllByList(list);
        reminderListRepository.delete(list);
    }
}

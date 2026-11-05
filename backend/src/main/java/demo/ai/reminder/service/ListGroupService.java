package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.list.ListGroup;
import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.dto.response.ListGroupResponse;
import demo.ai.reminder.repository.list.ListGroupRepository;
import demo.ai.reminder.repository.list.ReminderListRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListGroupService {

    private final ListGroupRepository listGroupRepository;
    private final ReminderListRepository reminderListRepository;
    private final CurrentUser currentUser;

    public List<ListGroupResponse> getGroups() {
        return listGroupRepository.findByUserIdOrderBySortOrderAscIdAsc(currentUser.id()).stream()
                .map(ListGroupResponse::from)
                .toList();
    }

    @Transactional
    public ListGroupResponse createGroup(String name) {
        int sortOrder = listGroupRepository.findNextSortOrder(currentUser.id());
        ListGroup group = listGroupRepository.save(new ListGroup(currentUser.reference(), name, sortOrder));
        return ListGroupResponse.from(group);
    }

    @Transactional
    public ListGroupResponse renameGroup(Long id, String name) {
        ListGroup group = ownedGroup(id);
        group.rename(name);
        return ListGroupResponse.from(group);
    }

    @Transactional
    public void reorderGroups(List<Long> ids) {
        SortOrders.reorder(ids, listGroupRepository.findByUserId(currentUser.id()),
                ListGroup::getId, ListGroup::changeSortOrder, "list group");
    }

    @Transactional
    public void deleteGroup(Long id) {
        ListGroup group = ownedGroup(id);
        reminderListRepository.findByGroupId(id).forEach(ReminderList::leaveGroup);
        listGroupRepository.delete(group);
    }

    ListGroup ownedGroup(Long id) {
        return listGroupRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List group not found: " + id));
    }
}

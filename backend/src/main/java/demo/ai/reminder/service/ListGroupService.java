package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListGroup;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.response.ListGroupResponse;
import demo.ai.reminder.repository.ListGroupRepository;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListGroupService {

    private static final Sort GROUP_SORT = Sort.by("sortOrder", "id");

    private final ListGroupRepository listGroupRepository;
    private final ReminderListRepository reminderListRepository;
    private final CurrentUser currentUser;

    public List<ListGroupResponse> getGroups() {
        return listGroupRepository.findByUserId(currentUser.id(), GROUP_SORT).stream()
                .map(ListGroupResponse::from)
                .toList();
    }

    @Transactional
    public ListGroupResponse createGroup(String name) {
        int sortOrder = listGroupRepository.findNextSortOrder(currentUser.id());
        ListGroup group = listGroupRepository.save(new ListGroup(currentUser.reference(), name, sortOrder));
        return ListGroupResponse.from(group);
    }

    // 응답에 갱신된 수정일(updatedAt)이 담기도록 flush한 뒤 변환한다.
    @Transactional
    public ListGroupResponse renameGroup(Long id, String name) {
        ListGroup group = ownedGroup(id);
        group.rename(name);
        listGroupRepository.flush();
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

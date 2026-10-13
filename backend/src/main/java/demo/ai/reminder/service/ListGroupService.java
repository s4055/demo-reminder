package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListGroup;
import demo.ai.reminder.dto.request.ListGroupRequest;
import demo.ai.reminder.dto.response.ListGroupResponse;
import demo.ai.reminder.repository.ListGroupRepository;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 사이드바 리스트 그룹. 모든 작업은 현재 사용자의 그룹으로 제한하며, 다른 사용자의 그룹은 404로 응답한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListGroupService {

    private final ListGroupRepository listGroupRepository;
    private final ReminderListRepository reminderListRepository;
    private final CurrentUser currentUser;

    // 사이드바 표시 순서대로 조회한다.
    public List<ListGroupResponse> getGroups() {
        return listGroupRepository.findByUserIdOrderBySortOrderAscIdAsc(currentUser.id()).stream()
                .map(ListGroupResponse::from)
                .toList();
    }

    // 새 그룹은 마지막 순서로 추가한다.
    @Transactional
    public ListGroupResponse createGroup(ListGroupRequest request) {
        int sortOrder = listGroupRepository.findMaxSortOrder(currentUser.id()) + 1;
        ListGroup group = listGroupRepository.save(
                new ListGroup(currentUser.reference(), request.name().strip(), sortOrder));
        return ListGroupResponse.from(group);
    }

    // 응답에 갱신된 수정일(updatedAt)이 담기도록 flush한 뒤 변환한다.
    @Transactional
    public ListGroupResponse renameGroup(Long id, ListGroupRequest request) {
        ListGroup group = findGroupOrThrow(id);
        group.rename(request.name().strip());
        listGroupRepository.flush();
        return ListGroupResponse.from(group);
    }

    // ids 순서대로 현재 사용자의 그룹 표시 순서를 0부터 다시 매긴다.
    @Transactional
    public void reorderGroups(List<Long> ids) {
        Map<Long, ListGroup> groups = listGroupRepository.findByUserId(currentUser.id()).stream()
                .collect(Collectors.toMap(ListGroup::getId, Function.identity()));
        SortOrders.validateIds(ids, groups.keySet(), "list group");
        for (int i = 0; i < ids.size(); i++) {
            groups.get(ids.get(i)).changeSortOrder(i);
        }
    }

    // 그룹만 삭제한다. 소속 리스트는 그룹에서 빠질 뿐 리스트와 리마인더는 그대로 남는다.
    @Transactional
    public void deleteGroup(Long id) {
        ListGroup group = findGroupOrThrow(id);
        reminderListRepository.findByGroupId(id).forEach(list -> list.changeGroup(null));
        listGroupRepository.delete(group);
    }

    private ListGroup findGroupOrThrow(Long id) {
        return listGroupRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List group not found: " + id));
    }
}

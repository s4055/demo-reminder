package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListGroup;
import demo.ai.reminder.domain.ListIcon;
import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.request.ReminderListRequest;
import demo.ai.reminder.dto.request.ReminderListUpdateRequest;
import demo.ai.reminder.dto.response.ReminderListResponse;
import demo.ai.reminder.repository.ListGroupRepository;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderListSummary;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
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
    private final ListGroupRepository listGroupRepository;
    private final ListAccess listAccess;
    private final CurrentUser currentUser;

    // 소유한 리스트와 공유받은 리스트를 모두 조회한다.
    public List<ReminderListResponse> getLists() {
        return reminderListRepository.findAllWithReminderCount(currentUser.id()).stream()
                .map(ReminderListResponse::from)
                .toList();
    }

    // 새 리스트는 사이드바 맨 아래(마지막 순서)에 추가하고, 만든 사용자가 OWNER 멤버가 된다.
    @Transactional
    public ReminderListResponse createList(ReminderListRequest request) {
        validateIcon(request.icon());
        int sortOrder = reminderListRepository.findMaxSortOrder(currentUser.id()) + 1;
        ReminderList list = reminderListRepository.save(
                new ReminderList(currentUser.reference(), request.name(), request.color(), request.icon(), sortOrder));
        return ReminderListResponse.from(new ReminderListSummary(list, 0, ListRole.OWNER, 1));
    }

    // 소유자만 수정할 수 있다. icon을 생략하면 기본 아이콘(null)이 된다.
    // 요청 버전이 현재 버전과 다르면(편집 중 다른 변경이 먼저 저장됨) 409다.
    @Transactional
    public ReminderListResponse updateList(Long id, ReminderListUpdateRequest request) {
        ReminderList list = listAccess.ownedList(id);
        Versions.verify(list.getVersion(), request.version(), "List " + id);
        validateIcon(request.icon());
        list.update(request.name(), request.color(), request.icon());
        return toOwnedResponse(list);
    }

    // 리스트를 자기 그룹에 넣거나(groupId) 그룹에서 뺀다(null). 리스트 소유자만 할 수 있고(편집자 403, 비멤버 404),
    // 다른 사용자의 그룹이나 없는 그룹은 404다.
    @Transactional
    public ReminderListResponse changeGroup(Long id, Long groupId) {
        ReminderList list = listAccess.ownedList(id);
        ListGroup group = groupId == null ? null : listGroupRepository.findByIdAndUserId(groupId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List group not found: " + groupId));
        list.changeGroup(group);
        return toOwnedResponse(list);
    }

    // ids 순서대로 현재 사용자가 소유한 리스트의 표시 순서를 0부터 다시 매긴다. 공유받은 리스트는 대상이 아니다.
    @Transactional
    public void reorderLists(List<Long> ids) {
        Map<Long, ReminderList> lists = reminderListRepository.findByUserId(currentUser.id()).stream()
                .collect(Collectors.toMap(ReminderList::getId, Function.identity()));
        SortOrders.validateIds(ids, lists.keySet(), "list");
        for (int i = 0; i < ids.size(); i++) {
            lists.get(ids.get(i)).changeSortOrder(i);
        }
    }

    // 소유자의 리스트 응답. 갱신된 수정일(updatedAt)이 담기도록 flush한 뒤 변환한다.
    private ReminderListResponse toOwnedResponse(ReminderList list) {
        reminderListRepository.flush();
        return ReminderListResponse.from(new ReminderListSummary(list,
                reminderRepository.countByListIdAndParentIsNullAndCompletedFalse(list.getId()),
                ListRole.OWNER, list.getMembers().size()));
    }

    private void validateIcon(String icon) {
        if (!ListIcon.isAllowed(icon)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unknown list icon: " + icon);
        }
    }

    // 소유자만 삭제할 수 있다. 다른 멤버가 만든 리마인더와 멤버 정보도 함께 삭제된다.
    @Transactional
    public void deleteList(Long id) {
        ReminderList list = listAccess.ownedList(id);
        reminderRepository.deleteAllByListId(id);
        reminderListRepository.delete(list);
    }
}

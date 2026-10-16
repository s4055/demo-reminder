package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.ListGroup;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.request.ReminderListRequest;
import demo.ai.reminder.dto.request.ReminderListUpdateRequest;
import demo.ai.reminder.dto.response.ReminderListResponse;
import demo.ai.reminder.repository.ListGroupRepository;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
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
    private final ListGroupRepository listGroupRepository;
    private final ListAccess listAccess;
    private final CurrentUser currentUser;

    // 소유한 리스트와 공유받은 리스트를 모두 조회한다.
    // 표시 순서(소유한 리스트를 사이드바 순서대로 먼저, 공유받은 리스트를 뒤에)는 저장소 쿼리가 정한다.
    public List<ReminderListResponse> getLists() {
        return reminderListRepository.findAllWithReminderCount(currentUser.id()).stream()
                .map(ReminderListResponse::from)
                .toList();
    }

    // 새 리스트는 사이드바 맨 아래(마지막 순서)에 추가하고, 만든 사용자가 OWNER 멤버가 된다.
    @Transactional
    public ReminderListResponse createList(ReminderListRequest request) {
        int sortOrder = reminderListRepository.findNextSortOrder(currentUser.id());
        ReminderList list = reminderListRepository.save(
                new ReminderList(currentUser.reference(), request.name(), request.color(), request.icon(), sortOrder));
        return toResponse(list);
    }

    // 소유자만 수정할 수 있다. icon을 생략하면 기본 아이콘(null)이 된다.
    // 요청 버전이 현재 버전과 다르면(편집 중 다른 변경이 먼저 저장됨) 409다.
    @Transactional
    public ReminderListResponse updateList(Long id, ReminderListUpdateRequest request) {
        ReminderList list = listAccess.ownedList(id);
        Versions.verify(list.getVersion(), request.version(), "List " + id);
        list.update(request.name(), request.color(), request.icon());
        return toResponse(list);
    }

    // 리스트를 자기 그룹에 넣거나(groupId) 그룹에서 뺀다(null). 리스트 소유자만 할 수 있고(편집자 403, 비멤버 404),
    // 다른 사용자의 그룹이나 없는 그룹은 404다.
    @Transactional
    public ReminderListResponse changeGroup(Long id, Long groupId) {
        ReminderList list = listAccess.ownedList(id);
        ListGroup group = groupId == null ? null : listGroupRepository.findByIdAndUserId(groupId, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List group not found: " + groupId));
        list.changeGroup(group);
        return toResponse(list);
    }

    // ids 순서대로 현재 사용자가 소유한 리스트의 표시 순서를 0부터 다시 매긴다. 공유받은 리스트는 대상이 아니다.
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

    // 소유자만 삭제할 수 있다. 다른 멤버가 만든 리마인더와 멤버 정보도 함께 삭제된다.
    @Transactional
    public void deleteList(Long id) {
        ReminderList list = listAccess.ownedList(id);
        reminderRepository.deleteAllByList(list);
        reminderListRepository.delete(list);
    }
}

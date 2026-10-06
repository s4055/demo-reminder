package demo.ai.reminder.service;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.ReminderListRequest;
import demo.ai.reminder.dto.ReminderListResponse;
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
        int sortOrder = reminderListRepository.findMaxSortOrder(currentUser.id()) + 1;
        ReminderList list = reminderListRepository.save(
                new ReminderList(currentUser.reference(), request.name(), request.color(), sortOrder));
        return ReminderListResponse.from(new ReminderListSummary(list, 0, ListRole.OWNER, 1));
    }

    // 소유자만 수정할 수 있다. 응답에 갱신된 수정일(updatedAt)이 담기도록 flush한 뒤 변환한다.
    @Transactional
    public ReminderListResponse updateList(Long id, ReminderListRequest request) {
        ReminderList list = listAccess.ownedList(id);
        list.update(request.name(), request.color());
        reminderListRepository.flush();
        return ReminderListResponse.from(new ReminderListSummary(list,
                reminderRepository.countByListIdAndParentIsNullAndCompletedFalse(id),
                ListRole.OWNER, list.getMembers().size()));
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

    // 소유자만 삭제할 수 있다. 다른 멤버가 만든 리마인더와 멤버 정보도 함께 삭제된다.
    @Transactional
    public void deleteList(Long id) {
        ReminderList list = listAccess.ownedList(id);
        reminderRepository.deleteAllByListId(id);
        reminderListRepository.delete(list);
    }
}

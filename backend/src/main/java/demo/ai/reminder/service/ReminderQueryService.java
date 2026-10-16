package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 리마인더 조회(리스트/태그별 목록, 스마트 뷰, 검색, 다가오는 일정). 리마인더 변경은 ReminderService가 맡는다.
 * 모두 현재 사용자가 볼 수 있는 리마인더(본인 것 + 멤버인 리스트의 것)만 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderQueryService {

    private static final Sort DEFAULT_SORT = Sort.by("id");
    private static final Sort LIST_SORT = Sort.by("sortOrder", "id");
    private static final Sort DUE_DATE_SORT = Sort.by("dueAt", "id");
    private static final Sort COMPLETED_SORT = Sort.by(Sort.Order.desc("completedAt"), Sort.Order.desc("id"));
    static final int SEARCH_QUERY_MAX_LENGTH = 100;
    private static final Comparator<Reminder> SEARCH_ORDER = Comparator
            .comparing(Reminder::isCompleted)
            .thenComparing(reminder -> reminder.isCompleted() ? null : reminder.getDueAt(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(reminder -> reminder.isCompleted() ? reminder.getCompletedAt() : null,
                    Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))
            .thenComparing(Reminder::getId);

    private final ReminderRepository reminderRepository;
    private final ListAccess listAccess;
    private final CurrentUser currentUser;

    // 리스트를 지정하면 최상위 리마인더만 표시 순서대로(하위 작업은 각 부모의 subtasks로),
    // 태그를 지정하면 그 태그가 붙은 리마인더를 생성순으로, 둘 다 없으면 전체를 생성순으로 조회한다.
    // 태그별/전체 조회에는 하위 작업도 개별 항목으로 포함된다. 멤버가 아닌 리스트는 404다.
    public List<ReminderResponse> getReminders(Long listId, String tag) {
        if (listId != null) {
            ReminderList list = listAccess.memberList(listId);
            return toResponses(reminderRepository.findByListAndParentIsNull(list, LIST_SORT));
        }
        if (tag != null) {
            return toResponses(reminderRepository.findAccessibleByTagName(currentUser.id(), tag.trim(), DEFAULT_SORT));
        }
        return toResponses(reminderRepository.findAccessible(currentUser.id(), DEFAULT_SORT));
    }

    public List<ReminderResponse> getSmartReminders(String view) {
        return toResponses(switch (SmartView.from(view)) {
            case TODAY -> {
                LocalDate today = LocalDate.now();
                yield reminderRepository.findAccessibleIncompleteDueBetween(
                        currentUser.id(), today.atStartOfDay(), today.plusDays(1).atStartOfDay(), DUE_DATE_SORT);
            }
            case SCHEDULED -> reminderRepository.findAccessibleIncompleteWithDueAt(currentUser.id(), DUE_DATE_SORT);
            case ALL -> reminderRepository.findAccessibleIncomplete(currentUser.id(), DEFAULT_SORT);
            case FLAGGED -> reminderRepository.findAccessibleIncompleteFlagged(currentUser.id(), DEFAULT_SORT);
            case COMPLETED -> reminderRepository.findAccessibleCompleted(currentUser.id(), COMPLETED_SORT);
        });
    }

    /**
     * 제목/메모/태그 이름에 검색어가 들어 있는(대소문자 무시) 리마인더를 하위 작업까지 개별 항목으로 찾는다.
     * 미완료 항목을 먼저 마감일시 순(마감일 없는 항목은 뒤), 같으면 생성순으로 두고, 그다음 완료 항목을 완료일시 최신순으로 둔다.
     * 검색어는 앞뒤 공백을 뺀 1~100자이며, %와 _는 문자 그대로 검색한다.
     */
    public List<ReminderResponse> searchReminders(String query) {
        String keyword = query == null ? "" : query.strip();
        if (keyword.isEmpty() || keyword.length() > SEARCH_QUERY_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "Search query must be 1 to " + SEARCH_QUERY_MAX_LENGTH + " characters");
        }
        return reminderRepository.searchAccessible(currentUser.id(), "%" + escapeLike(keyword.toLowerCase()) + "%")
                .stream()
                .sorted(SEARCH_ORDER)
                .map(ReminderResponse::from)
                .toList();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // 알림 스케줄링용. [from, to) 기간에 마감되는 미완료 리마인더를 하위 작업까지 개별 항목으로 마감일시 순으로 조회한다.
    public List<ReminderResponse> getUpcomingReminders(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "from must be before to: from=" + from + ", to=" + to);
        }
        return toResponses(reminderRepository.findAccessibleIncompleteDueBetween(
                currentUser.id(), from, to, DUE_DATE_SORT));
    }

    // 응답 변환은 트랜잭션 안에서 해야 지연 로딩되는 태그/하위 작업/부모를 읽을 수 있다.
    private List<ReminderResponse> toResponses(List<Reminder> reminders) {
        return reminders.stream().map(ReminderResponse::from).toList();
    }
}

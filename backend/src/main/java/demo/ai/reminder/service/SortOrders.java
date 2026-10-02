package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

// 순서 일괄 변경 요청의 공통 검증
final class SortOrders {

    private SortOrders() {
    }

    /**
     * 요청한 ids가 대상 범위의 id와 정확히 일치(누락/중복/범위 밖 id 없음)하는지 검사한다.
     */
    static void validateIds(List<Long> requestedIds, Collection<Long> targetIds, String target) {
        boolean sameSize = requestedIds.size() == targetIds.size();
        if (!sameSize || !new HashSet<>(requestedIds).equals(new HashSet<>(targetIds))) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "ids must contain every " + target + " exactly once");
        }
    }
}

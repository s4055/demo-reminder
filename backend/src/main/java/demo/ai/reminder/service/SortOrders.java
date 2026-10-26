package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

final class SortOrders {

    private SortOrders() {
    }

    static <T> void reorder(List<Long> ids, Collection<T> targets, Function<T, Long> idOf,
                            ObjIntConsumer<T> changeSortOrder, String target) {
        Map<Long, T> targetsById = targets.stream().collect(Collectors.toMap(idOf, Function.identity()));
        validateIds(ids, targetsById.keySet(), target);
        for (int i = 0; i < ids.size(); i++) {
            changeSortOrder.accept(targetsById.get(ids.get(i)), i);
        }
    }

    private static void validateIds(List<Long> requestedIds, Collection<Long> targetIds, String target) {
        boolean sameSize = requestedIds.size() == targetIds.size();
        if (!sameSize || !new HashSet<>(requestedIds).equals(new HashSet<>(targetIds))) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "ids must contain every " + target + " exactly once");
        }
    }
}

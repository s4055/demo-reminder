package demo.ai.reminder.dto.response;

import java.util.List;
import java.util.function.Function;

public record SliceResponse<T>(List<T> items, String nextCursor) {

    public static <E, T> SliceResponse<T> of(List<E> fetched, int size, Function<E, T> mapper,
                                             Function<E, String> cursorOf) {
        boolean hasNext = fetched.size() > size;
        List<E> page = hasNext ? fetched.subList(0, size) : fetched;
        return new SliceResponse<>(page.stream().map(mapper).toList(), hasNext ? cursorOf.apply(page.getLast()) : null);
    }
}

package demo.ai.reminder.domain;

import java.util.Set;

/**
 * 리스트에 지정할 수 있는 아이콘 키. 프론트엔드는 같은 이름의 lucide 아이콘으로 그린다.
 * 아이콘은 선택 사항이며, 지정하지 않으면(null) 기본 아이콘으로 표시한다.
 */
public final class ListIcon {

    public static final Set<String> KEYS = Set.of(
            "list", "shopping-cart", "briefcase", "house", "heart", "star",
            "book-open", "gift", "plane", "dumbbell", "music", "car",
            "graduation-cap", "utensils", "baby", "pill", "wallet", "calendar",
            "flag", "bookmark", "paw-print", "leaf", "code", "gamepad-2"
    );

    private ListIcon() {
    }

    public static boolean isAllowed(String icon) {
        return icon == null || KEYS.contains(icon);
    }
}
